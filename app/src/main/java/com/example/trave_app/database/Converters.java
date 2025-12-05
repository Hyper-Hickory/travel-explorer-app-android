package com.example.trave_app.database;

import androidx.room.TypeConverter;
import com.example.trave_app.notifications.model.NotificationType;

public class Converters {
    
    @TypeConverter
    public static String fromNotificationType(NotificationType type) {
        return type == null ? null : type.name();
    }
    
    @TypeConverter
    public static NotificationType toNotificationType(String typeString) {
        return typeString == null ? null : NotificationType.valueOf(typeString);
    }
}
