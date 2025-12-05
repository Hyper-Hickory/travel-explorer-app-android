package com.example.trave_app.notifications.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import com.example.trave_app.notifications.scheduler.NotificationScheduler;
import com.example.trave_app.notifications.model.AINotification;
import com.example.trave_app.notifications.model.NotificationType;
import java.util.concurrent.TimeUnit;

public class NotificationActionReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        int notificationId = intent.getIntExtra("notification_id", 0);
        
        if (action == null) return;
        
        switch (action) {
            case "snooze":
                handleSnoozeAction(context, notificationId);
                break;
            case "dismiss":
                handleDismissAction(context, notificationId);
                break;
            default:
                break;
        }
    }
    
    private void handleSnoozeAction(Context context, int notificationId) {
        // Cancel current notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        
        // Reschedule for 1 hour later
        NotificationScheduler scheduler = NotificationScheduler.getInstance(context);
        
        // Create a simple snooze notification
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.putExtra("notification_id", notificationId + 10000); // Different ID for snooze
        snoozeIntent.putExtra("title", "🔔 Snoozed Reminder");
        snoozeIntent.putExtra("message", "Your travel reminder is back! Ready to explore?");
        snoozeIntent.putExtra("type", NotificationType.SMART_REMINDER.name());
        snoozeIntent.putExtra("priority", 3);
        
        // Schedule for 1 hour later
        long snoozeTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        
        android.app.AlarmManager alarmManager = 
            (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            snoozeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
        }
        
        // Record user interaction for learning
        NotificationScheduler.getInstance(context).rescheduleBasedOnUserInteraction(notificationId, false);
    }
    
    private void handleDismissAction(Context context, int notificationId) {
        // Cancel notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        
        // Record user interaction for learning
        NotificationScheduler.getInstance(context).rescheduleBasedOnUserInteraction(notificationId, false);
    }
}
