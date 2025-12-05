package com.example.trave_app.notifications.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;
import com.example.trave_app.database.Converters;

@Entity(tableName = "ai_notifications")
@TypeConverters(Converters.class)
public class AINotification {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String message;
    private NotificationType type;
    private long scheduledTime;
    private long createdTime;
    private boolean isDelivered;
    private boolean isRead;
    private int priority; // 1-5, 5 being highest
    private String relatedPlaceId;
    private String actionData; // JSON string for action-specific data
    private double relevanceScore;
    private String category;

    public AINotification() {
        this.createdTime = System.currentTimeMillis();
        this.isDelivered = false;
        this.isRead = false;
        this.priority = 3; // Default medium priority
        this.relevanceScore = 0.5; // Default relevance
    }

    @Ignore
    public AINotification(String title, String message, NotificationType type, long scheduledTime) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
        this.scheduledTime = scheduledTime;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public long getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }

    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getRelatedPlaceId() { return relatedPlaceId; }
    public void setRelatedPlaceId(String relatedPlaceId) { this.relatedPlaceId = relatedPlaceId; }

    public String getActionData() { return actionData; }
    public void setActionData(String actionData) { this.actionData = actionData; }

    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean shouldDeliverNow() {
        return !isDelivered && scheduledTime <= System.currentTimeMillis();
    }

    public String getFormattedTitle() {
        return type != null ? type.getFormattedTitle() : title;
    }
}
