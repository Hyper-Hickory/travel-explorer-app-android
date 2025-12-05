package com.example.trave_app.firebase.model;

import java.util.HashMap;
import java.util.Map;

public class FirebaseUser {
    private String username;
    private String phone;
    private String location;
    private long createdAt;
    private Long lastLoginAt;

    public FirebaseUser() {}

    public FirebaseUser(String username, String phone, String location, long createdAt, Long lastLoginAt) {
        this.username = username;
        this.phone = phone;
        this.location = location;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("phone", phone);
        map.put("location", location);
        map.put("createdAt", createdAt);
        if (lastLoginAt != null) {
            map.put("lastLoginAt", lastLoginAt);
        }
        return map;
    }

    // getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
