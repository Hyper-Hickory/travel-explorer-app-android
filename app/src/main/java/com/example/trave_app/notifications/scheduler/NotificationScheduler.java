package com.example.trave_app.notifications.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.trave_app.notifications.model.AINotification;
import com.example.trave_app.notifications.model.NotificationPreference;
import com.example.trave_app.notifications.service.NotificationReceiver;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private static NotificationScheduler instance;
    private final Context context;
    private final AlarmManager alarmManager;

    private NotificationScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static synchronized NotificationScheduler getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationScheduler(context);
        }
        return instance;
    }

    public void scheduleNotification(AINotification notification, NotificationPreference preferences) {
        if (!shouldScheduleNotification(notification, preferences)) {
            return;
        }

        long optimizedTime = optimizeDeliveryTime(notification.getScheduledTime(), preferences);
        notification.setScheduledTime(optimizedTime);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notification.getId());
        intent.putExtra("title", notification.getTitle());
        intent.putExtra("message", notification.getMessage());
        intent.putExtra("type", notification.getType().name());
        intent.putExtra("priority", notification.getPriority());
        intent.putExtra("related_place_id", notification.getRelatedPlaceId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, optimizedTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, optimizedTime, pendingIntent);
            }
        } catch (SecurityException e) {
            // Fallback to set() if exact alarms are not allowed
            alarmManager.set(AlarmManager.RTC_WAKEUP, optimizedTime, pendingIntent);
        }
    }

    public void scheduleMultipleNotifications(List<AINotification> notifications, NotificationPreference preferences) {
        // Sort notifications by priority and relevance
        notifications.sort((n1, n2) -> {
            int priorityCompare = Integer.compare(n2.getPriority(), n1.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            return Double.compare(n2.getRelevanceScore(), n1.getRelevanceScore());
        });

        // Apply daily limit
        int maxNotifications = Math.min(notifications.size(), preferences.getMaxDailyNotifications());
        List<AINotification> filteredNotifications = notifications.subList(0, maxNotifications);

        // Schedule with proper spacing
        long currentTime = System.currentTimeMillis();
        long minInterval = TimeUnit.MINUTES.toMillis(preferences.getMinTimeBetweenNotifications());

        for (int i = 0; i < filteredNotifications.size(); i++) {
            AINotification notification = filteredNotifications.get(i);
            
            // Ensure minimum time between notifications
            long scheduledTime = Math.max(notification.getScheduledTime(), 
                    currentTime + (i * minInterval));
            notification.setScheduledTime(scheduledTime);
            
            scheduleNotification(notification, preferences);
        }
    }

    public void cancelNotification(int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
    }

    public void cancelAllNotifications(List<Integer> notificationIds) {
        for (int id : notificationIds) {
            cancelNotification(id);
        }
    }

    private boolean shouldScheduleNotification(AINotification notification, NotificationPreference preferences) {
        // Check if notification type is enabled
        if (!preferences.isTypeEnabled(notification.getType())) {
            return false;
        }

        // Check priority threshold
        if (notification.getPriority() < preferences.getMinimumPriorityLevel()) {
            return false;
        }

        // Check relevance threshold
        if (notification.getRelevanceScore() < preferences.getMinimumRelevanceScore()) {
            return false;
        }

        return true;
    }

    private long optimizeDeliveryTime(long originalTime, NotificationPreference preferences) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(originalTime);

        // Apply quiet hours logic
        if (preferences.isRespectQuietHours()) {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int quietStart = preferences.getQuietHoursStart();
            int quietEnd = preferences.getQuietHoursEnd();

            if (isInQuietHours(hour, quietStart, quietEnd)) {
                // Move to end of quiet hours
                calendar.set(Calendar.HOUR_OF_DAY, quietEnd);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                
                // If quiet hours end is tomorrow, add a day
                if (quietEnd <= quietStart && hour >= quietStart) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        }

        // Apply optimal timing based on user patterns
        long optimizedTime = applyOptimalTiming(calendar.getTimeInMillis(), preferences);

        // Ensure it's not in the past
        return Math.max(optimizedTime, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    }

    private boolean isInQuietHours(int currentHour, int quietStart, int quietEnd) {
        if (quietStart < quietEnd) {
            // Quiet hours within same day (e.g., 1 AM to 6 AM)
            return currentHour >= quietStart && currentHour < quietEnd;
        } else {
            // Quiet hours span midnight (e.g., 10 PM to 8 AM)
            return currentHour >= quietStart || currentHour < quietEnd;
        }
    }

    private long applyOptimalTiming(long scheduledTime, NotificationPreference preferences) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(scheduledTime);
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        // Optimize based on notification effectiveness research
        // Peak engagement times: 9-11 AM, 1-3 PM, 7-9 PM
        int[] peakHours = {9, 10, 13, 14, 19, 20};
        
        // If not in peak hours and more than 2 hours away, try to move to next peak
        boolean isInPeakHour = Arrays.stream(peakHours).anyMatch(peakHour -> peakHour == hour);
        
        if (!isInPeakHour && scheduledTime > System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)) {
            // Find next peak hour
            int nextPeakHour = findNextPeakHour(hour, peakHours);
            if (nextPeakHour != -1) {
                calendar.set(Calendar.HOUR_OF_DAY, nextPeakHour);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                
                // If the peak hour is earlier in the day, move to next day
                if (nextPeakHour <= hour) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        }

        return calendar.getTimeInMillis();
    }

    private int findNextPeakHour(int currentHour, int[] peakHours) {
        for (int peakHour : peakHours) {
            if (peakHour > currentHour) {
                return peakHour;
            }
        }
        // If no peak hour found today, return first peak hour (for next day)
        return peakHours.length > 0 ? peakHours[0] : -1;
    }

    public void rescheduleBasedOnUserInteraction(int notificationId, boolean wasEngaged) {
        // This method can be called when user interacts with notifications
        // to learn and improve future scheduling
        
        if (wasEngaged) {
            // User engaged - this was a good time
            // Store this timing preference for future use
            recordSuccessfulTiming(notificationId);
        } else {
            // User dismissed - might want to avoid this timing
            recordDismissedTiming(notificationId);
        }
    }

    private void recordSuccessfulTiming(int notificationId) {
        // Implementation for learning successful timing patterns
        // This could store timing preferences in SharedPreferences or database
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        
        // Store successful timing data for ML learning
        // This is a placeholder for future ML-based timing optimization
    }

    private void recordDismissedTiming(int notificationId) {
        // Implementation for learning dismissed timing patterns
        // This could help avoid similar timings in the future
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        
        // Store dismissed timing data for ML learning
        // This is a placeholder for future ML-based timing optimization
    }

    public long getNextOptimalTime(NotificationPreference preferences) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 30); // Start checking from 30 minutes from now
        
        long optimizedTime = optimizeDeliveryTime(calendar.getTimeInMillis(), preferences);
        return optimizedTime;
    }

    public boolean isOptimalTimeForNotification(NotificationPreference preferences) {
        if (preferences.isInQuietHours()) {
            return false;
        }
        
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        
        // Check if current time is in peak engagement hours
        int[] peakHours = {9, 10, 13, 14, 19, 20};
        return Arrays.stream(peakHours).anyMatch(hour -> hour == currentHour);
    }
}
