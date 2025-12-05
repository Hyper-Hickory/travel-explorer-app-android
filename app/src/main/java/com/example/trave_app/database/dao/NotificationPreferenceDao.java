package com.example.trave_app.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.trave_app.notifications.model.NotificationPreference;

@Dao
public interface NotificationPreferenceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotificationPreference preference);
    
    @Update
    void update(NotificationPreference preference);
    
    @Query("SELECT * FROM notification_preferences LIMIT 1")
    LiveData<NotificationPreference> getPreferences();
    
    @Query("SELECT * FROM notification_preferences LIMIT 1")
    NotificationPreference getPreferencesSync();
    
    @Query("DELETE FROM notification_preferences")
    void deleteAll();
}
