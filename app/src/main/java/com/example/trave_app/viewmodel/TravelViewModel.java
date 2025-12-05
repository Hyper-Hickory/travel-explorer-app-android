package com.example.trave_app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.repository.TravelRepository;

import java.util.List;

public class TravelViewModel extends AndroidViewModel {
    private TravelRepository repository;
    private LiveData<List<Place>> allPlaces;
    private LiveData<List<SearchHistory>> allSearchHistory;
    private LiveData<List<Favorite>> allFavorites;

    public TravelViewModel(@NonNull Application application) {
        super(application);
        repository = new TravelRepository(application);
        allPlaces = repository.getAllPlaces();
        allSearchHistory = repository.getAllSearchHistory();
        allFavorites = repository.getAllFavorites();
    }

    // Place operations
    public LiveData<List<Place>> getAllPlaces() {
        return allPlaces;
    }

    public LiveData<List<Place>> getPlacesByCategory(String category) {
        return repository.getPlacesByCategory(category);
    }

    public LiveData<List<Place>> getFavoritePlaces() {
        return repository.getFavoritePlaces();
    }

    public void insert(Place place) {
        repository.insert(place);
    }

    public void insertAllPlaces(List<Place> places) {
        repository.insertAllPlaces(places);
    }

    public void updatePlace(Place place) {
        repository.updatePlace(place);
    }

    public void deletePlace(Place place) {
        repository.deletePlace(place);
    }

    public void updateFavoriteStatus(String placeId, boolean isFavorite) {
        repository.updateFavoriteStatus(placeId, isFavorite);
    }

    public LiveData<List<Place>> searchPlacesByName(String searchQuery) {
        return repository.searchPlacesByName(searchQuery);
    }

    // Search History operations
    public LiveData<List<SearchHistory>> getAllSearchHistory() {
        return allSearchHistory;
    }

    public LiveData<List<SearchHistory>> getSearchHistoryByCategory(String category) {
        return repository.getSearchHistoryByCategory(category);
    }

    public LiveData<List<SearchHistory>> getRecentSearchHistory(int limit) {
        return repository.getRecentSearchHistory(limit);
    }

    public void insertSearchHistory(SearchHistory searchHistory) {
        repository.insertSearchHistory(searchHistory);
    }

    public void deleteSearchHistory(SearchHistory searchHistory) {
        repository.deleteSearchHistory(searchHistory);
    }

    public void deleteOldSearchHistory(long timestamp) {
        repository.deleteOldSearchHistory(timestamp);
    }

    // Favorite operations
    public LiveData<List<Favorite>> getAllFavorites() {
        return allFavorites;
    }

    public LiveData<List<Favorite>> getFavoritesByCategory(String category) {
        return repository.getFavoritesByCategory(category);
    }

    public void insertFavorite(Favorite favorite) {
        repository.insertFavorite(favorite);
    }

    public void updateFavorite(Favorite favorite) {
        repository.updateFavorite(favorite);
    }

    public void deleteFavorite(Favorite favorite) {
        repository.deleteFavorite(favorite);
    }

    public void deleteFavoriteByPlaceId(String placeId) {
        repository.deleteFavoriteByPlaceId(placeId);
    }

    public LiveData<List<Favorite>> searchFavoritesByName(String searchQuery) {
        return repository.searchFavoritesByName(searchQuery);
    }

    // Utility methods
    public void clearAllData() {
        repository.clearAllData();
    }
}
