package com.example.trave_app.notifications.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.trave_app.R;
import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.notifications.engine.RealTimePlaceDetector;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

public class RealTimeLocationService extends Service {
    private static final String TAG = "RealTimeLocationService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds
    private static final long FASTEST_INTERVAL = 15000; // 15 seconds
    private static final float MIN_DISTANCE = 50.0f; // 50 meters

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private RealTimePlaceDetector placeDetector;
    private Location lastKnownLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RealTimeLocationService created");
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        placeDetector = new RealTimePlaceDetector(this);
        
        createNotificationChannel();
        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting real-time location tracking");
        
        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        startLocationUpdates();
        
        return START_STICKY; // Restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RealTimeLocationService destroyed");
        stopLocationUpdates();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracks your location for real-time place notifications");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🗺️ Travel App Location Tracking")
            .setContentText("Finding nearby places for you...")
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    handleLocationUpdate(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL * 2)
            .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Location updates started");
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        }
    }

    private void handleLocationUpdate(Location newLocation) {
        Log.d(TAG, "New location: " + newLocation.getLatitude() + ", " + newLocation.getLongitude());
        
        // Check if we've moved enough to warrant checking for new places
        if (lastKnownLocation != null) {
            float distance = lastKnownLocation.distanceTo(newLocation);
            if (distance < MIN_DISTANCE) {
                Log.d(TAG, "Location change too small, skipping place detection");
                return;
            }
        }
        
        lastKnownLocation = newLocation;
        
        // Detect nearby places in background thread
        new Thread(() -> {
            try {
                List<Place> nearbyPlaces = placeDetector.detectNearbyPlaces(
                    newLocation.getLatitude(), 
                    newLocation.getLongitude()
                );
                
                if (!nearbyPlaces.isEmpty()) {
                    Log.d(TAG, "Found " + nearbyPlaces.size() + " nearby places");
                    placeDetector.generateRealTimeNotifications(nearbyPlaces, newLocation);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error detecting nearby places", e);
            }
        }).start();
    }
}
