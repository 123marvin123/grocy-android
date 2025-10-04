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

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ChatMessage;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

  private final List<ChatMessage> messages;
  private final Context context;

  public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
    this.context = context;
    this.messages = messages;
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
    holder.textMessage.setText(message.getMessage());
    
    if (message.isUser()) {
      holder.textSender.setText(R.string.sender_you);
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardMessage.getLayoutParams();
      params.gravity = Gravity.END;
      holder.cardMessage.setLayoutParams(params);
    } else {
      holder.textSender.setText(R.string.sender_gemini);
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

  static class MessageViewHolder extends RecyclerView.ViewHolder {
    MaterialCardView cardMessage;
    TextView textSender;
    TextView textMessage;

    MessageViewHolder(@NonNull View itemView) {
      super(itemView);
      cardMessage = itemView.findViewById(R.id.card_message);
      textSender = itemView.findViewById(R.id.text_sender);
      textMessage = itemView.findViewById(R.id.text_message);
    }
  }
}
