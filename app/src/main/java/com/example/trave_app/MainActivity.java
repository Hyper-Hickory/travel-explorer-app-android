package com.example.trave_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.notifications.service.RealTimeNotificationManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize notification channels
        RealTimeNotificationManager.createNotificationChannels(this);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize buttons
        MaterialButton buttonSearchNearby = findViewById(R.id.buttonSearchNearby);
        MaterialButton buttonRestaurants = findViewById(R.id.buttonRestaurants);
        MaterialButton buttonHotels = findViewById(R.id.buttonHotels);
        MaterialButton buttonCafes = findViewById(R.id.buttonCafes);
        MaterialButton buttonMalls = findViewById(R.id.buttonMalls);

        // Set click listeners
        buttonSearchNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to options activity for search
                startActivity(new Intent(MainActivity.this, OptionsActivity.class));
            }
        });

        buttonRestaurants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Searching for Restaurants nearby...", Toast.LENGTH_SHORT).show();
                // TODO: Implement restaurant search with Google Maps API
            }
        });

        buttonHotels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Searching for Hotels nearby...", Toast.LENGTH_SHORT).show();
                // TODO: Implement hotel search with Google Maps API
            }
        });

        buttonCafes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Searching for Cafes nearby...", Toast.LENGTH_SHORT).show();
                // TODO: Implement cafe search with Google Maps API
            }
        });

        buttonMalls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Searching for Malls nearby...", Toast.LENGTH_SHORT).show();
                // TODO: Implement mall search with Google Maps API
            }
        });

        // Travel Assistant button is available from Options screen only.

        // Add Database Demo button
        MaterialButton buttonDatabaseDemo = findViewById(R.id.buttonDatabaseDemo);
        buttonDatabaseDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DatabaseDemoActivity.class));
            }
        });

        // No floating chatbot button
    }
}
