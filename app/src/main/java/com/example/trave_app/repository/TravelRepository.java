package com.example.trave_app.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.database.dao.FavoriteDao;
import com.example.trave_app.database.dao.PlaceDao;
import com.example.trave_app.database.dao.SearchHistoryDao;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.firebase.sync.DataSyncService;

import java.util.List;

public class TravelRepository {
    private PlaceDao placeDao;
    private SearchHistoryDao searchHistoryDao;
    private FavoriteDao favoriteDao;
    private LiveData<List<Place>> allPlaces;
    private LiveData<List<SearchHistory>> allSearchHistory;
    private LiveData<List<Favorite>> allFavorites;
    private DataSyncService dataSyncService;

    public TravelRepository(Application application) {
        TravelDatabase db = TravelDatabase.getDatabase(application);
        placeDao = db.placeDao();
        searchHistoryDao = db.searchHistoryDao();
        favoriteDao = db.favoriteDao();
        allPlaces = placeDao.getAllPlaces();
        allSearchHistory = searchHistoryDao.getAllSearchHistory();
        allFavorites = favoriteDao.getAllFavorites();
        dataSyncService = DataSyncService.getInstance(application);
    }

    // Place operations
    public LiveData<List<Place>> getAllPlaces() {
        return allPlaces;
    }

    public LiveData<List<Place>> getPlacesByCategory(String category) {
        return placeDao.getPlacesByCategory(category);
    }

    public LiveData<List<Place>> getFavoritePlaces() {
        return placeDao.getFavoritePlaces();
    }

    public void insert(Place place) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.insert(place);
            // Sync to cloud in background (non-blocking)
            dataSyncService.syncSinglePlace(place).exceptionally(throwable -> {
                // Log error but don't fail the local operation
                return null;
            });
        });
    }

    public void insertAllPlaces(List<Place> places) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.insertAll(places);
            // Sync all places to cloud in background
            for (Place place : places) {
                dataSyncService.syncSinglePlace(place).exceptionally(throwable -> {
                    return null;
                });
            }
        });
    }

    public void updatePlace(Place place) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.update(place);
            // Sync updated place to cloud
            dataSyncService.syncSinglePlace(place).exceptionally(throwable -> {
                return null;
            });
        });
    }

    public void deletePlace(Place place) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.delete(place);
        });
    }

    public void updateFavoriteStatus(String placeId, boolean isFavorite) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.updateFavoriteStatus(placeId, isFavorite);
        });
    }

    public LiveData<List<Place>> searchPlacesByName(String searchQuery) {
        return placeDao.searchPlacesByName(searchQuery);
    }

    // Search History operations
    public LiveData<List<SearchHistory>> getAllSearchHistory() {
        return allSearchHistory;
    }

    public LiveData<List<SearchHistory>> getSearchHistoryByCategory(String category) {
        return searchHistoryDao.getSearchHistoryByCategory(category);
    }

    public LiveData<List<SearchHistory>> getRecentSearchHistory(int limit) {
        return searchHistoryDao.getRecentSearchHistory(limit);
    }

    public void insertSearchHistory(SearchHistory searchHistory) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            searchHistoryDao.insert(searchHistory);
            // Sync to cloud in background
            dataSyncService.syncSingleSearchHistory(searchHistory).exceptionally(throwable -> {
                return null;
            });
        });
    }

    public void deleteSearchHistory(SearchHistory searchHistory) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            searchHistoryDao.delete(searchHistory);
        });
    }

    public void deleteOldSearchHistory(long timestamp) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            searchHistoryDao.deleteOldSearchHistory(timestamp);
        });
    }

    // Favorite operations
    public LiveData<List<Favorite>> getAllFavorites() {
        return allFavorites;
    }

    public LiveData<List<Favorite>> getFavoritesByCategory(String category) {
        return favoriteDao.getFavoritesByCategory(category);
    }

    public void insertFavorite(Favorite favorite) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            favoriteDao.insert(favorite);
            // Sync to cloud in background
            dataSyncService.syncSingleFavorite(favorite).exceptionally(throwable -> {
                return null;
            });
        });
    }

    public void updateFavorite(Favorite favorite) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            favoriteDao.update(favorite);
            // Sync updated favorite to cloud
            dataSyncService.syncSingleFavorite(favorite).exceptionally(throwable -> {
                return null;
            });
        });
    }

    public void deleteFavorite(Favorite favorite) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            favoriteDao.delete(favorite);
            // Delete from cloud as well
            dataSyncService.deleteFavoriteFromCloud(favorite.getPlaceId()).exceptionally(throwable -> {
                return null;
            });
        });
    }

    public void deleteFavoriteByPlaceId(String placeId) {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            favoriteDao.deleteByPlaceId(placeId);
            // Delete from cloud as well
            dataSyncService.deleteFavoriteFromCloud(placeId).exceptionally(throwable -> {
                return null;
            });
        });
    }

    public LiveData<List<Favorite>> searchFavoritesByName(String searchQuery) {
        return favoriteDao.searchFavoritesByName(searchQuery);
    }

    // Cloud synchronization methods
    public void performFullSync() {
        dataSyncService.performFullSync().exceptionally(throwable -> {
            return null;
        });
    }

    public boolean isSyncing() {
        return dataSyncService.isSyncing();
    }

    // Utility methods
    public void clearAllData() {
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.deleteAllPlaces();
            searchHistoryDao.deleteAllSearchHistory();
            favoriteDao.deleteAllFavorites();
        });
    }
}
