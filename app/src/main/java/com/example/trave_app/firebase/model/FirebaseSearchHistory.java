package com.example.trave_app.firebase.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirebaseSearchHistory {
    @DocumentId
    private String documentId;
    
    private String searchQuery;
    private String category;
    private double latitude;
    private double longitude;
    private int resultsCount;
    private long searchTimestamp;
    
    @ServerTimestamp
    private Date lastModified;

    // Default constructor required for Firestore
    public FirebaseSearchHistory() {}

    // Constructor
    public FirebaseSearchHistory(String searchQuery, String category, double latitude, 
                                double longitude, int resultsCount, long searchTimestamp) {
        this.searchQuery = searchQuery;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.resultsCount = resultsCount;
        this.searchTimestamp = searchTimestamp;
    }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("searchQuery", searchQuery);
        map.put("category", category);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("resultsCount", resultsCount);
        map.put("searchTimestamp", searchTimestamp);
        return map;
    }

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getResultsCount() {
        return resultsCount;
    }

    public void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
    }

    public long getSearchTimestamp() {
        return searchTimestamp;
    }

    public void setSearchTimestamp(long searchTimestamp) {
        this.searchTimestamp = searchTimestamp;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
