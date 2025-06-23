package com.harsh.heyloo;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String messageText;
    private Timestamp timestamp;
    private boolean isRead;

    // Default constructor required for Firestore
    public Message() {
    }

    public Message(String messageId, String senderId, String receiverId, String messageText, Timestamp timestamp, boolean isRead) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
} 