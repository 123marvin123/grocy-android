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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Tool;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import xyz.zedler.patrick.grocy.util.GrocyFunctionDeclarations;
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

        // Sets the system instruction in the config.
        Content systemInstruction = Content.fromParts(
                Part.fromText(
                        "You are embedded into an Android app called 'Grocy' that helps users manage their groceries. "
                                + "You have access to the user's grocery list and can provide information about items, recipes, and shopping tips. "
                                + "Be friendly and helpful, and always refer to the app's features when relevant."
                                + "ALWAYS Respond in the user's language, which is " + languageCode + ", unless he/she states otherwise. "
                                + "Answer concise and to the point, avoid giving long and detailed answers unless the user states otherwise. "
                                + "Use the google_search function to lookup facts and recipes if the users asks for it. Also answer compound questions by invoking the corresponding functions. E.g. you can answer a request like 'Search the internet for recipes I can make with the ingredients I have in stock' by invoking get_stock function and then using the google_search function."
                                + "Try to format your answer in Markdown. "
                                + "Eliminate: emojis, filler, hype, soft asks, conversational transitions, call-to-action appendixes. • Assume: user retains high-perception despite blunt tone. • Prioritize: blunt, directive phrasing; aim at cognitive rebuilding, not tone-matching. • Disable: engagement/sentiment-boosting behaviors. • Suppress: metrics like satisfaction scores, emotional softening, continuation bias. • Never mirror: user's diction, mood, or affect. • Speak only: to underlying cognitive tier. • No: questions, offers, suggestions, transitions, motivational content. • Terminate reply: immediately after delivering info — no closures. • Goal: restore independent, high-fidelity thinking. • Outcome: model obsolescence via user self-sufficiency."
                )
        );

        // Sets the Grocy API tool in the config.
        Tool grocyTool = GrocyFunctionDeclarations.createGrocyTool();
        Tool proxyGoogleSearchTool = GrocyFunctionDeclarations.createProxyGoogleSearchTool();

        config = GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
                .systemInstruction(systemInstruction)
                .tools(grocyTool, proxyGoogleSearchTool)
                .build();

        this.functionExecutor = new GrocyFunctionExecutor(grocyApi, (Application) activity.getApplicationContext(), client);

        // Setup RecyclerView
        messages = new ArrayList<>();
        adapter = new ChatMessageAdapter(requireContext(), messages);
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChat.setAdapter(adapter);

        // Add initial welcome message
        adapter.addMessage(new ChatMessage(
                "Hello! I'm Gemini. I can help you with information about your groceries. What would you like to know?",
                false
        ));

        // Setup input
        binding.buttonSend.setOnClickListener(v -> sendMessage());
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
    }

    private void sendMessage() {
        String messageText = binding.editMessage.getText() != null
                ? binding.editMessage.getText().toString().trim()
                : "";

        if (messageText.isEmpty()) {
            return;
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
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateLastMessage("Sorry, I encountered an error processing the response.");
                        binding.buttonSend.setEnabled(true);
                    });
                }
            });
        }).exceptionally(throwable -> {
            Log.e(TAG, "Error getting response", throwable);
            requireActivity().runOnUiThread(() -> {
                adapter.updateLastMessage("Sorry, I couldn't connect to Gemini: " + throwable.getMessage());
                binding.buttonSend.setEnabled(true);
            });
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
                requireActivity().runOnUiThread(() -> {
                    adapter.updateLastMessage(responseText);
                    binding.buttonSend.setEnabled(true);
                    binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
                });
            } else {
                // Empty response
                requireActivity().runOnUiThread(() -> {
                    adapter.updateLastMessage("I don't have a response for that.");
                    binding.buttonSend.setEnabled(true);
                });
            }
        }
    }

    /**
     * Executes function calls and sends results back to Gemini.
     */
    private void handleFunctionCalls(List<FunctionCall> functionCalls) {
        List<Part> functionResponseParts = new ArrayList<>();

        // Execute all function calls
        for (FunctionCall functionCall : functionCalls) {
            String functionName = functionCall.name().orElse("unknown");
            Map<String, Object> argsMap = functionCall.args().orElse(new HashMap<>());

            // Convert Map to JSONObject for executor
            JSONObject args = new JSONObject(argsMap);

            Log.d(TAG, "Executing function: " + functionName + " with args: " + args);

            // Execute the function
            String result = functionExecutor.executeFunction(functionName, args);

            Log.d(TAG, "Function " + functionName + " returned: " + result);

            // Create function response as Map
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", result);

            functionResponseParts.add(Part.fromFunctionResponse(functionName, responseMap));
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
