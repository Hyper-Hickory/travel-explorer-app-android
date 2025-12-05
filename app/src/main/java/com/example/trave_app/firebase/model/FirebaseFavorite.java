package com.example.trave_app.firebase.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirebaseFavorite {
    @DocumentId
    private String documentId;
    
    private String placeId;
    private String name;
    private String category;
    private double latitude;
    private double longitude;
    private String address;
    private float rating;
    private String notes;
    private long addedAt;
    
    @ServerTimestamp
    private Date lastModified;

    // Default constructor required for Firestore
    public FirebaseFavorite() {}

    // Constructor
    public FirebaseFavorite(String placeId, String name, String category, double latitude, 
                           double longitude, String address, float rating, String notes, long addedAt) {
        this.placeId = placeId;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.rating = rating;
        this.notes = notes;
        this.addedAt = addedAt;
    }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("placeId", placeId);
        map.put("name", name);
        map.put("category", category);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("address", address);
        map.put("rating", rating);
        map.put("notes", notes);
        map.put("addedAt", addedAt);
        return map;
    }

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
