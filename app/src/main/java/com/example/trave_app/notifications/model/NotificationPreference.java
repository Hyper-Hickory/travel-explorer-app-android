package com.example.trave_app.notifications.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.trave_app.database.Converters;

@Entity(tableName = "notification_preferences")
@TypeConverters(Converters.class)
public class NotificationPreference {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private boolean smartRecommendationsEnabled;
    private boolean patternAlertsEnabled;
    private boolean locationAwareEnabled;
    private boolean travelInsightsEnabled;
    private boolean favoriteUpdatesEnabled;
    private boolean smartRemindersEnabled;
    private boolean weatherAlertsEnabled;
    
    // Time preferences
    private int quietHoursStart; // Hour in 24-hour format (e.g., 22 for 10 PM)
    private int quietHoursEnd;   // Hour in 24-hour format (e.g., 8 for 8 AM)
    private boolean respectQuietHours;
    
    // Frequency preferences
    private int maxDailyNotifications;
    private int minTimeBetweenNotifications; // Minutes
    
    // Priority thresholds
    private int minimumPriorityLevel; // 1-5, notifications below this won't be shown
    private double minimumRelevanceScore; // 0.0-1.0
    
    // Location preferences
    private boolean enableLocationBasedTiming;
    private boolean realTimeTrackingEnabled;
    private double locationRadiusKm; // Radius for location-based notifications
    
    public NotificationPreference() {
        // Default settings - all enabled with reasonable defaults
        this.smartRecommendationsEnabled = true;
        this.patternAlertsEnabled = true;
        this.locationAwareEnabled = true;
        this.travelInsightsEnabled = true;
        this.favoriteUpdatesEnabled = true;
        this.smartRemindersEnabled = true;
        this.weatherAlertsEnabled = true;
        
        this.quietHoursStart = 22; // 10 PM
        this.quietHoursEnd = 8;    // 8 AM
        this.respectQuietHours = true;
        
        this.maxDailyNotifications = 10;
        this.minTimeBetweenNotifications = 30; // 30 minutes
        
        this.minimumPriorityLevel = 2;
        this.minimumRelevanceScore = 0.3;
        
        this.enableLocationBasedTiming = true;
        this.realTimeTrackingEnabled = true;
        this.locationRadiusKm = 5.0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isSmartRecommendationsEnabled() { return smartRecommendationsEnabled; }
    public void setSmartRecommendationsEnabled(boolean smartRecommendationsEnabled) { 
        this.smartRecommendationsEnabled = smartRecommendationsEnabled; 
    }

    public boolean isPatternAlertsEnabled() { return patternAlertsEnabled; }
    public void setPatternAlertsEnabled(boolean patternAlertsEnabled) { 
        this.patternAlertsEnabled = patternAlertsEnabled; 
    }

    public boolean isLocationAwareEnabled() { return locationAwareEnabled; }
    public void setLocationAwareEnabled(boolean locationAwareEnabled) { 
        this.locationAwareEnabled = locationAwareEnabled; 
    }

    public boolean isTravelInsightsEnabled() { return travelInsightsEnabled; }
    public void setTravelInsightsEnabled(boolean travelInsightsEnabled) { 
        this.travelInsightsEnabled = travelInsightsEnabled; 
    }

    public boolean isFavoriteUpdatesEnabled() { return favoriteUpdatesEnabled; }
    public void setFavoriteUpdatesEnabled(boolean favoriteUpdatesEnabled) { 
        this.favoriteUpdatesEnabled = favoriteUpdatesEnabled; 
    }

    public boolean isSmartRemindersEnabled() { return smartRemindersEnabled; }
    public void setSmartRemindersEnabled(boolean smartRemindersEnabled) { 
        this.smartRemindersEnabled = smartRemindersEnabled; 
    }

    public boolean isWeatherAlertsEnabled() { return weatherAlertsEnabled; }
    public void setWeatherAlertsEnabled(boolean weatherAlertsEnabled) { 
        this.weatherAlertsEnabled = weatherAlertsEnabled; 
    }

    public int getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(int quietHoursStart) { this.quietHoursStart = quietHoursStart; }

    public int getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(int quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }

    public boolean isRespectQuietHours() { return respectQuietHours; }
    public void setRespectQuietHours(boolean respectQuietHours) { this.respectQuietHours = respectQuietHours; }

    public int getMaxDailyNotifications() { return maxDailyNotifications; }
    public void setMaxDailyNotifications(int maxDailyNotifications) { 
        this.maxDailyNotifications = maxDailyNotifications; 
    }

    public int getMinTimeBetweenNotifications() { return minTimeBetweenNotifications; }
    public void setMinTimeBetweenNotifications(int minTimeBetweenNotifications) { 
        this.minTimeBetweenNotifications = minTimeBetweenNotifications; 
    }

    public int getMinimumPriorityLevel() { return minimumPriorityLevel; }
    public void setMinimumPriorityLevel(int minimumPriorityLevel) { 
        this.minimumPriorityLevel = minimumPriorityLevel; 
    }

    public double getMinimumRelevanceScore() { return minimumRelevanceScore; }
    public void setMinimumRelevanceScore(double minimumRelevanceScore) { 
        this.minimumRelevanceScore = minimumRelevanceScore; 
    }

    public boolean isEnableLocationBasedTiming() { return enableLocationBasedTiming; }
    public void setEnableLocationBasedTiming(boolean enableLocationBasedTiming) { 
        this.enableLocationBasedTiming = enableLocationBasedTiming; 
    }

    public boolean isRealTimeTrackingEnabled() {
        return realTimeTrackingEnabled;
    }

    public void setRealTimeTrackingEnabled(boolean realTimeTrackingEnabled) {
        this.realTimeTrackingEnabled = realTimeTrackingEnabled;
    }

    public double getLocationRadiusKm() { return locationRadiusKm; }
    public void setLocationRadiusKm(double locationRadiusKm) { this.locationRadiusKm = locationRadiusKm; }

    public boolean isTypeEnabled(NotificationType type) {
        switch (type) {
            case SMART_RECOMMENDATION: return smartRecommendationsEnabled;
            case PATTERN_ALERT: return patternAlertsEnabled;
            case LOCATION_AWARE: return locationAwareEnabled;
            case TRAVEL_INSIGHT: return travelInsightsEnabled;
            case FAVORITE_UPDATE: return favoriteUpdatesEnabled;
            case SMART_REMINDER: return smartRemindersEnabled;
            case WEATHER_ALERT: return weatherAlertsEnabled;
            default: return true;
        }
    }

    public boolean isInQuietHours() {
        if (!respectQuietHours) return false;
        
        int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        if (quietHoursStart < quietHoursEnd) {
            // Same day quiet hours (e.g., 22 to 8 next day)
            return currentHour >= quietHoursStart || currentHour < quietHoursEnd;
        } else {
            // Quiet hours span midnight (e.g., 10 PM to 8 AM)
            return currentHour >= quietHoursStart && currentHour < quietHoursEnd;
        }
    }
}
