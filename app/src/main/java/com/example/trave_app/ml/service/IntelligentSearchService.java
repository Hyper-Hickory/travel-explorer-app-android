package com.example.trave_app.ml.service;

import android.content.Context;
import android.util.Log;

import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.ml.engine.TravelRecommendationEngine;

import java.util.*;
import java.util.stream.Collectors;

public class IntelligentSearchService {
    private static final String TAG = "IntelligentSearchService";
    private static IntelligentSearchService instance;
    private Context context;
    private TravelRecommendationEngine recommendationEngine;

    // Search ranking weights
    private static final double NAME_MATCH_WEIGHT = 0.4;
    private static final double CATEGORY_MATCH_WEIGHT = 0.3;
    private static final double RATING_WEIGHT = 0.2;
    private static final double PERSONALIZATION_WEIGHT = 0.1;

    private IntelligentSearchService(Context context) {
        this.context = context;
        this.recommendationEngine = TravelRecommendationEngine.getInstance(context);
    }

    public static synchronized IntelligentSearchService getInstance(Context context) {
        if (instance == null) {
            instance = new IntelligentSearchService(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Perform intelligent search with ML-based ranking
     */
    public List<Place> performIntelligentSearch(String query, List<Place> allPlaces, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            // Return personalized recommendations for empty query
            return recommendationEngine.getPersonalizedRecommendations(allPlaces, maxResults);
        }

        Log.d(TAG, "Performing intelligent search for: " + query);
        
        String normalizedQuery = query.toLowerCase().trim();
        List<SearchResult> searchResults = new ArrayList<>();

        for (Place place : allPlaces) {
            double relevanceScore = calculateRelevanceScore(place, normalizedQuery);
            if (relevanceScore > 0) {
                searchResults.add(new SearchResult(place, relevanceScore));
            }
        }

        // Sort by relevance score and return top results
        return searchResults.stream()
                .sorted((a, b) -> Double.compare(b.relevanceScore, a.relevanceScore))
                .limit(maxResults)
                .map(sr -> sr.place)
                .collect(Collectors.toList());
    }

    /**
     * Calculate ML-based relevance score for search results
     */
    private double calculateRelevanceScore(Place place, String query) {
        double score = 0.0;

        // Name matching score
        double nameScore = calculateNameMatchScore(place.getName(), query);
        score += nameScore * NAME_MATCH_WEIGHT;

        // Category matching score
        double categoryScore = calculateCategoryMatchScore(place.getCategory(), query);
        score += categoryScore * CATEGORY_MATCH_WEIGHT;

        // Rating score (normalized)
        double ratingScore = place.getRating() / 5.0;
        score += ratingScore * RATING_WEIGHT;

        // Personalization score (user preference for this category)
        double personalizationScore = recommendationEngine.getUserPreference()
                .getCategoryPreference(place.getCategory());
        score += personalizationScore * PERSONALIZATION_WEIGHT;

        return score;
    }

    /**
     * Calculate name matching score using fuzzy matching
     */
    private double calculateNameMatchScore(String placeName, String query) {
        if (placeName == null || query == null) return 0.0;
        
        String normalizedName = placeName.toLowerCase();
        String normalizedQuery = query.toLowerCase();

        // Exact match gets highest score
        if (normalizedName.equals(normalizedQuery)) {
            return 1.0;
        }

        // Contains match
        if (normalizedName.contains(normalizedQuery)) {
            return 0.8;
        }

        // Word-by-word matching
        String[] queryWords = normalizedQuery.split("\\s+");
        String[] nameWords = normalizedName.split("\\s+");
        
        int matchingWords = 0;
        for (String queryWord : queryWords) {
            for (String nameWord : nameWords) {
                if (nameWord.contains(queryWord) || queryWord.contains(nameWord)) {
                    matchingWords++;
                    break;
                }
            }
        }

        if (queryWords.length > 0) {
            return (double) matchingWords / queryWords.length * 0.6;
        }

        // Fuzzy matching (simple Levenshtein-based)
        double similarity = calculateStringSimilarity(normalizedName, normalizedQuery);
        return similarity > 0.7 ? similarity * 0.4 : 0.0;
    }

    /**
     * Calculate category matching score
     */
    private double calculateCategoryMatchScore(String category, String query) {
        if (category == null || query == null) return 0.0;

        String normalizedCategory = category.toLowerCase();
        String normalizedQuery = query.toLowerCase();

        // Direct category match
        if (normalizedCategory.equals(normalizedQuery)) {
            return 1.0;
        }

        // Category contains query
        if (normalizedCategory.contains(normalizedQuery)) {
            return 0.8;
        }

        // Semantic matching for common terms
        Map<String, List<String>> categoryKeywords = getCategoryKeywords();
        
        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            if (entry.getKey().equals(normalizedCategory)) {
                for (String keyword : entry.getValue()) {
                    if (normalizedQuery.contains(keyword) || keyword.contains(normalizedQuery)) {
                        return 0.6;
                    }
                }
            }
        }

        return 0.0;
    }

    /**
     * Get smart search suggestions based on ML analysis
     */
    public List<String> getSmartSearchSuggestions(String partialQuery, List<SearchHistory> searchHistory) {
        List<String> suggestions = new ArrayList<>();

        // Get ML-based suggestions from recommendation engine
        List<String> mlSuggestions = recommendationEngine.getSmartSearchSuggestions(partialQuery);
        suggestions.addAll(mlSuggestions);

        // Add popular searches from history
        if (searchHistory != null && !searchHistory.isEmpty()) {
            Map<String, Long> queryFrequency = searchHistory.stream()
                    .collect(Collectors.groupingBy(
                            SearchHistory::getSearchQuery,
                            Collectors.counting()
                    ));

            List<String> popularQueries = queryFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .filter(query -> query.toLowerCase().contains(partialQuery.toLowerCase()))
                    .collect(Collectors.toList());

            suggestions.addAll(popularQueries);
        }

        // Add contextual suggestions
        suggestions.addAll(getContextualSuggestions(partialQuery));

        return suggestions.stream()
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
    }

    /**
     * Get contextual search suggestions
     */
    private List<String> getContextualSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("near") || lowerQuery.contains("nearby")) {
            suggestions.addAll(Arrays.asList(
                    "Restaurants near me",
                    "Hotels near me",
                    "Cafes nearby",
                    "Parks near me"
            ));
        } else if (lowerQuery.contains("best") || lowerQuery.contains("top")) {
            suggestions.addAll(Arrays.asList(
                    "Best restaurants",
                    "Top hotels",
                    "Best cafes",
                    "Top attractions"
            ));
        } else if (lowerQuery.contains("cheap") || lowerQuery.contains("budget")) {
            suggestions.addAll(Arrays.asList(
                    "Budget hotels",
                    "Cheap restaurants",
                    "Affordable cafes",
                    "Budget hostels"
            ));
        }

        return suggestions;
    }

    /**
     * Auto-complete search query based on ML predictions
     */
    public List<String> getAutoCompleteSuggestions(String partialQuery, List<Place> places) {
        if (partialQuery == null || partialQuery.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> suggestions = new HashSet<>();
        String query = partialQuery.toLowerCase();

        // Extract place names that match
        for (Place place : places) {
            String placeName = place.getName().toLowerCase();
            if (placeName.startsWith(query)) {
                suggestions.add(place.getName());
            }
        }

        // Add category-based completions
        String[] categories = {"restaurants", "cafes", "hotels", "hostels", "malls", "parks", "gas_stations", "parking"};
        for (String category : categories) {
            if (category.startsWith(query)) {
                suggestions.add(capitalizeFirst(category.replace("_", " ")));
            }
        }

        return suggestions.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    // Helper methods
    private Map<String, List<String>> getCategoryKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();
        
        keywords.put("restaurants", Arrays.asList("food", "eat", "dining", "meal", "cuisine"));
        keywords.put("cafes", Arrays.asList("coffee", "tea", "drink", "beverage", "cafe"));
        keywords.put("hotels", Arrays.asList("stay", "accommodation", "lodge", "inn", "resort"));
        keywords.put("hostels", Arrays.asList("budget", "backpacker", "dorm", "cheap stay"));
        keywords.put("malls", Arrays.asList("shopping", "store", "retail", "shop", "market"));
        keywords.put("parks", Arrays.asList("nature", "garden", "outdoor", "recreation", "green"));
        keywords.put("gas_stations", Arrays.asList("fuel", "petrol", "gas", "station"));
        keywords.put("parking", Arrays.asList("park", "lot", "garage", "space"));

        return keywords;
    }

    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int editDistance = calculateEditDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }

    private int calculateEditDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // Inner class for search results
    private static class SearchResult {
        Place place;
        double relevanceScore;

        SearchResult(Place place, double relevanceScore) {
            this.place = place;
            this.relevanceScore = relevanceScore;
        }
    }
}
