package com.example.trave_app.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "search_history")
public class SearchHistory {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "search_query")
    private String searchQuery;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "results_count")
    private int resultsCount;

    @ColumnInfo(name = "search_timestamp")
    private long searchTimestamp;

    // Constructor
    public SearchHistory(String searchQuery, String category, double latitude, 
                        double longitude, int resultsCount, long searchTimestamp) {
        this.searchQuery = searchQuery;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.resultsCount = resultsCount;
        this.searchTimestamp = searchTimestamp;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
