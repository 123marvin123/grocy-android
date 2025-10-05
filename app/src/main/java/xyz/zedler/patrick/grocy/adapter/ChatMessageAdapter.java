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

package xyz.zedler.patrick.grocy.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ChatMessage;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

  private final List<ChatMessage> messages;
  private final Context context;
  private final Markwon markwon;

  public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
    this.context = context;
    this.messages = messages;
    // Initialize Markwon with linkify, table and HTML support
    this.markwon = Markwon.builder(context)
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .build();
  }

  @NonNull
  @Override
  public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_gemini_message, parent, false);
    return new MessageViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
    ChatMessage message = messages.get(position);
    
    // Handle loading state
    if (message.isLoading()) {
      holder.progressLoading.setVisibility(View.VISIBLE);
      holder.textMessage.setVisibility(View.GONE);
      holder.buttonCopy.setVisibility(View.GONE);
    } else {
      holder.progressLoading.setVisibility(View.GONE);
      holder.textMessage.setVisibility(View.VISIBLE);
      // Use Markwon to render markdown text
      markwon.setMarkdown(holder.textMessage, message.getMessage());
    }

    if (message.isUser()) {
      holder.imageSender.setImageResource(R.drawable.ic_round_person_anim);
      holder.textSender.setText(R.string.sender_you);
      holder.buttonCopy.setVisibility(View.GONE);
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardMessage.getLayoutParams();
      params.gravity = Gravity.END;
      holder.cardMessage.setLayoutParams(params);
    } else {
      holder.imageSender.setImageResource(R.drawable.ic_round_gemini);
      holder.textSender.setText(R.string.sender_gemini);
      
      if (!message.isLoading()) {
        holder.buttonCopy.setVisibility(View.VISIBLE);

        // Set up copy button click listener
        holder.buttonCopy.setOnClickListener(v -> {
          ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("Gemini Message", message.getMessage());
          clipboard.setPrimaryClip(clip);

          Toast.makeText(context, R.string.msg_copied_clipboard, Toast.LENGTH_SHORT).show();
        });
      }

      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardMessage.getLayoutParams();
      params.gravity = Gravity.START;
      holder.cardMessage.setLayoutParams(params);
    }
  }

  @Override
  public int getItemCount() {
    return messages.size();
  }

  public void addMessage(ChatMessage message) {
    messages.add(message);
    notifyItemInserted(messages.size() - 1);
  }

  public void updateLastMessage(String newText) {
    if (!messages.isEmpty()) {
      int lastIndex = messages.size() - 1;
      messages.get(lastIndex).setMessage(newText);
      messages.get(lastIndex).setLoading(false);
      notifyItemChanged(lastIndex);
    }
  }

  static class MessageViewHolder extends RecyclerView.ViewHolder {
    MaterialCardView cardMessage;
    TextView textSender;
    TextView textMessage;
    MaterialButton buttonCopy;
    ImageView imageSender;
    ProgressBar progressLoading;

    MessageViewHolder(@NonNull View itemView) {
      super(itemView);
      cardMessage = itemView.findViewById(R.id.card_message);
      textSender = itemView.findViewById(R.id.text_sender);
      textMessage = itemView.findViewById(R.id.text_message);
      buttonCopy = itemView.findViewById(R.id.button_copy);
      imageSender = itemView.findViewById(R.id.image_sender);
      progressLoading = itemView.findViewById(R.id.progress_loading);
    }
  }
}