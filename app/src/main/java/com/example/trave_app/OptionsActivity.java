package com.example.trave_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.example.trave_app.notifications.ui.NotificationSettingsActivity;

public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_options);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize card views
        MaterialCardView cardRestaurants = findViewById(R.id.cardRestaurants);
        MaterialCardView cardCafes = findViewById(R.id.cardCafes);
        MaterialCardView cardHotels = findViewById(R.id.cardHotels);
        MaterialCardView cardHostels = findViewById(R.id.cardHostels);
        MaterialCardView cardMalls = findViewById(R.id.cardMalls);
        MaterialCardView cardParks = findViewById(R.id.cardParks);
        MaterialCardView cardGasStations = findViewById(R.id.cardGasStations);
        MaterialCardView cardParking = findViewById(R.id.cardParking);
        MaterialCardView cardChatbot = findViewById(R.id.cardChatbot);
        MaterialCardView cardMLInsights = findViewById(R.id.cardMLInsights);
        MaterialCardView cardNotifications = findViewById(R.id.cardNotifications);
        MaterialCardView cardPicnic = findViewById(R.id.cardPicnic);

        // Set click listeners for cards
        cardRestaurants.setOnClickListener(v -> {
            navigateToMap("restaurants");
        });

        cardCafes.setOnClickListener(v -> {
            navigateToMap("cafes");
        });

        cardHotels.setOnClickListener(v -> {
            navigateToMap("hotels");
        });

        cardHostels.setOnClickListener(v -> {
            navigateToMap("hostels");
        });

        cardMalls.setOnClickListener(v -> {
            navigateToMap("malls");
        });

        cardParks.setOnClickListener(v -> {
            navigateToMap("parks");
        });

        cardGasStations.setOnClickListener(v -> {
            navigateToMap("gas_stations");
        });

        cardParking.setOnClickListener(v -> {
            navigateToMap("parking");
        });

        cardChatbot.setOnClickListener(v -> {
            navigateToChatbot();
        });

        cardMLInsights.setOnClickListener(v -> {
            navigateToMLInsights();
        });

        cardNotifications.setOnClickListener(v -> {
            navigateToNotificationSettings();
        });

        cardPicnic.setOnClickListener(v -> {
            navigateToPicnic();
        });
    }

    private void navigateToMap(String searchType) {
        Toast.makeText(this, "Searching for " + searchType.replace("_", " ") + " within 2km...", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(OptionsActivity.this, MapActivity.class);
        intent.putExtra("search_type", searchType);
        startActivity(intent);
    }

    private void navigateToChatbot() {
        Intent intent = new Intent(OptionsActivity.this, LocalAssistantActivity.class);
        startActivity(intent);
    }

    private void navigateToMLInsights() {
        Intent intent = new Intent(OptionsActivity.this, MLInsightsActivity.class);
        startActivity(intent);
    }

    private void navigateToNotificationSettings() {
        Intent intent = new Intent(OptionsActivity.this, NotificationSettingsActivity.class);
        startActivity(intent);
    }

    private void navigateToPicnic() {
        Intent intent = new Intent(OptionsActivity.this, PicnicActivity.class);
        startActivity(intent);
    }
}
