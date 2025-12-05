package com.example.trave_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.viewmodel.TravelViewModel;

import java.util.ArrayList;
import java.util.List;

public class DatabaseDemoActivity extends AppCompatActivity {
    
    private TravelViewModel viewModel;
    private TextView statusText;
    private RecyclerView recyclerView;
    private Button btnAddSampleData, btnViewPlaces, btnViewHistory, btnViewFavorites, btnClearData;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_demo);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(TravelViewModel.class);
        
        // Initialize UI components
        initializeViews();
        setupClickListeners();
        setupObservers();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        recyclerView = findViewById(R.id.recyclerView);
        btnAddSampleData = findViewById(R.id.btnAddSampleData);
        btnViewPlaces = findViewById(R.id.btnViewPlaces);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewFavorites = findViewById(R.id.btnViewFavorites);
        btnClearData = findViewById(R.id.btnClearData);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        statusText.setText("Database Demo Ready - Click buttons to test functionality");
    }
    
    private void setupClickListeners() {
        btnAddSampleData.setOnClickListener(v -> addSampleData());
        btnViewPlaces.setOnClickListener(v -> viewAllPlaces());
        btnViewHistory.setOnClickListener(v -> viewSearchHistory());
        btnViewFavorites.setOnClickListener(v -> viewFavorites());
        btnClearData.setOnClickListener(v -> clearAllData());
    }
    
    private void setupObservers() {
        // Observe places data
        viewModel.getAllPlaces().observe(this, places -> {
            if (places != null) {
                updateStatus("Places in database: " + places.size());
            }
        });
    }
    
    private void addSampleData() {
        updateStatus("Adding sample data to database...");
        
        // Add sample places
        List<Place> samplePlaces = createSamplePlaces();
        for (Place place : samplePlaces) {
            viewModel.insert(place);
        }
        
        // Add sample search history
        List<SearchHistory> sampleHistory = createSampleSearchHistory();
        for (SearchHistory history : sampleHistory) {
            viewModel.insertSearchHistory(history);
        }
        
        // Add sample favorites
        List<Favorite> sampleFavorites = createSampleFavorites();
        for (Favorite favorite : sampleFavorites) {
            viewModel.insertFavorite(favorite);
        }
        
        Toast.makeText(this, "Sample data added successfully!", Toast.LENGTH_SHORT).show();
        updateStatus("Sample data added: 5 Places, 3 Search History, 2 Favorites");
    }
    
    private void viewAllPlaces() {
        viewModel.getAllPlaces().observe(this, places -> {
            if (places != null && !places.isEmpty()) {
                StringBuilder sb = new StringBuilder("PLACES IN DATABASE:\n\n");
                for (Place place : places) {
                    sb.append("📍 ").append(place.getName())
                      .append("\n   Category: ").append(place.getCategory())
                      .append("\n   Location: ").append(place.getLatitude()).append(", ").append(place.getLongitude())
                      .append("\n   Rating: ").append(place.getRating()).append("⭐")
                      .append("\n   Favorite: ").append(place.isFavorite() ? "❤️ Yes" : "No")
                      .append("\n\n");
                }
                updateStatus(sb.toString());
            } else {
                updateStatus("No places found in database. Add sample data first.");
            }
        });
    }
    
    private void viewSearchHistory() {
        viewModel.getAllSearchHistory().observe(this, histories -> {
            if (histories != null && !histories.isEmpty()) {
                StringBuilder sb = new StringBuilder("SEARCH HISTORY:\n\n");
                for (SearchHistory history : histories) {
                    sb.append("🔍 ").append(history.getSearchQuery())
                      .append("\n   Category: ").append(history.getCategory())
                      .append("\n   Results: ").append(history.getResultsCount())
                      .append("\n   Time: ").append(new java.util.Date(history.getSearchTimestamp()))
                      .append("\n\n");
                }
                updateStatus(sb.toString());
            } else {
                updateStatus("No search history found. Add sample data first.");
            }
        });
    }
    
    private void viewFavorites() {
        viewModel.getAllFavorites().observe(this, favorites -> {
            if (favorites != null && !favorites.isEmpty()) {
                StringBuilder sb = new StringBuilder("FAVORITE PLACES:\n\n");
                for (Favorite favorite : favorites) {
                    sb.append("❤️ ").append(favorite.getName())
                      .append("\n   Category: ").append(favorite.getCategory())
                      .append("\n   Rating: ").append(favorite.getRating()).append("⭐")
                      .append("\n   Notes: ").append(favorite.getNotes())
                      .append("\n   Added: ").append(new java.util.Date(favorite.getAddedAt()))
                      .append("\n\n");
                }
                updateStatus(sb.toString());
            } else {
                updateStatus("No favorites found. Add sample data first.");
            }
        });
    }
    
    private void clearAllData() {
        viewModel.clearAllData();
        updateStatus("All database data cleared!");
        Toast.makeText(this, "Database cleared successfully!", Toast.LENGTH_SHORT).show();
    }
    
    private void updateStatus(String message) {
        statusText.setText(message);
    }
    
    private List<Place> createSamplePlaces() {
        // Use curated Vashi dataset
        return com.example.trave_app.data.VashiPlacesProvider.getAllPlaces(this);
    }
    
    private List<SearchHistory> createSampleSearchHistory() {
        List<SearchHistory> histories = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        // Center around Vashi
        histories.add(new SearchHistory("nearby restaurants", "restaurant", 19.0655, 72.9975, 15, currentTime - 3600000));
        histories.add(new SearchHistory("coffee shops", "cafe", 19.0654, 72.9968, 8, currentTime - 1800000));
        histories.add(new SearchHistory("hotels near me", "hotel", 19.0657, 72.9978, 12, currentTime - 900000));
        return histories;
    }
    
    private List<Favorite> createSampleFavorites() {
        List<Favorite> favorites = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        // Use a couple of Vashi favorites
        favorites.add(new Favorite("vashi_cafes_starbucks_coffee", "Starbucks Coffee", "cafes", 19.0654, 72.9968, "Ground Floor, Inorbit Mall, Sector 30A, Vashi, Navi Mumbai", 4.4f, "Great coffee and wifi", currentTime));
        favorites.add(new Favorite("vashi_hotels_four_points_by_sheraton_navi_mumbai_vashi", "Four Points by Sheraton", "hotels", 19.0657, 72.9978, "Sector 30A, Vashi", 4.4f, "Convenient for Inorbit", currentTime));
        return favorites;
    }
}
