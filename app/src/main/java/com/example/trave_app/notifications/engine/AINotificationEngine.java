package com.example.trave_app.notifications.engine;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.ml.engine.TravelRecommendationEngine;
import com.example.trave_app.ml.model.TravelPreference;
import com.example.trave_app.notifications.model.AINotification;
import com.example.trave_app.notifications.model.NotificationType;
import com.example.trave_app.notifications.model.NotificationPreference;
import com.example.trave_app.notifications.service.RealTimeLocationService;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AINotificationEngine {
    private static AINotificationEngine instance;
    private final Context context;
    private final TravelRecommendationEngine mlEngine;
    private final Random random;

    private AINotificationEngine(Context context) {
        this.context = context.getApplicationContext();
        this.mlEngine = TravelRecommendationEngine.getInstance(context);
        this.random = new Random();
    }

    public static synchronized AINotificationEngine getInstance(Context context) {
        if (instance == null) {
            instance = new AINotificationEngine(context);
        }
        return instance;
    }

    public List<AINotification> generateSmartRecommendations(List<Place> places, 
                                                           List<Favorite> favorites, 
                                                           TravelPreference userPreference,
                                                           Location currentLocation) {
        List<AINotification> notifications = new ArrayList<>();
        
        if (places.isEmpty()) return notifications;

        // Get ML-based recommendations
        List<Place> recommendations = mlEngine.getPersonalizedRecommendations(places, 5);
        
        for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
            Place place = recommendations.get(i);
            AINotification notification = createRecommendationNotification(place, userPreference, currentLocation);
            if (notification != null) {
                notifications.add(notification);
            }
        }

        return notifications;
    }

    public List<AINotification> generatePatternBasedAlerts(List<SearchHistory> searchHistory,
                                                          List<Favorite> favorites,
                                                          TravelPreference userPreference) {
        List<AINotification> notifications = new ArrayList<>();

        // Analyze search patterns
        Map<String, Integer> categoryFrequency = analyzeCategoryPatterns(searchHistory);
        String mostSearchedCategory = getMostFrequentCategory(categoryFrequency);

        if (mostSearchedCategory != null && categoryFrequency.get(mostSearchedCategory) >= 3) {
            AINotification notification = createPatternAlertNotification(mostSearchedCategory, categoryFrequency.get(mostSearchedCategory));
            if (notification != null) {
                notifications.add(notification);
            }
        }

        // Analyze time patterns
        AINotification timePatternNotification = analyzeTimePatterns(searchHistory);
        if (timePatternNotification != null) {
            notifications.add(timePatternNotification);
        }

        return notifications;
    }

    public List<AINotification> generateLocationAwareNotifications(Location currentLocation,
                                                                  List<Place> nearbyPlaces,
                                                                  List<Favorite> favorites) {
        List<AINotification> notifications = new ArrayList<>();

        if (currentLocation == null || nearbyPlaces.isEmpty()) return notifications;

        // Check for nearby favorites
        for (Favorite favorite : favorites) {
            for (Place place : nearbyPlaces) {
                if (place.getName().equals(favorite.getName())) {
                    AINotification notification = createLocationAwareNotification(place, favorite);
                    if (notification != null) {
                        notifications.add(notification);
                    }
                }
            }
        }

        // Suggest new places in current area
        if (nearbyPlaces.size() > 0) {
            Place suggestedPlace = nearbyPlaces.get(random.nextInt(nearbyPlaces.size()));
            AINotification notification = createNearbyPlaceNotification(suggestedPlace, currentLocation);
            if (notification != null) {
                notifications.add(notification);
            }
        }

        return notifications;
    }

    public List<AINotification> generateTravelInsights(List<Place> visitedPlaces,
                                                      List<Favorite> favorites,
                                                      List<SearchHistory> searchHistory) {
        List<AINotification> notifications = new ArrayList<>();

        // Weekly insights
        if (shouldGenerateWeeklyInsight()) {
            AINotification weeklyInsight = createWeeklyInsightNotification(visitedPlaces, favorites, searchHistory);
            if (weeklyInsight != null) {
                notifications.add(weeklyInsight);
            }
        }

        // Discovery insights
        AINotification discoveryInsight = createDiscoveryInsightNotification(visitedPlaces, favorites);
        if (discoveryInsight != null) {
            notifications.add(discoveryInsight);
        }

        return notifications;
    }

    public List<AINotification> generateSmartReminders(List<Favorite> favorites,
                                                      List<SearchHistory> recentSearches) {
        List<AINotification> notifications = new ArrayList<>();

        // Remind about unvisited favorites
        for (Favorite favorite : favorites) {
            if (shouldRemindAboutFavorite(favorite)) {
                AINotification reminder = createFavoriteReminderNotification(favorite);
                if (reminder != null) {
                    notifications.add(reminder);
                }
            }
        }

        // Follow up on recent searches
        for (SearchHistory search : recentSearches) {
            if (shouldFollowUpOnSearch(search)) {
                AINotification followUp = createSearchFollowUpNotification(search);
                if (followUp != null) {
                    notifications.add(followUp);
                }
            }
        }

        return notifications;
    }

    public void startRealTimeLocationTracking() {
        Log.d("AINotificationEngine", "Starting real-time location tracking service");
        
        Intent serviceIntent = new Intent(context, RealTimeLocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void stopRealTimeLocationTracking() {
        Log.d("AINotificationEngine", "Stopping real-time location tracking service");
        
        Intent serviceIntent = new Intent(context, RealTimeLocationService.class);
        context.stopService(serviceIntent);
    }

    private AINotification createRecommendationNotification(Place place, TravelPreference preference, Location currentLocation) {
        String title = " Perfect Match Found!";
        String message = String.format("Based on your preferences, you might love %s! It's a %s with %.1f★ rating.",
                place.getName(), place.getCategory(), place.getRating());

        AINotification notification = new AINotification(title, message, NotificationType.SMART_RECOMMENDATION, 
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(random.nextInt(60) + 30));
        
        notification.setRelatedPlaceId(String.valueOf(place.getId()));
        notification.setPriority(4);
        notification.setRelevanceScore(0.8 + random.nextDouble() * 0.2);
        notification.setCategory(place.getCategory());

        return notification;
    }

    private AINotification createPatternAlertNotification(String category, int frequency) {
        String title = " Pattern Detected!";
        String message = String.format("You've searched for %s places %d times recently. Here are some new suggestions!", 
                category, frequency);

        AINotification notification = new AINotification(title, message, NotificationType.PATTERN_ALERT,
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2));
        
        notification.setPriority(3);
        notification.setRelevanceScore(0.7);
        notification.setCategory(category);

        return notification;
    }

    private AINotification createLocationAwareNotification(Place place, Favorite favorite) {
        String title = " You're Near a Favorite!";
        String message = String.format("You're close to %s, one of your favorite places! Perfect time for a visit.", 
                place.getName());

        AINotification notification = new AINotification(title, message, NotificationType.LOCATION_AWARE,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
        
        notification.setRelatedPlaceId(String.valueOf(place.getId()));
        notification.setPriority(5);
        notification.setRelevanceScore(0.9);
        notification.setCategory(place.getCategory());

        return notification;
    }

    private AINotification createNearbyPlaceNotification(Place place, Location currentLocation) {
        String title = " Discover Something New!";
        String message = String.format("There's a highly-rated %s nearby: %s (%.1f★). Want to check it out?",
                place.getCategory(), place.getName(), place.getRating());

        AINotification notification = new AINotification(title, message, NotificationType.LOCATION_AWARE,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(random.nextInt(30) + 15));
        
        notification.setRelatedPlaceId(String.valueOf(place.getId()));
        notification.setPriority(3);
        notification.setRelevanceScore(0.6 + place.getRating() / 10.0);
        notification.setCategory(place.getCategory());

        return notification;
    }

    private AINotification createWeeklyInsightNotification(List<Place> visitedPlaces, List<Favorite> favorites, List<SearchHistory> searchHistory) {
        String title = " Your Weekly Travel Insights";
        String message = String.format("This week: %d places explored, %d new favorites, %d searches. You're becoming quite the explorer!",
                visitedPlaces.size(), favorites.size(), searchHistory.size());

        AINotification notification = new AINotification(title, message, NotificationType.TRAVEL_INSIGHT,
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        
        notification.setPriority(2);
        notification.setRelevanceScore(0.5);

        return notification;
    }

    private AINotification createDiscoveryInsightNotification(List<Place> visitedPlaces, List<Favorite> favorites) {
        if (visitedPlaces.isEmpty()) return null;

        Set<String> categories = new HashSet<>();
        for (Place place : visitedPlaces) {
            categories.add(place.getCategory());
        }

        String title = " Discovery Insight";
        String message = String.format("You've explored %d different types of places! Your travel diversity score is growing.",
                categories.size());

        AINotification notification = new AINotification(title, message, NotificationType.TRAVEL_INSIGHT,
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6));
        
        notification.setPriority(2);
        notification.setRelevanceScore(0.6);

        return notification;
    }

    private AINotification createFavoriteReminderNotification(Favorite favorite) {
        String title = " Favorite Place Reminder";
        String message = String.format("It's been a while since you visited %s. Maybe it's time for another visit?",
                favorite.getName());

        AINotification notification = new AINotification(title, message, NotificationType.SMART_REMINDER,
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(random.nextInt(12) + 6));
        
        notification.setPriority(2);
        notification.setRelevanceScore(0.4);
        notification.setCategory(favorite.getCategory());

        return notification;
    }

    private AINotification createSearchFollowUpNotification(SearchHistory search) {
        String title = " Search Follow-up";
        String message = String.format("Still looking for %s places? We found some new options that might interest you!",
                search.getSearchQuery());

        AINotification notification = new AINotification(title, message, NotificationType.SMART_REMINDER,
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(random.nextInt(24) + 12));
        
        notification.setPriority(2);
        notification.setRelevanceScore(0.5);

        return notification;
    }

    private AINotification analyzeTimePatterns(List<SearchHistory> searchHistory) {
        if (searchHistory.size() < 5) return null;

        Map<Integer, Integer> hourFrequency = new HashMap<>();
        for (SearchHistory search : searchHistory) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(search.getSearchTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourFrequency.put(hour, hourFrequency.getOrDefault(hour, 0) + 1);
        }

        int peakHour = Collections.max(hourFrequency.entrySet(), Map.Entry.comparingByValue()).getKey();
        
        String title = " Perfect Timing!";
        String message = String.format("You usually search for places around %d:00. Here are some timely suggestions!",
                peakHour);

        AINotification notification = new AINotification(title, message, NotificationType.TIME_OPTIMIZED,
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        
        notification.setPriority(3);
        notification.setRelevanceScore(0.7);

        return notification;
    }

    private Map<String, Integer> analyzeCategoryPatterns(List<SearchHistory> searchHistory) {
        Map<String, Integer> categoryFrequency = new HashMap<>();
        for (SearchHistory search : searchHistory) {
            String query = search.getSearchQuery().toLowerCase();
            String category = inferCategoryFromQuery(query);
            if (category != null) {
                categoryFrequency.put(category, categoryFrequency.getOrDefault(category, 0) + 1);
            }
        }
        return categoryFrequency;
    }

    private String getMostFrequentCategory(Map<String, Integer> categoryFrequency) {
        return categoryFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String inferCategoryFromQuery(String query) {
        if (query.contains("restaurant") || query.contains("food") || query.contains("eat")) return "restaurant";
        if (query.contains("hotel") || query.contains("stay") || query.contains("accommodation")) return "hotel";
        if (query.contains("cafe") || query.contains("coffee")) return "cafe";
        if (query.contains("park") || query.contains("garden")) return "park";
        if (query.contains("mall") || query.contains("shopping")) return "shopping_mall";
        if (query.contains("gas") || query.contains("fuel")) return "gas_station";
        return null;
    }

    private boolean shouldGenerateWeeklyInsight() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && cal.get(Calendar.HOUR_OF_DAY) >= 18;
    }

    private boolean shouldRemindAboutFavorite(Favorite favorite) {
        long daysSinceAdded = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - favorite.getAddedAt());
        return daysSinceAdded >= 7 && random.nextDouble() < 0.3;
    }

    private boolean shouldFollowUpOnSearch(SearchHistory search) {
        long hoursSinceSearch = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - search.getSearchTimestamp());
        return hoursSinceSearch >= 24 && hoursSinceSearch <= 72 && random.nextDouble() < 0.4;
    }
}
