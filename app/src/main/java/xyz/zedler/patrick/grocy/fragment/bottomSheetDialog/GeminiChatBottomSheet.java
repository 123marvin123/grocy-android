/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ChatMessageAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetGeminiChatBinding;
import xyz.zedler.patrick.grocy.model.ChatMessage;
import xyz.zedler.patrick.grocy.util.GrocyFunctionExecutor;
import xyz.zedler.patrick.grocy.util.LocaleUtil;

public class GeminiChatBottomSheet extends BaseBottomSheetDialogFragment {

    private static final String TAG = GeminiChatBottomSheet.class.getSimpleName();

    private MainActivity activity;
    private FragmentBottomsheetGeminiChatBinding binding;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages;
    private List<Content> chatHistory;

    private Client client;

    private GrocyApi grocyApi;
    private GrocyFunctionExecutor functionExecutor;

    private GenerateContentConfig config;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private boolean isFirstMessage = true;

    private ActivityResultLauncher<Intent> voiceInputLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        binding = FragmentBottomsheetGeminiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.activity = (MainActivity) requireActivity();
        this.grocyApi = new GrocyApi((Application) activity.getApplicationContext());

        // Initialize voice input launcher
        voiceInputLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            binding.editMessage.setText(spokenText);
                            binding.editMessage.setSelection(spokenText.length());
                        }
                    }
                }
        );

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Check if Gemini is enabled and API key is set
        boolean geminiEnabled = sharedPrefs.getBoolean(Constants.SETTINGS.GEMINI.ENABLED, false);
        String apiKey = sharedPrefs.getString(Constants.SETTINGS.GEMINI.API_KEY, "");

        if (!geminiEnabled) {
            Toast.makeText(requireContext(), R.string.error_gemini_not_enabled, Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_gemini_no_api_key, Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        // Initialize Gemini
        client = Client.builder().apiKey(apiKey).build();
        chatHistory = new ArrayList<>();

        String languageCode = LocaleUtil.getLanguageCode(AppCompatDelegate.getApplicationLocales());

        String configJson = "{}";
        try(InputStream io = getResources().openRawResource(R.raw.gemini_config)) {
            configJson = convertStreamToString(io);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        configJson = configJson.replaceFirst("<LANG_CODE>", Objects.requireNonNullElse(languageCode, "unknown"));

        config = GenerateContentConfig.fromJson(configJson);

        this.functionExecutor = new GrocyFunctionExecutor(grocyApi, (Application) activity.getApplicationContext(), client, config.tools().get().get(0));

        // Setup RecyclerView
        messages = new ArrayList<>();
        adapter = new ChatMessageAdapter(requireContext(), messages);
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChat.setAdapter(adapter);

        // Setup input
        binding.buttonSend.setOnClickListener(v -> sendMessage());
        binding.buttonVoice.setOnClickListener(v -> startVoiceInput());
        binding.editMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Setup toolbar
        binding.toolbarGemini.setNavigationOnClickListener(v -> dismiss());
        binding.toolbarGemini.setNavigationIcon(R.drawable.ic_round_close);

        // Setup clear chat button
        binding.buttonClearChat.setOnClickListener(v -> clearChat());

        binding.editMessage.requestFocus();

        // Load chat history first
        loadChatHistory();

        // Add welcome message only if no history was loaded
        if (messages.isEmpty()) {
            adapter.addMessage(new ChatMessage(
                    getString(R.string.msg_gemini_welcome),
                    false
            ));
        } else {
            // Scroll to the most recent message if history was loaded
            binding.recyclerChat.post(() ->
                    binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1)
            );
        }

        // Add listener for input text
        addListenerForInputText();
    }

    private void addListenerForInputText() {
        binding.editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (binding.buttonVoice.getVisibility() == View.VISIBLE) {
                        // Animate out: fade out and scale down
                        binding.buttonVoice.animate()
                                .alpha(0f)
                                .scaleX(0f)
                                .scaleY(0f)
                                .setDuration(150)
                                .withEndAction(() -> binding.buttonVoice.setVisibility(View.GONE))
                                .start();
                    }
                } else {
                    if (binding.buttonVoice.getVisibility() == View.GONE) {
                        // Animate in: fade in and scale up
                        binding.buttonVoice.setVisibility(View.VISIBLE);
                        binding.buttonVoice.setAlpha(0f);
                        binding.buttonVoice.setScaleX(0f);
                        binding.buttonVoice.setScaleY(0f);
                        binding.buttonVoice.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start();
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // Set initial visibility based on current text
        if (binding.editMessage.getText() != null && binding.editMessage.getText().length() > 0) {
            binding.buttonVoice.setVisibility(View.GONE);
        } else {
            // Ensure button is fully visible and scaled correctly initially
            binding.buttonVoice.setAlpha(1f);
            binding.buttonVoice.setScaleX(1f);
            binding.buttonVoice.setScaleY(1f);
        }
    }

    private String convertStreamToString(InputStream is) throws Exception {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleUtil.getLanguageCode(AppCompatDelegate.getApplicationLocales()));
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.hint_ask_gemini));

        try {
            voiceInputLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.error_no_voice_recognizer, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String messageText = binding.editMessage.getText() != null
                ? binding.editMessage.getText().toString().trim()
                : "";

        if (messageText.isEmpty()) {
            return;
        }

        // Expand bottom sheet on first message
        if (isFirstMessage) {
            expandBottomSheet();
            isFirstMessage = false;
        }

        // Add user message to chat
        ChatMessage userMessage = new ChatMessage(messageText, true);
        adapter.addMessage(userMessage);
        binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);

        // Clear input
        binding.editMessage.setText("");

        // Disable send button while processing
        binding.buttonSend.setEnabled(false);

        // Add user message to history
        Content userMessageContent = Content.builder()
                .role("user")
                .parts(Part.fromText(messageText))
                .build();

        chatHistory.add(userMessageContent);

        // Add empty Gemini message placeholder for streaming
        ChatMessage geminiMessage = new ChatMessage("", false);
        geminiMessage.setLoading(true);
        adapter.addMessage(geminiMessage);
        binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);

        // Send to Gemini (non-streaming for function call support)
        CompletableFuture<GenerateContentResponse> responseFuture =
                client.async.models.generateContent("gemini-2.5-flash", chatHistory, config);

        responseFuture.thenAccept(response -> {
            executor.execute(() -> {
                try {
                    handleResponse(response);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            adapter.updateLastMessage("Sorry, I encountered an error processing the response.");
                            binding.buttonSend.setEnabled(true);
                        });
                    }
                }
            });
        }).exceptionally(throwable -> {
            Log.e(TAG, "Error getting response", throwable);
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    adapter.updateLastMessage("Sorry, I couldn't connect to Gemini: " + throwable.getMessage());
                    binding.buttonSend.setEnabled(true);
                });
            }
            return null;
        });
    }

    /**
     * Handles a response from Gemini, which may contain text or function calls.
     */
    private void handleResponse(GenerateContentResponse response) {
        // Check if response contains function calls
        List<FunctionCall> functionCalls = response.functionCalls();

        if (functionCalls != null && !functionCalls.isEmpty()) {
            // Execute all function calls
            Log.d(TAG, "Response contains " + functionCalls.size() + " function call(s)");
            handleFunctionCalls(functionCalls);
        } else {
            // Regular text response
            String responseText = response.text();
            if (responseText != null && !responseText.isEmpty()) {
                // Add response to chat history
                chatHistory.add(Content.builder()
                        .role("model")
                        .parts(Part.fromText(responseText))
                        .build());

                // Update UI
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateLastMessage(responseText);
                        binding.buttonSend.setEnabled(true);
                        binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
                        saveChatHistory();
                    });
                }
            } else {
                // Empty response
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateLastMessage("I don't have a response for that.");
                        binding.buttonSend.setEnabled(true);
                    });
                }
            }
        }
    }

    /**
     * Executes function calls and sends results back to Gemini.
     */
    private void handleFunctionCalls(List<FunctionCall> functionCalls) {
        List<CompletableFuture<Part>> futureResponses = new ArrayList<>();

        // Execute all function calls asynchronously
        for (FunctionCall functionCall : functionCalls) {
            String functionName = functionCall.name().orElse("unknown");
            Map<String, Object> argsMap = functionCall.args().orElse(new HashMap<>());

            // Convert Map to JSONObject for executor
            JSONObject args = new JSONObject(argsMap);

            // Execute the function and create a future for the Part
            CompletableFuture<Part> futurePart = functionExecutor.executeFunction(functionName, args)
                    .thenApply(result -> {
                        Log.d(TAG, "Function " + functionName + " returned: " + result);

                        // Create function response as Map
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("result", result);

                        return Part.fromFunctionResponse(functionName, responseMap);
                    }).exceptionally(throwable -> {
                        Log.e(TAG, "Error executing function " + functionName, throwable);

                        // On error, return a Part indicating the error
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("error", "Function execution failed: " + throwable.getMessage());
                        return Part.fromFunctionResponse(functionName, errorMap);
                    });

            futureResponses.add(futurePart);
        }

        // Wait for all function calls to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futureResponses.toArray(new CompletableFuture[0])
        );

        allFutures.thenAccept(v -> {
            executor.execute(() -> {
                try {
                    // Collect all the Part results
                    List<Part> functionResponseParts = new ArrayList<>();
                    for (CompletableFuture<Part> futurePart : futureResponses) {
                        functionResponseParts.add(futurePart.join());
                    }

                    // Add function responses to chat history
                    Content functionResponseContent = Content.builder()
                            .role("function")
                            .parts(functionResponseParts)
                            .build();

                    chatHistory.add(functionResponseContent);

                    // Get final response from Gemini with function results
                    CompletableFuture<GenerateContentResponse> finalResponseFuture =
                            client.async.models.generateContent("gemini-2.5-flash", chatHistory, config);

                    finalResponseFuture.thenAccept(finalResponse -> {
                        executor.execute(() -> {
                            try {
                                // Get the text response after function execution
                                String responseText = finalResponse.text();

                                if (responseText != null && !responseText.isEmpty()) {
                                    // Add final response to chat history
                                    chatHistory.add(Content.builder()
                                            .role("model")
                                            .parts(Part.fromText(responseText))
                                            .build());

                                    // Update UI with final response
                                    requireActivity().runOnUiThread(() -> {
                                        adapter.updateLastMessage(responseText);
                                        binding.buttonSend.setEnabled(true);
                                        binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
                                    });
                                } else {
                                    requireActivity().runOnUiThread(() -> {
                                        if (finalResponse.functionCalls() != null && !finalResponse.functionCalls().isEmpty()) {
                                            handleFunctionCalls(finalResponse.functionCalls());
                                        } else {
                                            adapter.updateLastMessage("OK.");
                                            binding.buttonSend.setEnabled(true);
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing final response", e);
                                requireActivity().runOnUiThread(() -> {
                                    adapter.updateLastMessage("Sorry, I encountered an error processing the final response.");
                                    binding.buttonSend.setEnabled(true);
                                });
                            }
                        });
                    }).exceptionally(throwable -> {
                        Log.e(TAG, "Error getting final response", throwable);
                        requireActivity().runOnUiThread(() -> {
                            adapter.updateLastMessage("Sorry, I couldn't get a final response: " + throwable.getMessage());
                            binding.buttonSend.setEnabled(true);
                        });
                        return null;
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error collecting function results", e);
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateLastMessage("Sorry, I encountered an error collecting function results.");
                        binding.buttonSend.setEnabled(true);
                    });
                }
            });
        }).exceptionally(throwable -> {
            Log.e(TAG, "Error executing function calls", throwable);
            requireActivity().runOnUiThread(() -> {
                adapter.updateLastMessage("Sorry, I couldn't execute the function calls: " + throwable.getMessage());
                binding.buttonSend.setEnabled(true);
            });
            return null;
        });
    }

    /**
     * Expands the bottom sheet to full height.
     */
    private void expandBottomSheet() {
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void clearChat() {
        // Clear messages
        messages.clear();
        chatHistory.clear();

        // Add welcome message back
        adapter.addMessage(new ChatMessage(
                getString(R.string.msg_gemini_welcome),
                false
        ));

        // Save empty chat history
        saveChatHistory();

        // Reset first message flag
        isFirstMessage = true;

        // Notify adapter
        adapter.notifyDataSetChanged();
    }

    private void saveChatHistory() {
        try {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

            // Save messages
            JSONArray messagesArray = new JSONArray();
            for (ChatMessage message : messages) {
                if (!message.isLoading()) { // Don't save loading messages
                    JSONObject messageObj = new JSONObject();
                    messageObj.put("message", message.getMessage());
                    messageObj.put("isUser", message.isUser());
                    messagesArray.put(messageObj);
                }
            }

            JSONObject chatData = new JSONObject();
            chatData.put("messages", messagesArray);

            sharedPrefs.edit()
                    .putString(Constants.SETTINGS.GEMINI.CHAT_HISTORY, chatData.toString())
                    .apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving chat history", e);
        }
    }

    private void loadChatHistory() {
        try {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String chatDataStr = sharedPrefs.getString(Constants.SETTINGS.GEMINI.CHAT_HISTORY, "");

            if (chatDataStr.isEmpty()) {
                return;
            }

            JSONObject chatData = new JSONObject(chatDataStr);

            // Load messages
            JSONArray messagesArray = chatData.getJSONArray("messages");
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject messageObj = messagesArray.getJSONObject(i);
                ChatMessage message = new ChatMessage(
                        messageObj.getString("message"),
                        messageObj.getBoolean("isUser")
                );
                messages.add(message);
            }

            // Update adapter
            adapter.notifyDataSetChanged();

            // If there are messages, expand the bottom sheet and set first message to false
            if (!messages.isEmpty()) {
                isFirstMessage = false;
                expandBottomSheet();
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error loading chat history", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            saveChatHistory();
        }
        binding = null;
    }

    @Override
    public void applyBottomInset(int bottom) {
        if (binding != null) {
            binding.linearInputContainer.setPadding(
                    binding.linearInputContainer.getPaddingLeft(),
                    binding.linearInputContainer.getPaddingTop(),
                    binding.linearInputContainer.getPaddingRight(),
                    bottom
            );
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
