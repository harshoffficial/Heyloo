package com.harsh.heyloo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private Context context;
    private FirebaseUser currentUser;
    private OnMessageLongClickListener longClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message);
    }

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        try {
            if (position < 0 || position >= messageList.size()) {
                System.out.println("MessageAdapter - Invalid position: " + position + ", list size: " + messageList.size());
                return VIEW_TYPE_RECEIVED; // Default to received if position is invalid
            }
            Message message = messageList.get(position);
            if (message != null && message.getSenderId() != null && currentUser != null) {
                boolean isSent = message.getSenderId().equals(currentUser.getUid());
                System.out.println("MessageAdapter - Message: " + message.getMessageText() + 
                                 ", Sender: " + message.getSenderId() + 
                                 ", Current User: " + currentUser.getUid() + 
                                 ", Is Sent: " + isSent);
                if (isSent) {
                    return VIEW_TYPE_SENT;
                } else {
                    return VIEW_TYPE_RECEIVED;
                }
            }
            System.out.println("MessageAdapter - Message or sender is null, defaulting to received");
            return VIEW_TYPE_RECEIVED; // Default to received if message is null
        } catch (Exception e) {
            System.out.println("MessageAdapter - Error in getItemViewType: " + e.getMessage());
            return VIEW_TYPE_RECEIVED; // Default to received on error
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view;
            System.out.println("MessageAdapter - Creating ViewHolder with viewType: " + viewType);
            if (viewType == VIEW_TYPE_SENT) {
                System.out.println("MessageAdapter - Inflating sent message layout");
                view = LayoutInflater.from(context).inflate(R.layout.chatting_send, parent, false);
                return new SentMessageViewHolder(view);
            } else {
                System.out.println("MessageAdapter - Inflating received message layout");
                view = LayoutInflater.from(context).inflate(R.layout.chatting_receive, parent, false);
                return new ReceivedMessageViewHolder(view);
            }
        } catch (Exception e) {
            System.out.println("MessageAdapter - Error in onCreateViewHolder: " + e.getMessage());
            // Fallback to received layout
            View view = LayoutInflater.from(context).inflate(R.layout.chatting_receive, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            if (position < 0 || position >= messageList.size()) {
                return;
            }
            Message message = messageList.get(position);
            if (message == null) {
                return;
            }
            
            if (holder.getItemViewType() == VIEW_TYPE_SENT) {
                ((SentMessageViewHolder) holder).bind(message);
                // Add long click listener for sent messages
                holder.itemView.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(message);
                    }
                    return true;
                });
            } else {
                ((ReceivedMessageViewHolder) holder).bind(message);
                // Add long click listener for received messages
                holder.itemView.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(message);
                    }
                    return true;
                });
            }
        } catch (Exception e) {
            System.out.println("MessageAdapter - Error in onBindViewHolder: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessages(List<Message> messageList) {
        System.out.println("MessageAdapter - setMessages called with " + (messageList != null ? messageList.size() : 0) + " messages");
        this.messageList = messageList;
        notifyDataSetChanged();
        System.out.println("MessageAdapter - notifyDataSetChanged called");
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    // ViewHolder for sent messages
    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
        }

        public void bind(Message message) {
            try {
                if (messageText != null && message != null && message.getMessageText() != null) {
                    messageText.setText(message.getMessageText());
                }
                
                if (messageTime != null && message != null && message.getTimestamp() != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        messageTime.setText(sdf.format(message.getTimestamp().toDate()));
                    } catch (Exception e) {
                        System.out.println("SentMessageViewHolder - Error formatting time: " + e.getMessage());
                        messageTime.setText("");
                    }
                }
            } catch (Exception e) {
                System.out.println("SentMessageViewHolder - Error in bind: " + e.getMessage());
            }
        }
    }

    // ViewHolder for received messages
    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageTime;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
        }

        public void bind(Message message) {
            try {
                if (messageText != null && message != null && message.getMessageText() != null) {
                    messageText.setText(message.getMessageText());
                }
                
                if (messageTime != null && message != null && message.getTimestamp() != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        messageTime.setText(sdf.format(message.getTimestamp().toDate()));
                    } catch (Exception e) {
                        System.out.println("ReceivedMessageViewHolder - Error formatting time: " + e.getMessage());
                        messageTime.setText("");
                    }
                }
            } catch (Exception e) {
                System.out.println("ReceivedMessageViewHolder - Error in bind: " + e.getMessage());
            }
        }
    }
} 