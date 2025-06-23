package com.harsh.heyloo;

public class User {
    private String userId;
    private String userName;
    private String profileImageUrl;
    private String email;

    // Default constructor required for Firestore
    public User() {
    }

    public User(String userId, String userName, String profileImageUrl, String email) {
        this.userId = userId;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 