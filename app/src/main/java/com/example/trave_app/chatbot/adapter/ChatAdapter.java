package com.example.trave_app.chatbot.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave_app.R;
import com.example.trave_app.chatbot.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout userMessageLayout;
        private LinearLayout aiMessageLayout;
        private TextView userMessageText;
        private TextView aiMessageText;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userMessageLayout = itemView.findViewById(R.id.userMessageLayout);
            aiMessageLayout = itemView.findViewById(R.id.aiMessageLayout);
            userMessageText = itemView.findViewById(R.id.userMessageText);
            aiMessageText = itemView.findViewById(R.id.aiMessageText);
        }

        public void bind(ChatMessage message) {
            if (message.isUserMessage()) {
                userMessageLayout.setVisibility(View.VISIBLE);
                aiMessageLayout.setVisibility(View.GONE);
                userMessageText.setText(message.getMessage());
            } else {
                userMessageLayout.setVisibility(View.GONE);
                aiMessageLayout.setVisibility(View.VISIBLE);
                aiMessageText.setText(message.getMessage());
            }
        }
    }
}
