package com.example.trave_app.notifications.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class RealTimeNotificationManager {
    private static final String REALTIME_CHANNEL_ID = "realtime_places_channel";
    private static final String LOCATION_TRACKING_CHANNEL_ID = "location_tracking_channel";
    
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Real-time places notification channel
            NotificationChannel realtimeChannel = new NotificationChannel(
                REALTIME_CHANNEL_ID,
                "Real-time Place Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            realtimeChannel.setDescription("Notifications about nearby places with ratings and locations");
            realtimeChannel.enableVibration(true);
            realtimeChannel.enableLights(true);
            manager.createNotificationChannel(realtimeChannel);
            
            // Location tracking service channel
            NotificationChannel trackingChannel = new NotificationChannel(
                LOCATION_TRACKING_CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            );
            trackingChannel.setDescription("Background location tracking for place notifications");
            trackingChannel.setShowBadge(false);
            manager.createNotificationChannel(trackingChannel);
        }
    }
}
