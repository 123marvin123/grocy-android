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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.ChatMessageAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetGeminiChatBinding;
import xyz.zedler.patrick.grocy.model.ChatMessage;

public class GeminiChatBottomSheet extends BaseBottomSheetDialogFragment {

  private static final String TAG = GeminiChatBottomSheet.class.getSimpleName();

  private FragmentBottomsheetGeminiChatBinding binding;
  private ChatMessageAdapter adapter;
  private List<ChatMessage> messages;
  private List<Content> chatHistory;
  private GenerativeModelFutures model;
  private final Executor executor = Executors.newSingleThreadExecutor();

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
    binding = FragmentBottomsheetGeminiChatBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
    
    // Check if Gemini is enabled and API key is set
    boolean geminiEnabled = sharedPrefs.getBoolean(Constants.PREF.GEMINI_ENABLED, false);
    String apiKey = sharedPrefs.getString(Constants.PREF.GEMINI_API_KEY, "");
    
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
    GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
    model = GenerativeModelFutures.from(gm);
    chatHistory = new ArrayList<>();

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
    Content.Builder userMessageBuilder = new Content.Builder();
    userMessageBuilder.setRole("user");
    userMessageBuilder.addText(messageText);
    chatHistory.add(userMessageBuilder.build());

    // Send to Gemini
    Content[] contentArray = chatHistory.toArray(new Content[0]);
    ListenableFuture<GenerateContentResponse> response = model.generateContent(contentArray);
    
    Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
      @Override
      public void onSuccess(GenerateContentResponse result) {
        try {
          String responseText = result.getText();
          
          // Add response to history
          Content.Builder modelMessageBuilder = new Content.Builder();
          modelMessageBuilder.setRole("model");
          modelMessageBuilder.addText(responseText);
          chatHistory.add(modelMessageBuilder.build());
          
          requireActivity().runOnUiThread(() -> {
            ChatMessage geminiMessage = new ChatMessage(responseText, false);
            adapter.addMessage(geminiMessage);
            binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
            binding.buttonSend.setEnabled(true);
          });
        } catch (Exception e) {
          Log.e(TAG, "Error processing Gemini response", e);
          requireActivity().runOnUiThread(() -> {
            ChatMessage errorMessage = new ChatMessage(
                "Sorry, I encountered an error processing the response.", 
                false
            );
            adapter.addMessage(errorMessage);
            binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
            binding.buttonSend.setEnabled(true);
          });
        }
      }

      @Override
      public void onFailure(Throwable t) {
        Log.e(TAG, "Error sending message to Gemini", t);
        requireActivity().runOnUiThread(() -> {
          ChatMessage errorMessage = new ChatMessage(
              "Sorry, I couldn't connect to Gemini: " + t.getMessage(), 
              false
          );
          adapter.addMessage(errorMessage);
          binding.recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
          binding.buttonSend.setEnabled(true);
        });
      }
    }, executor);
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
