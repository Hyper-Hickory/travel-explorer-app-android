package com.example.trave_app.ml.engine;

import android.content.Context;
import android.util.Log;

import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.ml.model.TravelPreference;

import java.util.*;
import java.util.stream.Collectors;

public class TravelRecommendationEngine {
    private static final String TAG = "TravelRecommendationEngine";
    private static TravelRecommendationEngine instance;
    private Context context;
    private TravelPreference userPreference;

    // ML weights for different factors
    private static final double CATEGORY_WEIGHT = 0.4;
    private static final double RATING_WEIGHT = 0.3;
    private static final double FREQUENCY_WEIGHT = 0.2;
    private static final double RECENCY_WEIGHT = 0.1;

    private TravelRecommendationEngine(Context context) {
        this.context = context;
        this.userPreference = new TravelPreference("default_user");
    }

    public static synchronized TravelRecommendationEngine getInstance(Context context) {
        if (instance == null) {
            instance = new TravelRecommendationEngine(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Learn from user behavior - places visited, favorites, searches
     */
    public void learnFromUserBehavior(List<Place> visitedPlaces, List<Favorite> favorites, 
                                    List<SearchHistory> searchHistory) {
        Log.d(TAG, "Learning from user behavior...");
        
        // Learn from visited places
        for (Place place : visitedPlaces) {
            userPreference.updateCategoryPreference(place.getCategory(), 
                place.getRating() > 0 ? place.getRating() / 5.0 : 0.5);
            userPreference.incrementVisitFrequency(place.getCategory());
        }

        // Learn from favorites (higher weight)
        for (Favorite favorite : favorites) {
            userPreference.updateCategoryPreference(favorite.getCategory(), 1.5);
            userPreference.incrementVisitFrequency(favorite.getCategory());
        }

        // Learn from search patterns
        Map<String, Integer> searchCounts = new HashMap<>();
        for (SearchHistory search : searchHistory) {
            String query = search.getSearchQuery().toLowerCase();
            String inferredCategory = inferCategoryFromSearch(query);
            if (inferredCategory != null) {
                searchCounts.put(inferredCategory, 
                    searchCounts.getOrDefault(inferredCategory, 0) + 1);
            }
        }

        // Update preferences based on search patterns
        for (Map.Entry<String, Integer> entry : searchCounts.entrySet()) {
            userPreference.updateCategoryPreference(entry.getKey(), 
                entry.getValue() * 0.3); // Lower weight for searches
        }

        userPreference.normalizePreferences();
        Log.d(TAG, "User preferences updated: " + userPreference.getCategoryPreferences());
    }

    /**
     * Generate personalized recommendations based on ML analysis
     */
    public List<Place> getPersonalizedRecommendations(List<Place> allPlaces, int maxResults) {
        if (allPlaces.isEmpty()) {
            return new ArrayList<>();
        }

        Log.d(TAG, "Generating personalized recommendations...");
        
        List<PlaceScore> scoredPlaces = new ArrayList<>();
        
        for (Place place : allPlaces) {
            double score = calculateRecommendationScore(place);
            scoredPlaces.add(new PlaceScore(place, score));
        }

        // Sort by score (highest first) and return top results
        return scoredPlaces.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(maxResults)
                .map(ps -> ps.place)
                .collect(Collectors.toList());
    }

    /**
     * Calculate ML-based recommendation score for a place
     */
    private double calculateRecommendationScore(Place place) {
        double score = 0.0;

        // Category preference score
        double categoryScore = userPreference.getCategoryPreference(place.getCategory());
        score += categoryScore * CATEGORY_WEIGHT;

        // Rating score (normalized)
        double ratingScore = place.getRating() / 5.0;
        score += ratingScore * RATING_WEIGHT;

        // Frequency score (how often user visits this category)
        int frequency = userPreference.getVisitFrequency().getOrDefault(place.getCategory(), 0);
        double frequencyScore = Math.min(frequency / 10.0, 1.0); // Cap at 1.0
        score += frequencyScore * FREQUENCY_WEIGHT;

        // Recency bonus (newer places get slight boost)
        double recencyScore = 0.5; // Default neutral score
        score += recencyScore * RECENCY_WEIGHT;

        return score;
    }

    /**
     * Predict next likely destination category based on user patterns
     */
    public String predictNextDestinationCategory() {
        Map<String, Double> preferences = userPreference.getCategoryPreferences();
        
        if (preferences.isEmpty()) {
            return "restaurants"; // Default fallback
        }

        return preferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("restaurants");
    }

    /**
     * Get smart search suggestions based on ML analysis
     */
    public List<String> getSmartSearchSuggestions(String partialQuery) {
        List<String> suggestions = new ArrayList<>();
        String query = partialQuery.toLowerCase().trim();

        // Category-based suggestions
        Map<String, Double> preferences = userPreference.getCategoryPreferences();
        List<String> topCategories = preferences.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String category : topCategories) {
            if (category.toLowerCase().contains(query) || query.isEmpty()) {
                suggestions.add(capitalizeFirst(category));
            }
        }

        // Add contextual suggestions
        if (query.contains("food") || query.contains("eat")) {
            suggestions.addAll(Arrays.asList("Restaurants near me", "Best cafes", "Local cuisine"));
        } else if (query.contains("stay") || query.contains("hotel")) {
            suggestions.addAll(Arrays.asList("Hotels nearby", "Budget hostels", "Luxury accommodations"));
        } else if (query.contains("fun") || query.contains("activity")) {
            suggestions.addAll(Arrays.asList("Tourist attractions", "Parks and recreation", "Entertainment"));
        }

        return suggestions.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * Analyze travel patterns and provide insights
     */
    public Map<String, Object> analyzeTravelPatterns() {
        Map<String, Object> insights = new HashMap<>();
        
        Map<String, Double> preferences = userPreference.getCategoryPreferences();
        Map<String, Integer> frequency = userPreference.getVisitFrequency();

        // Most preferred category
        String topCategory = preferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        // Most visited category
        String mostVisited = frequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        // Travel diversity score (how varied are the preferences)
        double diversityScore = calculateDiversityScore(preferences);

        insights.put("topPreference", capitalizeFirst(topCategory));
        insights.put("mostVisited", capitalizeFirst(mostVisited));
        insights.put("diversityScore", diversityScore);
        insights.put("totalVisits", frequency.values().stream().mapToInt(Integer::intValue).sum());
        insights.put("preferenceStrength", preferences.getOrDefault(topCategory, 0.0));

        return insights;
    }

    // Helper methods
    private String inferCategoryFromSearch(String query) {
        query = query.toLowerCase();
        
        if (query.contains("restaurant") || query.contains("food") || query.contains("eat")) {
            return "restaurants";
        } else if (query.contains("cafe") || query.contains("coffee")) {
            return "cafes";
        } else if (query.contains("hotel") || query.contains("stay")) {
            return "hotels";
        } else if (query.contains("hostel")) {
            return "hostels";
        } else if (query.contains("mall") || query.contains("shop")) {
            return "malls";
        } else if (query.contains("park") || query.contains("garden")) {
            return "parks";
        } else if (query.contains("gas") || query.contains("fuel")) {
            return "gas_stations";
        } else if (query.contains("parking")) {
            return "parking";
        }
        
        return null;
    }

    private double calculateDiversityScore(Map<String, Double> preferences) {
        if (preferences.isEmpty()) return 0.0;
        
        // Calculate entropy-based diversity score
        double entropy = 0.0;
        for (double preference : preferences.values()) {
            if (preference > 0) {
                entropy -= preference * Math.log(preference) / Math.log(2);
            }
        }
        
        // Normalize to 0-1 scale
        double maxEntropy = Math.log(preferences.size()) / Math.log(2);
        return maxEntropy > 0 ? entropy / maxEntropy : 0.0;
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // Inner class for scoring places
    private static class PlaceScore {
        Place place;
        double score;

        PlaceScore(Place place, double score) {
            this.place = place;
            this.score = score;
        }
    }

    // Getters
    public TravelPreference getUserPreference() {
        return userPreference;
    }

    public void setUserPreference(TravelPreference preference) {
        this.userPreference = preference;
    }
}
