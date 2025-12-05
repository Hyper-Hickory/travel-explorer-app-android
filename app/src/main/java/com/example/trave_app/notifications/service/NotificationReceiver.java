package com.example.trave_app.notifications.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.trave_app.MainActivity;
import com.example.trave_app.R;
import com.example.trave_app.notifications.model.NotificationType;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "ai_travel_notifications";
    private static final String CHANNEL_NAME = "AI Travel Notifications";
    private static final String CHANNEL_DESCRIPTION = "Smart travel recommendations and alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        
        int notificationId = intent.getIntExtra("notification_id", 0);
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String typeString = intent.getStringExtra("type");
        int priority = intent.getIntExtra("priority", 3);
        String relatedPlaceId = intent.getStringExtra("related_place_id");

        if (title == null || message == null) {
            return;
        }

        NotificationType type = NotificationType.valueOf(typeString);
        showNotification(context, notificationId, title, message, type, priority, relatedPlaceId);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription(CHANNEL_DESCRIPTION);
                channel.enableLights(true);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, int notificationId, String title, String message, 
                                NotificationType type, int priority, String relatedPlaceId) {
        
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Add extra data for handling notification tap
        if (relatedPlaceId != null) {
            mainIntent.putExtra("related_place_id", relatedPlaceId);
            mainIntent.putExtra("notification_type", type.name());
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create action buttons based on notification type
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(getNotificationIcon(type))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(getNotificationPriority(priority))
                .setCategory(getNotificationCategory(type));

        // Add action buttons based on notification type
        addActionButtons(context, builder, type, relatedPlaceId, notificationId);

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }

    private int getNotificationIcon(NotificationType type) {
        // Using Android's built-in icons since we don't have custom ones
        switch (type) {
            case SMART_RECOMMENDATION:
                return android.R.drawable.ic_dialog_info;
            case PATTERN_ALERT:
                return android.R.drawable.ic_menu_report_image;
            case LOCATION_AWARE:
                return android.R.drawable.ic_dialog_map;
            case TIME_OPTIMIZED:
                return android.R.drawable.ic_menu_recent_history;
            case TRAVEL_INSIGHT:
                return android.R.drawable.ic_menu_info_details;
            case FAVORITE_UPDATE:
                return android.R.drawable.btn_star_big_on;
            case SMART_REMINDER:
                return android.R.drawable.ic_popup_reminder;
            case WEATHER_ALERT:
                return android.R.drawable.ic_dialog_alert;
            default:
                return android.R.drawable.ic_dialog_info;
        }
    }

    private int getNotificationPriority(int priority) {
        switch (priority) {
            case 5: return NotificationCompat.PRIORITY_MAX;
            case 4: return NotificationCompat.PRIORITY_HIGH;
            case 3: return NotificationCompat.PRIORITY_DEFAULT;
            case 2: return NotificationCompat.PRIORITY_LOW;
            case 1: return NotificationCompat.PRIORITY_MIN;
            default: return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    private String getNotificationCategory(NotificationType type) {
        switch (type) {
            case SMART_RECOMMENDATION:
            case PATTERN_ALERT:
            case TRAVEL_INSIGHT:
                return NotificationCompat.CATEGORY_RECOMMENDATION;
            case LOCATION_AWARE:
                return NotificationCompat.CATEGORY_LOCATION_SHARING;
            case TIME_OPTIMIZED:
            case SMART_REMINDER:
                return NotificationCompat.CATEGORY_REMINDER;
            case FAVORITE_UPDATE:
                return NotificationCompat.CATEGORY_SOCIAL;
            case WEATHER_ALERT:
                return NotificationCompat.CATEGORY_ALARM;
            default:
                return NotificationCompat.CATEGORY_RECOMMENDATION;
        }
    }

    private void addActionButtons(Context context, NotificationCompat.Builder builder, 
                                NotificationType type, String relatedPlaceId, int notificationId) {
        
        // Common "View Details" action
        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.putExtra("action", "view_details");
        viewIntent.putExtra("related_place_id", relatedPlaceId);
        viewIntent.putExtra("notification_type", type.name());
        
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
            context, 
            notificationId * 10 + 1, 
            viewIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(android.R.drawable.ic_menu_view, "View", viewPendingIntent);

        // Type-specific actions
        switch (type) {
            case SMART_RECOMMENDATION:
            case LOCATION_AWARE:
                // Add "Navigate" action for location-based notifications
                Intent navigateIntent = new Intent(context, MainActivity.class);
                navigateIntent.putExtra("action", "navigate");
                navigateIntent.putExtra("related_place_id", relatedPlaceId);
                
                PendingIntent navigatePendingIntent = PendingIntent.getActivity(
                    context, 
                    notificationId * 10 + 2, 
                    navigateIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                builder.addAction(android.R.drawable.ic_menu_directions, "Navigate", navigatePendingIntent);
                break;

            case FAVORITE_UPDATE:
                // Add "Add to Favorites" action
                Intent favoriteIntent = new Intent(context, MainActivity.class);
                favoriteIntent.putExtra("action", "add_favorite");
                favoriteIntent.putExtra("related_place_id", relatedPlaceId);
                
                PendingIntent favoritePendingIntent = PendingIntent.getActivity(
                    context, 
                    notificationId * 10 + 3, 
                    favoriteIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                builder.addAction(android.R.drawable.btn_star_big_off, "Favorite", favoritePendingIntent);
                break;

            case SMART_REMINDER:
                // Add "Snooze" action for reminders
                Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
                snoozeIntent.putExtra("action", "snooze");
                snoozeIntent.putExtra("notification_id", notificationId);
                
                PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                    context, 
                    notificationId * 10 + 4, 
                    snoozeIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent);
                break;
        }

        // Common "Dismiss" action
        Intent dismissIntent = new Intent(context, NotificationActionReceiver.class);
        dismissIntent.putExtra("action", "dismiss");
        dismissIntent.putExtra("notification_id", notificationId);
        
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
            context, 
            notificationId * 10 + 5, 
            dismissIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent);
    }
}
