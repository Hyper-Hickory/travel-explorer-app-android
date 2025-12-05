package com.example.trave_app.firebase.sync;

import android.content.Context;
import android.util.Log;
import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.firebase.repository.FirebaseRepository;
import com.example.trave_app.firebase.model.FirebasePlace;
import com.example.trave_app.firebase.model.FirebaseFavorite;
import com.example.trave_app.firebase.model.FirebaseSearchHistory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataSyncService {
    private static final String TAG = "DataSyncService";
    private static DataSyncService instance;
    private FirebaseRepository firebaseRepository;
    private TravelDatabase localDatabase;
    private ExecutorService executorService;
    private boolean isSyncing = false;

    private DataSyncService(Context context) {
        firebaseRepository = FirebaseRepository.getInstance();
        localDatabase = TravelDatabase.getInstance(context);
        executorService = Executors.newFixedThreadPool(3);
    }

    public static synchronized DataSyncService getInstance(Context context) {
        if (instance == null) {
            instance = new DataSyncService(context.getApplicationContext());
        }
        return instance;
    }

    public CompletableFuture<Void> performFullSync() {
        if (isSyncing) {
            Log.d(TAG, "Sync already in progress, skipping");
            return CompletableFuture.completedFuture(null);
        }

        isSyncing = true;
        Log.d(TAG, "Starting full data synchronization");

        CompletableFuture<Void> syncFuture = CompletableFuture.allOf(
            syncPlacesToCloud(),
            syncFavoritesToCloud(),
            syncSearchHistoryToCloud(),
            syncPlacesFromCloud(),
            syncFavoritesFromCloud(),
            syncSearchHistoryFromCloud()
        ).thenRun(() -> {
            isSyncing = false;
            Log.d(TAG, "Full synchronization completed");
        }).exceptionally(throwable -> {
            isSyncing = false;
            Log.e(TAG, "Error during full synchronization", throwable);
            return null;
        });

        return syncFuture;
    }

    public CompletableFuture<Void> syncPlacesToCloud() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Place> localPlaces = localDatabase.placeDao().getAllPlacesSync();
                return localPlaces;
            } catch (Exception e) {
                Log.e(TAG, "Error getting local places", e);
                throw new RuntimeException(e);
            }
        }, executorService).thenCompose(places -> {
            if (places.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return firebaseRepository.syncAllPlacesToCloud(places);
        });
    }

    public CompletableFuture<Void> syncFavoritesToCloud() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Favorite> localFavorites = localDatabase.favoriteDao().getAllFavoritesSync();
                return localFavorites;
            } catch (Exception e) {
                Log.e(TAG, "Error getting local favorites", e);
                throw new RuntimeException(e);
            }
        }, executorService).thenCompose(favorites -> {
            if (favorites.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return firebaseRepository.syncAllFavoritesToCloud(favorites);
        });
    }

    public CompletableFuture<Void> syncSearchHistoryToCloud() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SearchHistory> localSearchHistory = localDatabase.searchHistoryDao().getAllSearchHistorySync();
                return localSearchHistory;
            } catch (Exception e) {
                Log.e(TAG, "Error getting local search history", e);
                throw new RuntimeException(e);
            }
        }, executorService).thenCompose(searchHistories -> {
            if (searchHistories.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            
            CompletableFuture<Void> allSyncs = CompletableFuture.completedFuture(null);
            for (SearchHistory history : searchHistories) {
                allSyncs = allSyncs.thenCompose(v -> firebaseRepository.syncSearchHistoryToCloud(history));
            }
            return allSyncs;
        });
    }

    public CompletableFuture<Void> syncPlacesFromCloud() {
        return firebaseRepository.getPlacesFromCloud()
            .thenCompose(firebasePlaces -> {
                return CompletableFuture.runAsync(() -> {
                    try {
                        for (FirebasePlace firebasePlace : firebasePlaces) {
                            // Check if place already exists locally
                            Place existingPlace = localDatabase.placeDao().getPlaceByPlaceIdSync(firebasePlace.getPlaceId());
                            if (existingPlace == null) {
                                // Convert Firebase place to local place and insert
                                Place localPlace = new Place(
                                    firebasePlace.getPlaceId(),
                                    firebasePlace.getName(),
                                    firebasePlace.getCategory(),
                                    firebasePlace.getLatitude(),
                                    firebasePlace.getLongitude(),
                                    firebasePlace.getAddress(),
                                    firebasePlace.getRating(),
                                    firebasePlace.isFavorite(),
                                    firebasePlace.getCreatedAt()
                                );
                                localDatabase.placeDao().insertSync(localPlace);
                                Log.d(TAG, "Synced place from cloud: " + firebasePlace.getName());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing places from cloud", e);
                        throw new RuntimeException(e);
                    }
                }, executorService);
            });
    }

    public CompletableFuture<Void> syncFavoritesFromCloud() {
        return firebaseRepository.getFavoritesFromCloud()
            .thenCompose(firebaseFavorites -> {
                return CompletableFuture.runAsync(() -> {
                    try {
                        for (FirebaseFavorite firebaseFavorite : firebaseFavorites) {
                            // Check if favorite already exists locally
                            Favorite existingFavorite = localDatabase.favoriteDao().getFavoriteByPlaceIdSync(firebaseFavorite.getPlaceId());
                            if (existingFavorite == null) {
                                // Convert Firebase favorite to local favorite and insert
                                Favorite localFavorite = new Favorite(
                                    firebaseFavorite.getPlaceId(),
                                    firebaseFavorite.getName(),
                                    firebaseFavorite.getCategory(),
                                    firebaseFavorite.getLatitude(),
                                    firebaseFavorite.getLongitude(),
                                    firebaseFavorite.getAddress(),
                                    firebaseFavorite.getRating(),
                                    firebaseFavorite.getNotes(),
                                    firebaseFavorite.getAddedAt()
                                );
                                localDatabase.favoriteDao().insertSync(localFavorite);
                                Log.d(TAG, "Synced favorite from cloud: " + firebaseFavorite.getName());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing favorites from cloud", e);
                        throw new RuntimeException(e);
                    }
                }, executorService);
            });
    }

    public CompletableFuture<Void> syncSearchHistoryFromCloud() {
        return firebaseRepository.getSearchHistoryFromCloud()
            .thenCompose(firebaseSearchHistories -> {
                return CompletableFuture.runAsync(() -> {
                    try {
                        for (FirebaseSearchHistory firebaseHistory : firebaseSearchHistories) {
                            // Convert Firebase search history to local search history and insert
                            SearchHistory localHistory = new SearchHistory(
                                firebaseHistory.getSearchQuery(),
                                firebaseHistory.getCategory(),
                                firebaseHistory.getLatitude(),
                                firebaseHistory.getLongitude(),
                                firebaseHistory.getResultsCount(),
                                firebaseHistory.getSearchTimestamp()
                            );
                            localDatabase.searchHistoryDao().insertSync(localHistory);
                        }
                        Log.d(TAG, "Synced search history from cloud");
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing search history from cloud", e);
                        throw new RuntimeException(e);
                    }
                }, executorService);
            });
    }

    public CompletableFuture<Void> syncSinglePlace(Place place) {
        return firebaseRepository.syncPlaceToCloud(place);
    }

    public CompletableFuture<Void> syncSingleFavorite(Favorite favorite) {
        return firebaseRepository.syncFavoriteToCloud(favorite);
    }

    public CompletableFuture<Void> syncSingleSearchHistory(SearchHistory searchHistory) {
        return firebaseRepository.syncSearchHistoryToCloud(searchHistory);
    }

    public CompletableFuture<Void> deleteFavoriteFromCloud(String placeId) {
        return firebaseRepository.deleteFavoriteFromCloud(placeId);
    }

    public boolean isSyncing() {
        return isSyncing;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
