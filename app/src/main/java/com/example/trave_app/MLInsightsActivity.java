package com.example.trave_app;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.ml.engine.TravelRecommendationEngine;
import com.example.trave_app.viewmodel.TravelViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MLInsightsActivity extends AppCompatActivity {
    
    private TextView tvTopPreference;
    private TextView tvMostVisited;
    private TextView tvDiversityScore;
    private TextView tvTotalVisits;
    private TextView tvPredictedNext;
    private RecyclerView rvRecommendations;
    private ImageButton btnBack;
    
    private TravelViewModel travelViewModel;
    private TravelRecommendationEngine mlEngine;
    private ExecutorService executorService;
    
    // Cache for travel data
    private List<Place> places = new ArrayList<>();
    private List<Favorite> favorites = new ArrayList<>();
    private List<SearchHistory> searchHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ml_insights);
        
        initializeViews();
        initializeServices();
        setupClickListeners();
        loadTravelData();
    }

    private void initializeViews() {
        tvTopPreference = findViewById(R.id.tvTopPreference);
        tvMostVisited = findViewById(R.id.tvMostVisited);
        tvDiversityScore = findViewById(R.id.tvDiversityScore);
        tvTotalVisits = findViewById(R.id.tvTotalVisits);
        tvPredictedNext = findViewById(R.id.tvPredictedNext);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        btnBack = findViewById(R.id.btnBack);
        
        // Hide recommendations RecyclerView for now since we don't have an adapter
        rvRecommendations.setVisibility(android.view.View.GONE);
    }

    private void initializeServices() {
        travelViewModel = new ViewModelProvider(this).get(TravelViewModel.class);
        mlEngine = TravelRecommendationEngine.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadTravelData() {
        // Load places
        travelViewModel.getAllPlaces().observe(this, placesList -> {
            if (placesList != null) {
                places.clear();
                places.addAll(placesList);
                updateMLInsights();
            }
        });
        
        // Load favorites
        travelViewModel.getAllFavorites().observe(this, favoritesList -> {
            if (favoritesList != null) {
                favorites.clear();
                favorites.addAll(favoritesList);
                updateMLInsights();
            }
        });
        
        // Load search history
        travelViewModel.getAllSearchHistory().observe(this, searchHistoryList -> {
            if (searchHistoryList != null) {
                searchHistory.clear();
                searchHistory.addAll(searchHistoryList);
                updateMLInsights();
            }
        });
    }

    private void updateMLInsights() {
        if (places.isEmpty() && favorites.isEmpty() && searchHistory.isEmpty()) {
            // Show default values when no data is available
            showDefaultInsights();
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Learn from user behavior
                mlEngine.learnFromUserBehavior(places, favorites, searchHistory);
                
                // Get travel pattern insights
                Map<String, Object> insights = mlEngine.analyzeTravelPatterns();
                
                // Get next destination prediction
                String nextDestination = mlEngine.predictNextDestinationCategory();
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    updateInsightsUI(insights, nextDestination);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showDefaultInsights();
                });
            }
        });
    }

    private void updateInsightsUI(Map<String, Object> insights, String nextDestination) {
        // Update travel pattern analysis
        tvTopPreference.setText(insights.get("topPreference").toString());
        tvMostVisited.setText(insights.get("mostVisited").toString());
        tvDiversityScore.setText(String.format("%.1f%%", (Double) insights.get("diversityScore")));
        tvTotalVisits.setText(insights.get("totalVisits").toString());
        
        // Update prediction
        tvPredictedNext.setText(nextDestination != null ? nextDestination : "Restaurants");
    }

    private void showDefaultInsights() {
        tvTopPreference.setText("No data yet");
        tvMostVisited.setText("No data yet");
        tvDiversityScore.setText("0.0%");
        tvTotalVisits.setText("0");
        tvPredictedNext.setText("Start exploring to get predictions!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
