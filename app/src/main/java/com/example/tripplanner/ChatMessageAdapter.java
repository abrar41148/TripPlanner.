package com.example.tripplanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    public interface OnItineraryActionListener {
        void onSave(int position);
        void onRedo(int position);
    }

    private List<ChatMessage> messages;
    private OnItineraryActionListener listener;

    private static final int TYPE_USER = ChatMessage.TYPE_USER;
    private static final int TYPE_BOT = ChatMessage.TYPE_BOT;
    private static final int TYPE_BOT_ITINERARY = ChatMessage.TYPE_BOT_ITINERARY;

    public ChatMessageAdapter(List<ChatMessage> messages, OnItineraryActionListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView;
        
        if (viewType == TYPE_USER) {
            itemView = inflater.inflate(R.layout.item_chat_user, parent, false);
        } else if (viewType == TYPE_BOT_ITINERARY) {
            itemView = inflater.inflate(R.layout.item_chat_bot_itinerary, parent, false);
        } else {
            itemView = inflater.inflate(R.layout.item_chat_bot, parent, false);
        }
        
        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (message.message.contains("<") && message.message.contains(">")) {
            holder.tvMessage.setText(android.text.Html.fromHtml(message.message, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvMessage.setText(message.message);
        }

        if (getItemViewType(position) == TYPE_BOT_ITINERARY) {
            if (message.actionTaken) {
                if (holder.btnSave != null) holder.btnSave.setEnabled(false);
                if (holder.btnRedo != null) holder.btnRedo.setEnabled(false);
                if (holder.llItineraryButtons != null) holder.llItineraryButtons.setAlpha(0.5f);
            } else {
                if (holder.btnSave != null) {
                    holder.btnSave.setEnabled(true);
                    holder.btnSave.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onSave(position);
                        }
                    });
                }
                if (holder.btnRedo != null) {
                    holder.btnRedo.setEnabled(true);
                    holder.btnRedo.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onRedo(position);
                        }
                    });
                }
                if (holder.llItineraryButtons != null) holder.llItineraryButtons.setAlpha(1.0f);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        View btnSave;
        View btnRedo;
        View llItineraryButtons;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            btnSave = itemView.findViewById(R.id.btnSave);
            btnRedo = itemView.findViewById(R.id.btnRedo);
            llItineraryButtons = itemView.findViewById(R.id.llItineraryButtons);
        }
    }
}
