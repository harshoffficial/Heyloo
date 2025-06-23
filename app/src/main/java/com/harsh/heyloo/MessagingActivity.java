package com.harsh.heyloo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MessagingActivity extends AppCompatActivity {

    private String selectedUserId;
    private String selectedUserName;
    private String selectedUserProfileImage;
    private String selectedUserEmail;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // UI Components
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageView sendButton;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private List<String> loadedMessageIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            System.out.println("MessagingActivity - Starting onCreate");
        setContentView(R.layout.activity_messaging);
            System.out.println("MessagingActivity - Layout set successfully");

            // Initialize Firebase
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            currentUser = auth.getCurrentUser();

            // Get user information from intent
            selectedUserId = getIntent().getStringExtra("selected_user_id");
            selectedUserName = getIntent().getStringExtra("selected_user_name");
            selectedUserProfileImage = getIntent().getStringExtra("selected_user_profile_image");
            selectedUserEmail = getIntent().getStringExtra("selected_user_email");

            System.out.println("MessagingActivity - User ID: " + selectedUserId);
            System.out.println("MessagingActivity - User Name: " + selectedUserName);
            System.out.println("MessagingActivity - User Profile Image: " + selectedUserProfileImage);
            System.out.println("MessagingActivity - User Email: " + selectedUserEmail);

            // Initialize UI components
            initializeViews();
            setupRecyclerView();
            loadProfileImage();
            loadMessages();

            System.out.println("MessagingActivity - onCreate completed successfully");

        } catch (Exception e) {
            System.out.println("Error in MessagingActivity onCreate: " + e.getMessage());
            e.printStackTrace();
            finish();
        }
    }

    private void initializeViews() {
        TextView userNameTextView = findViewById(R.id.user_name);
        com.google.android.material.imageview.ShapeableImageView profileImageView = findViewById(R.id.profile_image);
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageInput = findViewById(R.id.message);
        sendButton = findViewById(R.id.send);

        // Set user name
        if (selectedUserName != null) {
            userNameTextView.setText(selectedUserName);
            System.out.println("MessagingActivity - User name set: " + selectedUserName);
        }

        // Set up send button click listener
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);
        
        // Set up long click listener for message deletion
        messageAdapter.setOnMessageLongClickListener(message -> {
            showDeleteMessageDialog(message);
        });
    }

    private void loadProfileImage() {
        com.google.android.material.imageview.ShapeableImageView profileImageView = findViewById(R.id.profile_image);
        
        if (selectedUserProfileImage != null && !selectedUserProfileImage.isEmpty()) {
            System.out.println("MessagingActivity - Loading profile image: " + selectedUserProfileImage);
            Picasso.get()
                    .load(selectedUserProfileImage)
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .resize(200, 200)
                    .centerCrop()
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            System.out.println("MessagingActivity - Profile image loaded successfully");
                        }

                        @Override
                        public void onError(Exception e) {
                            System.out.println("MessagingActivity - Profile image loading failed: " + e.getMessage());
                        }
                    });
        } else {
            System.out.println("MessagingActivity - Setting default profile image");
            profileImageView.setImageResource(R.drawable.person);
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (currentUser == null || selectedUserId == null) {
            System.out.println("MessagingActivity - Current user or selected user is null");
            return;
        }

        try {
            // Create message object
            Message message = new Message();
            message.setSenderId(currentUser.getUid());
            message.setReceiverId(selectedUserId);
            message.setMessageText(messageText);
            message.setTimestamp(Timestamp.now());
            message.setRead(false);

            // Save to Firebase
            DocumentReference messageRef = db.collection("messages").document();
            message.setMessageId(messageRef.getId());
            
            messageRef.set(message)
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("MessagingActivity - Message sent successfully");
                        try {
                            // Clear input immediately
                            if (messageInput != null) {
                                messageInput.setText("");
                            }
                            // Don't add to adapter here - let the Firebase listener handle it
                        } catch (Exception e) {
                            System.out.println("MessagingActivity - Error updating UI after send: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("MessagingActivity - Failed to send message: " + e.getMessage());
                    });
        } catch (Exception e) {
            System.out.println("MessagingActivity - Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMessages() {
        if (currentUser == null || selectedUserId == null) {
            System.out.println("MessagingActivity - Cannot load messages: currentUser or selectedUserId is null");
            return;
        }

        try {
            // Query messages between current user and selected user
            // Use a simpler approach to get all messages between these two users
            db.collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            System.out.println("MessagingActivity - Error loading messages: " + error.getMessage());
                            return;
                        }

                        try {
                            if (value != null && messageAdapter != null) {
                                List<Message> messages = new ArrayList<>();
                                for (var document : value) {
                                    try {
                                        Message message = document.toObject(Message.class);
                                        if (message != null) {
                                            message.setMessageId(document.getId());
                                            messages.add(message);
                                        }
                                    } catch (Exception e) {
                                        System.out.println("MessagingActivity - Error parsing message: " + e.getMessage());
                                    }
                                }
                                
                                // Filter messages to only include conversation between these two users
                                List<Message> conversationMessages = new ArrayList<>();
                                for (Message message : messages) {
                                    try {
                                        if (message.getSenderId() != null && message.getReceiverId() != null) {
                                            // Check if this message is between current user and selected user
                                            boolean isFromCurrentUser = message.getSenderId().equals(currentUser.getUid()) && 
                                                                       message.getReceiverId().equals(selectedUserId);
                                            boolean isToCurrentUser = message.getSenderId().equals(selectedUserId) && 
                                                                     message.getReceiverId().equals(currentUser.getUid());
                                            
                                            if (isFromCurrentUser || isToCurrentUser) {
                                                conversationMessages.add(message);
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println("MessagingActivity - Error filtering message: " + e.getMessage());
                                    }
                                }
                                
                                System.out.println("MessagingActivity - Total conversation messages: " + conversationMessages.size());
                                
                                // Check if the message list has actually changed by comparing message IDs
                                List<String> newMessageIds = new ArrayList<>();
                                for (Message message : conversationMessages) {
                                    if (message.getMessageId() != null) {
                                        newMessageIds.add(message.getMessageId());
                                    }
                                }
                                
                                // Only update if the message IDs have changed
                                if (!newMessageIds.equals(loadedMessageIds)) {
                                    loadedMessageIds = newMessageIds;
                                    messageAdapter.setMessages(conversationMessages);
                                    if (!conversationMessages.isEmpty() && messagesRecyclerView != null) {
                                        messagesRecyclerView.post(() -> {
                                            try {
                                                messagesRecyclerView.smoothScrollToPosition(conversationMessages.size() - 1);
                                            } catch (Exception e) {
                                                System.out.println("MessagingActivity - Error scrolling to new messages: " + e.getMessage());
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("MessagingActivity - Error processing messages: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.out.println("MessagingActivity - Error setting up message listener: " + e.getMessage());
        }
    }

    private void showDeleteMessageDialog(Message message) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteMessage(message);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void deleteMessage(Message message) {
        if (message == null || message.getMessageId() == null) {
            System.out.println("MessagingActivity - Cannot delete message: message or messageId is null");
            return;
        }

        try {
            // Delete from Firebase
            db.collection("messages").document(message.getMessageId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("MessagingActivity - Message deleted successfully");
                        // The Firebase listener will automatically update the UI
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("MessagingActivity - Failed to delete message: " + e.getMessage());
                        // Show error message to user
                        android.widget.Toast.makeText(this, "Failed to delete message", android.widget.Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            System.out.println("MessagingActivity - Error deleting message: " + e.getMessage());
            android.widget.Toast.makeText(this, "Error deleting message", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    public void onBackClick(View view) {
        System.out.println("MessagingActivity - Back button clicked");
        finish();
    }
}