package com.example.trave_app.notifications.model;

public enum NotificationType {
    SMART_RECOMMENDATION("Smart Travel Recommendation", "🎯"),
    PATTERN_ALERT("Pattern-Based Alert", "📊"),
    LOCATION_AWARE("Location-Aware Notification", "📍"),
    TIME_OPTIMIZED("Time-Optimized Alert", "⏰"),
    TRAVEL_INSIGHT("Travel Insight", "💡"),
    FAVORITE_UPDATE("Favorite Place Update", "❤️"),
    SMART_REMINDER("Smart Reminder", "🔔"),
    WEATHER_ALERT("Weather Alert", "🌤️");

    private final String displayName;
    private final String emoji;

    NotificationType(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getFormattedTitle() {
        return emoji + " " + displayName;
    }
}
