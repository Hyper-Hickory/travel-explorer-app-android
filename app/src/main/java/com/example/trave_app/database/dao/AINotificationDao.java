package com.example.trave_app.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.trave_app.notifications.model.AINotification;
import com.example.trave_app.notifications.model.NotificationType;

import java.util.List;

@Dao
public interface AINotificationDao {
    
    @Insert
    void insert(AINotification notification);
    
    @Insert
    void insertAll(List<AINotification> notifications);
    
    @Update
    void update(AINotification notification);
    
    @Delete
    void delete(AINotification notification);
    
    @Query("DELETE FROM ai_notifications WHERE id = :id")
    void deleteById(int id);
    
    @Query("SELECT * FROM ai_notifications ORDER BY scheduledTime DESC")
    LiveData<List<AINotification>> getAllNotifications();
    
    @Query("SELECT * FROM ai_notifications WHERE isDelivered = 0 ORDER BY scheduledTime ASC")
    LiveData<List<AINotification>> getPendingNotifications();
    
    @Query("SELECT * FROM ai_notifications WHERE isDelivered = 1 ORDER BY scheduledTime DESC")
    LiveData<List<AINotification>> getDeliveredNotifications();
    
    @Query("SELECT * FROM ai_notifications WHERE type = :type ORDER BY scheduledTime DESC")
    LiveData<List<AINotification>> getNotificationsByType(NotificationType type);
    
    @Query("SELECT * FROM ai_notifications WHERE scheduledTime <= :currentTime AND isDelivered = 0")
    List<AINotification> getNotificationsDueForDelivery(long currentTime);
    
    @Query("SELECT * FROM ai_notifications WHERE priority >= :minPriority AND relevanceScore >= :minRelevance AND isDelivered = 0")
    List<AINotification> getHighPriorityNotifications(int minPriority, double minRelevance);
    
    @Query("SELECT * FROM ai_notifications WHERE relatedPlaceId = :placeId ORDER BY scheduledTime DESC")
    LiveData<List<AINotification>> getNotificationsForPlace(String placeId);
    
    @Query("SELECT COUNT(*) FROM ai_notifications WHERE DATE(scheduledTime/1000, 'unixepoch') = DATE('now') AND isDelivered = 1")
    int getTodayDeliveredCount();
    
    @Query("UPDATE ai_notifications SET isDelivered = 1 WHERE id = :id")
    void markAsDelivered(int id);
    
    @Query("UPDATE ai_notifications SET isRead = 1 WHERE id = :id")
    void markAsRead(int id);
    
    @Query("DELETE FROM ai_notifications WHERE scheduledTime < :cutoffTime AND isDelivered = 1")
    void deleteOldDeliveredNotifications(long cutoffTime);
    
    @Query("SELECT * FROM ai_notifications WHERE id = :id")
    AINotification getNotificationById(int id);
}
