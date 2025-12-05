package com.example.trave_app.notifications.service;

import android.content.Context;
import android.location.Location;
import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.ml.engine.TravelRecommendationEngine;
import com.example.trave_app.ml.model.TravelPreference;
import com.example.trave_app.notifications.engine.AINotificationEngine;
import com.example.trave_app.notifications.scheduler.NotificationScheduler;
import com.example.trave_app.notifications.model.AINotification;
import com.example.trave_app.notifications.model.NotificationPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AINotificationService {
    private static AINotificationService instance;
    private final Context context;
    private final TravelDatabase database;
    private final AINotificationEngine notificationEngine;
    private final NotificationScheduler scheduler;
    private final ExecutorService executorService;

    private AINotificationService(Context context) {
        this.context = context.getApplicationContext();
        this.database = TravelDatabase.getDatabase(context);
        this.notificationEngine = AINotificationEngine.getInstance(context);
        this.scheduler = NotificationScheduler.getInstance(context);
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public static synchronized AINotificationService getInstance(Context context) {
        if (instance == null) {
            instance = new AINotificationService(context);
        }
        return instance;
    }

    public void generateAndScheduleNotifications() {
        executorService.execute(() -> {
            try {
                // Get user preferences
                NotificationPreference preferences = database.notificationPreferenceDao().getPreferencesSync();
                if (preferences == null) {
                    preferences = new NotificationPreference();
                    database.notificationPreferenceDao().insert(preferences);
                }

                // Get data from database
                List<Place> places = getAllPlacesSync();
                List<Favorite> favorites = getAllFavoritesSync();
                List<SearchHistory> searchHistory = getRecentSearchHistorySync();
                TravelPreference userPreference = createUserPreference(places, favorites, searchHistory);

                // Generate notifications
                List<AINotification> allNotifications = new ArrayList<>();

                // Smart recommendations
                if (preferences.isSmartRecommendationsEnabled()) {
                    List<AINotification> recommendations = notificationEngine.generateSmartRecommendations(
                            places, favorites, userPreference, null);
                    allNotifications.addAll(recommendations);
                }

                // Pattern-based alerts
                if (preferences.isPatternAlertsEnabled()) {
                    List<AINotification> patternAlerts = notificationEngine.generatePatternBasedAlerts(
                            searchHistory, favorites, userPreference);
                    allNotifications.addAll(patternAlerts);
                }

                // Travel insights
                if (preferences.isTravelInsightsEnabled()) {
                    List<AINotification> insights = notificationEngine.generateTravelInsights(
                            places, favorites, searchHistory);
                    allNotifications.addAll(insights);
                }

                // Smart reminders
                if (preferences.isSmartRemindersEnabled()) {
                    List<AINotification> reminders = notificationEngine.generateSmartReminders(
                            favorites, searchHistory);
                    allNotifications.addAll(reminders);
                }

                // Save notifications to database
                if (!allNotifications.isEmpty()) {
                    database.aiNotificationDao().insertAll(allNotifications);
                    
                    // Schedule notifications
                    scheduler.scheduleMultipleNotifications(allNotifications, preferences);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void generateLocationBasedNotifications(Location currentLocation) {
        if (currentLocation == null) return;

        executorService.execute(() -> {
            try {
                NotificationPreference preferences = database.notificationPreferenceDao().getPreferencesSync();
                if (preferences == null || !preferences.isLocationAwareEnabled()) {
                    return;
                }

                List<Place> nearbyPlaces = getNearbyPlacesSync(currentLocation, preferences.getLocationRadiusKm());
                List<Favorite> favorites = getAllFavoritesSync();

                List<AINotification> locationNotifications = notificationEngine.generateLocationAwareNotifications(
                        currentLocation, nearbyPlaces, favorites);

                if (!locationNotifications.isEmpty()) {
                    database.aiNotificationDao().insertAll(locationNotifications);
                    scheduler.scheduleMultipleNotifications(locationNotifications, preferences);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void processScheduledNotifications() {
        executorService.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                List<AINotification> dueNotifications = database.aiNotificationDao()
                        .getNotificationsDueForDelivery(currentTime);

                NotificationPreference preferences = database.notificationPreferenceDao().getPreferencesSync();
                if (preferences == null) return;

                for (AINotification notification : dueNotifications) {
                    if (shouldDeliverNotification(notification, preferences)) {
                        // Mark as delivered in database
                        database.aiNotificationDao().markAsDelivered(notification.getId());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void cleanupOldNotifications() {
        executorService.execute(() -> {
            try {
                // Delete notifications older than 30 days
                long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                database.aiNotificationDao().deleteOldDeliveredNotifications(cutoffTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void updateNotificationPreferences(NotificationPreference preferences) {
        executorService.execute(() -> {
            try {
                database.notificationPreferenceDao().update(preferences);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean shouldDeliverNotification(AINotification notification, NotificationPreference preferences) {
        // Check daily limit
        int todayCount = database.aiNotificationDao().getTodayDeliveredCount();
        if (todayCount >= preferences.getMaxDailyNotifications()) {
            return false;
        }

        // Check quiet hours
        if (preferences.isInQuietHours()) {
            return false;
        }

        // Check priority and relevance thresholds
        if (notification.getPriority() < preferences.getMinimumPriorityLevel()) {
            return false;
        }

        if (notification.getRelevanceScore() < preferences.getMinimumRelevanceScore()) {
            return false;
        }

        return true;
    }

    private List<Place> getAllPlacesSync() {
        try {
            // This is a simplified version - in real implementation you'd use proper async handling
            return new ArrayList<>(); // Placeholder
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Favorite> getAllFavoritesSync() {
        try {
            return new ArrayList<>(); // Placeholder
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<SearchHistory> getRecentSearchHistorySync() {
        try {
            return new ArrayList<>(); // Placeholder
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Place> getNearbyPlacesSync(Location location, double radiusKm) {
        try {
            return new ArrayList<>(); // Placeholder
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private TravelPreference createUserPreference(List<Place> places, List<Favorite> favorites, List<SearchHistory> searchHistory) {
        TravelPreference preference = new TravelPreference();
        
        // Analyze user preferences from data
        if (!favorites.isEmpty()) {
            // Set preferences based on favorite categories
            for (Favorite favorite : favorites) {
                String category = favorite.getCategory();
                if (category != null) {
                    switch (category.toLowerCase()) {
                        case "restaurant":
                            preference.updateCategoryPreference("restaurants", 0.9);
                            break;
                        case "hotel":
                            preference.updateCategoryPreference("hotels", 0.8);
                            break;
                        case "cafe":
                            preference.updateCategoryPreference("cafes", 0.7);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return preference;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
