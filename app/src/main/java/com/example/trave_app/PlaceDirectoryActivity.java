package com.example.trave_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PlaceDirectoryActivity extends AppCompatActivity implements PlaceAdapter.OnPlaceClickListener {

    private RecyclerView recyclerView;
    private Spinner spinnerState, spinnerCategory;
    private PlaceAdapter adapter;
    private List<PlaceItem> allPlaces = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_place_directory);

        recyclerView = findViewById(R.id.recyclerPlaces);
        spinnerState = findViewById(R.id.spinnerState);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        loadData();
        setupFilters();
        applyFilters();
    }

    private void loadData() {
        try {
            InputStream is = getResources().openRawResource(R.raw.places_india);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            Type listType = new TypeToken<List<PlaceItem>>(){}.getType();
            allPlaces = new Gson().fromJson(reader, listType);
            if (allPlaces == null) allPlaces = new ArrayList<>();
        } catch (Exception e) {
            allPlaces = new ArrayList<>();
        }
    }

    private void setupFilters() {
        // States
        Set<String> states = new HashSet<>();
        for (PlaceItem p : allPlaces) states.add(p.state);
        List<String> stateList = new ArrayList<>(states);
        Collections.sort(stateList);
        stateList.add(0, "All States");
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stateList);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerState.setAdapter(stateAdapter);

        // Categories
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        categories.add("beach");
        categories.add("hotel");
        categories.add("restaurant");
        categories.add("park");
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spinnerState.setOnItemSelectedListener(listener);
        spinnerCategory.setOnItemSelectedListener(listener);
    }

    private void applyFilters() {
        String state = (String) spinnerState.getSelectedItem();
        String category = (String) spinnerCategory.getSelectedItem();
        List<PlaceItem> filtered = new ArrayList<>(allPlaces);
        if (state != null && !state.equals("All States")) {
            filtered = filtered.stream().filter(p -> p.state.equalsIgnoreCase(state)).collect(Collectors.toList());
        }
        if (category != null && !category.equals("All Categories")) {
            filtered = filtered.stream().filter(p -> p.category.equalsIgnoreCase(category)).collect(Collectors.toList());
        }
        adapter.updateData(filtered);
    }

    @Override
    public void onPlaceClicked(PlaceItem item) {
        String label = Uri.encode(item.name);
        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", item.latitude, item.longitude, item.latitude, item.longitude, label);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    public static class PlaceItem {
        public String name;
        public String category; // beach, hotel, restaurant, park
        public String state; // e.g., Maharashtra
        public double latitude;
        public double longitude;
        public String address;
    }
}
