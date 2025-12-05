package com.example.trave_app.ml.model;

import java.util.Map;
import java.util.HashMap;

public class TravelPreference {
    private String userId;
    private Map<String, Double> categoryPreferences;
    private Map<String, Double> locationPreferences;
    private Map<String, Integer> visitFrequency;
    private double averageRating;
    private String preferredTimeOfDay;
    private double budgetRange;
    private long lastUpdated;

    public TravelPreference() {
        this.categoryPreferences = new HashMap<>();
        this.locationPreferences = new HashMap<>();
        this.visitFrequency = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    public TravelPreference(String userId) {
        this();
        this.userId = userId;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<String, Double> getCategoryPreferences() { return categoryPreferences; }
    public void setCategoryPreferences(Map<String, Double> categoryPreferences) {
        this.categoryPreferences = categoryPreferences;
    }

    public Map<String, Double> getLocationPreferences() { return locationPreferences; }
    public void setLocationPreferences(Map<String, Double> locationPreferences) {
        this.locationPreferences = locationPreferences;
    }

    public Map<String, Integer> getVisitFrequency() { return visitFrequency; }
    public void setVisitFrequency(Map<String, Integer> visitFrequency) {
        this.visitFrequency = visitFrequency;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public String getPreferredTimeOfDay() { return preferredTimeOfDay; }
    public void setPreferredTimeOfDay(String preferredTimeOfDay) {
        this.preferredTimeOfDay = preferredTimeOfDay;
    }

    public double getBudgetRange() { return budgetRange; }
    public void setBudgetRange(double budgetRange) { this.budgetRange = budgetRange; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    // Utility methods
    public void updateCategoryPreference(String category, double weight) {
        categoryPreferences.put(category, 
            categoryPreferences.getOrDefault(category, 0.0) + weight);
        this.lastUpdated = System.currentTimeMillis();
    }

    public void incrementVisitFrequency(String category) {
        visitFrequency.put(category, 
            visitFrequency.getOrDefault(category, 0) + 1);
        this.lastUpdated = System.currentTimeMillis();
    }

    public double getCategoryPreference(String category) {
        return categoryPreferences.getOrDefault(category, 0.0);
    }

    public void normalizePreferences() {
        // Normalize category preferences to sum to 1.0
        double sum = categoryPreferences.values().stream()
            .mapToDouble(Double::doubleValue).sum();
        
        if (sum > 0) {
            categoryPreferences.replaceAll((k, v) -> v / sum);
        }
        this.lastUpdated = System.currentTimeMillis();
    }
}
