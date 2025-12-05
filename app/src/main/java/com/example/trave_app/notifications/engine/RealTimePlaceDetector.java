package com.example.trave_app.notifications.engine;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.trave_app.R;
import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.MapActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RealTimePlaceDetector {
    private static final String TAG = "RealTimePlaceDetector";
    private static final String CHANNEL_ID = "realtime_places_channel";
    private static final double SEARCH_RADIUS_KM = 1.0; // 1 km radius
    
    private Context context;
    private TravelDatabase database;
    private NotificationManager notificationManager;
    private Random random;
    
    // All place categories to detect
    private final List<String> PLACE_CATEGORIES = Arrays.asList(
        "restaurants", "cafes", "hotels", "hostels", "malls", "parks", "gas_stations", "parking"
    );

    public RealTimePlaceDetector(Context context) {
        this.context = context;
        this.database = TravelDatabase.getInstance(context);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.random = new Random();
    }

    public List<Place> detectNearbyPlaces(double latitude, double longitude) {
        Log.d(TAG, "Detecting places near: " + latitude + ", " + longitude);
        
        List<Place> nearbyPlaces = new ArrayList<>();
        
        try {
            // Get places from database within radius
            List<Place> allPlaces = database.placeDao().getAllPlacesSync();
            
            for (Place place : allPlaces) {
                double distance = calculateDistance(latitude, longitude, 
                    place.getLatitude(), place.getLongitude());
                
                if (distance <= SEARCH_RADIUS_KM) {
                    nearbyPlaces.add(place);
                }
            }
            
            // If no places in database, use curated Vashi places for demonstration
            if (nearbyPlaces.isEmpty()) {
                nearbyPlaces = getCuratedVashiPlaces();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting nearby places", e);
            // Use curated Vashi places as fallback
            nearbyPlaces = getCuratedVashiPlaces();
        }
        
        return nearbyPlaces;
    }

    public void generateRealTimeNotifications(List<Place> nearbyPlaces, Location userLocation) {
        Log.d(TAG, "Generating real-time notifications for " + nearbyPlaces.size() + " places");
        
        // Group places by category and send notifications
        for (String category : PLACE_CATEGORIES) {
            List<Place> categoryPlaces = new ArrayList<>();
            for (Place place : nearbyPlaces) {
                if (place.getCategory().equals(category)) {
                    categoryPlaces.add(place);
                }
            }
            
            if (!categoryPlaces.isEmpty()) {
                sendCategoryNotification(category, categoryPlaces, userLocation);
            }
        }
    }

    private void sendCategoryNotification(String category, List<Place> places, Location userLocation) {
        if (places.isEmpty()) return;
        
        // Get the best rated place in this category
        Place bestPlace = places.get(0);
        for (Place place : places) {
            if (place.getRating() > bestPlace.getRating()) {
                bestPlace = place;
            }
        }
        
        // Calculate distance to best place
        double distance = calculateDistance(
            userLocation.getLatitude(), userLocation.getLongitude(),
            bestPlace.getLatitude(), bestPlace.getLongitude()
        );
        
        // Create notification content
        String title = getNotificationTitle(category, places.size());
        String message = createNotificationMessage(bestPlace, distance, places.size());
        
        // Create intent to open map at this location
        Intent mapIntent = new Intent(context, MapActivity.class);
        mapIntent.putExtra("category", category);
        mapIntent.putExtra("latitude", bestPlace.getLatitude());
        mapIntent.putExtra("longitude", bestPlace.getLongitude());
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            category.hashCode(), 
            mapIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build and send notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_notifications, "View on Map", pendingIntent);
        
        // Send notification with unique ID for each category
        int notificationId = 2000 + category.hashCode();
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, "Sent notification for " + category + ": " + title);
    }

    private String getNotificationTitle(String category, int count) {
        String emoji = getCategoryEmoji(category);
        String categoryName = getCategoryDisplayName(category);
        
        if (count == 1) {
            return emoji + " " + categoryName + " Nearby!";
        } else {
            return emoji + " " + count + " " + categoryName + " Nearby!";
        }
    }

    private String createNotificationMessage(Place bestPlace, double distance, int totalCount) {
        StringBuilder message = new StringBuilder();
        
        // Best place info
        message.append("⭐ ").append(bestPlace.getName());
        message.append(" (").append(String.format("%.1f", bestPlace.getRating())).append("★)");
        message.append(" - ").append(String.format("%.0f", distance * 1000)).append("m away");
        
        // Location info
        if (bestPlace.getAddress() != null && !bestPlace.getAddress().isEmpty()) {
            message.append("\n📍 ").append(bestPlace.getAddress());
        }
        
        // Additional places info
        if (totalCount > 1) {
            message.append("\n\n+ ").append(totalCount - 1).append(" more nearby");
        }
        
        return message.toString();
    }

    private String getCategoryEmoji(String category) {
        switch (category.toLowerCase()) {
            case "restaurants": return "🍽️";
            case "cafes": return "☕";
            case "hotels": return "🏨";
            case "hostels": return "🏠";
            case "malls": return "🛍️";
            case "parks": return "🌳";
            case "gas_stations": return "⛽";
            case "parking": return "🅿️";
            default: return "📍";
        }
    }

    private String getCategoryDisplayName(String category) {
        switch (category.toLowerCase()) {
            case "restaurants": return "Restaurants";
            case "cafes": return "Cafes";
            case "hotels": return "Hotels";
            case "hostels": return "Hostels";
            case "malls": return "Shopping Malls";
            case "parks": return "Parks";
            case "gas_stations": return "Gas Stations";
            case "parking": return "Parking";
            default: return "Places";
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }

    private List<Place> getCuratedVashiPlaces() {
        try {
            List<com.example.trave_app.database.entity.Place> all = com.example.trave_app.data.VashiPlacesProvider.getAllPlaces(context);
            return new ArrayList<>(all);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
