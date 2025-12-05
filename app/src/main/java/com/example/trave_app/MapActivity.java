package com.example.trave_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

import com.example.trave_app.data.VashiPlacesProvider;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String searchType;
    private static final double SEARCH_RADIUS_KM = 2.0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private TextView mapTitle;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        mapTitle = findViewById(R.id.mapTitle);
        backButton = findViewById(R.id.backButton);

        // Set back button click listener
        backButton.setOnClickListener(v -> finish());

        // Get search type from intent
        searchType = getIntent().getStringExtra("search_type");
        if (searchType != null) {
            updateTitle(searchType);
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_GOOGLE_MAPS_API_KEY_HERE");
        }
        placesClient = Places.createClient(this);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void updateTitle(String searchType) {
        String title = "";
        switch (searchType) {
            case "restaurants":
                title = "Restaurants";
                break;
            case "cafes":
                title = "Cafes";
                break;
            case "hotels":
                title = "Hotels";
                break;
            case "hostels":
                title = "Hostels";
                break;
            case "malls":
                title = "Malls";
                break;
            case "parks":
                title = "Parks";
                break;
            case "gas_stations":
                title = "Gas Stations";
                break;
            case "parking":
                title = "Parking";
                break;
            default:
                title = "Places";
        }
        mapTitle.setText(title);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        mMap.setMyLocationEnabled(true);
        
        // Get current location and search for places
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14));
                            searchNearbyPlaces();
                        } else {
                            // Default to Vashi, Navi Mumbai if current location is not available
                            LatLng vashi = new LatLng(19.0771, 73.0007);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vashi, 14));
                            addVashiMarkers();
                        }
                    }
                });
    }

    private void searchNearbyPlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

        placesClient.findCurrentPlace(request).addOnSuccessListener((response) -> {
            for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                Place place = placeLikelihood.getPlace();
                if (place.getLatLng() != null && isPlaceTypeMatch(place.getPlaceTypes())) {
                    addMarkerForPlace(place);
                }
            }
        }).addOnFailureListener((exception) -> {
            Toast.makeText(this, "Places search failed. Showing Vashi locations.", Toast.LENGTH_SHORT).show();
            addVashiMarkers();
        });
    }

    private boolean isPlaceTypeMatch(List<String> placeTypes) {
        if (placeTypes == null || searchType == null) return false;

        switch (searchType) {
            case "restaurants":
                return placeTypes.contains("restaurant") || 
                       placeTypes.contains("food") ||
                       placeTypes.contains("meal_takeaway");
            case "cafes":
                return placeTypes.contains("cafe") ||
                       placeTypes.contains("bakery");
            case "hotels":
                return placeTypes.contains("lodging");
            case "hostels":
                return placeTypes.contains("lodging");
            case "malls":
                return placeTypes.contains("shopping_mall");
            case "parks":
                return placeTypes.contains("park") ||
                       placeTypes.contains("tourist_attraction");
            case "gas_stations":
                return placeTypes.contains("gas_station");
            case "parking":
                return placeTypes.contains("parking");
            default:
                return false;
        }
    }

    private void addMarkerForPlace(Place place) {
        if (place.getLatLng() != null) {
            float markerColor = getMarkerColor(searchType);
            mMap.addMarker(new MarkerOptions()
                    .position(place.getLatLng())
                    .title(place.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        }
    }

    private void addVashiMarkers() {
        float markerColor = getMarkerColor(searchType);
        List<com.example.trave_app.database.entity.Place> places;
        if (searchType != null) {
            places = VashiPlacesProvider.getPlacesByCategory(this, searchType);
        } else {
            places = VashiPlacesProvider.getAllPlaces(this);
        }
        for (com.example.trave_app.database.entity.Place p : places) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLatitude(), p.getLongitude()))
                    .title(p.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        }
        if (!places.isEmpty()) {
            LatLng focus = new LatLng(places.get(0).getLatitude(), places.get(0).getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 14));
        }
    }

    private float getMarkerColor(String searchType) {
        switch (searchType) {
            case "restaurants":
                return BitmapDescriptorFactory.HUE_RED;
            case "cafes":
                return BitmapDescriptorFactory.HUE_ORANGE;
            case "hotels":
                return BitmapDescriptorFactory.HUE_BLUE;
            case "hostels":
                return BitmapDescriptorFactory.HUE_CYAN;
            case "malls":
                return BitmapDescriptorFactory.HUE_MAGENTA;
            case "parks":
                return BitmapDescriptorFactory.HUE_GREEN;
            case "gas_stations":
                return BitmapDescriptorFactory.HUE_YELLOW;
            case "parking":
                return BitmapDescriptorFactory.HUE_VIOLET;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }

    private String getSamplePlaceName(String searchType, int index) {
        switch (searchType) {
            case "restaurants":
                return "Restaurant " + index;
            case "cafes":
                return "Cafe " + index;
            case "hotels":
                return "Hotel " + index;
            case "hostels":
                return "Hostel " + index;
            case "malls":
                return "Mall " + index;
            case "parks":
                return "Park " + index;
            case "gas_stations":
                return "Gas Station " + index;
            case "parking":
                return "Parking " + index;
            default:
                return "Place " + index;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Showing Vashi, Navi Mumbai.", Toast.LENGTH_SHORT).show();
                LatLng vashi = new LatLng(19.0771, 73.0007);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vashi, 14));
                addVashiMarkers();
            }
        }
    }
}
