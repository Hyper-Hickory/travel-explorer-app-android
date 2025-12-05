package com.example.trave_app.firebase.repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.trave_app.firebase.model.FirebasePlace;
import com.example.trave_app.firebase.model.FirebaseFavorite;
import com.example.trave_app.firebase.model.FirebaseSearchHistory;
import com.example.trave_app.firebase.model.FirebaseUser;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";
    private static final String PLACES_COLLECTION = "places";
    private static final String FAVORITES_COLLECTION = "favorites";
    private static final String SEARCH_HISTORY_COLLECTION = "search_history";
    private static final String USERS_COLLECTION = "users";
    
    private FirebaseFirestore db;
    private static FirebaseRepository instance;

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

    // Users operations
    public java.util.concurrent.CompletableFuture<Void> syncUserRegistration(String username, String phone, String location, long createdAt) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        FirebaseUser fu = new FirebaseUser(username, phone, location, createdAt, null);
        db.collection(USERS_COLLECTION)
                .document(username)
                .set(fu.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User synced to cloud: " + username);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing user to cloud", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    public java.util.concurrent.CompletableFuture<Void> recordUserLogin(String username) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("lastLoginAt", System.currentTimeMillis());
        db.collection(USERS_COLLECTION)
                .document(username)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Login recorded for user: " + username);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error recording login", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    // Places operations
    public CompletableFuture<Void> syncPlaceToCloud(Place place) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        FirebasePlace firebasePlace = new FirebasePlace(
            place.getPlaceId(), place.getName(), place.getCategory(),
            place.getLatitude(), place.getLongitude(), place.getAddress(),
            place.getRating(), place.isFavorite(), place.getCreatedAt()
        );

        db.collection(PLACES_COLLECTION)
            .document(place.getPlaceId())
            .set(firebasePlace.toMap())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Place synced to cloud: " + place.getName());
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error syncing place to cloud", e);
                future.completeExceptionally(e);
            });
            
        return future;
    }

    public CompletableFuture<List<FirebasePlace>> getPlacesFromCloud() {
        CompletableFuture<List<FirebasePlace>> future = new CompletableFuture<>();
        
        db.collection(PLACES_COLLECTION)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<FirebasePlace> places = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        FirebasePlace place = document.toObject(FirebasePlace.class);
                        place.setDocumentId(document.getId());
                        places.add(place);
                    }
                    future.complete(places);
                } else {
                    Log.e(TAG, "Error getting places from cloud", task.getException());
                    future.completeExceptionally(task.getException());
                }
            });
            
        return future;
    }

    // Favorites operations
    public CompletableFuture<Void> syncFavoriteToCloud(Favorite favorite) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        FirebaseFavorite firebaseFavorite = new FirebaseFavorite(
            favorite.getPlaceId(), favorite.getName(), favorite.getCategory(),
            favorite.getLatitude(), favorite.getLongitude(), favorite.getAddress(),
            favorite.getRating(), favorite.getNotes(), favorite.getAddedAt()
        );

        db.collection(FAVORITES_COLLECTION)
            .document(favorite.getPlaceId())
            .set(firebaseFavorite.toMap())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Favorite synced to cloud: " + favorite.getName());
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error syncing favorite to cloud", e);
                future.completeExceptionally(e);
            });
            
        return future;
    }

    public CompletableFuture<List<FirebaseFavorite>> getFavoritesFromCloud() {
        CompletableFuture<List<FirebaseFavorite>> future = new CompletableFuture<>();
        
        db.collection(FAVORITES_COLLECTION)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<FirebaseFavorite> favorites = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        FirebaseFavorite favorite = document.toObject(FirebaseFavorite.class);
                        favorite.setDocumentId(document.getId());
                        favorites.add(favorite);
                    }
                    future.complete(favorites);
                } else {
                    Log.e(TAG, "Error getting favorites from cloud", task.getException());
                    future.completeExceptionally(task.getException());
                }
            });
            
        return future;
    }

    public CompletableFuture<Void> deleteFavoriteFromCloud(String placeId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        db.collection(FAVORITES_COLLECTION)
            .document(placeId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Favorite deleted from cloud: " + placeId);
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting favorite from cloud", e);
                future.completeExceptionally(e);
            });
            
        return future;
    }

    // Search History operations
    public CompletableFuture<Void> syncSearchHistoryToCloud(SearchHistory searchHistory) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        FirebaseSearchHistory firebaseSearchHistory = new FirebaseSearchHistory(
            searchHistory.getSearchQuery(), searchHistory.getCategory(),
            searchHistory.getLatitude(), searchHistory.getLongitude(),
            searchHistory.getResultsCount(), searchHistory.getSearchTimestamp()
        );

        db.collection(SEARCH_HISTORY_COLLECTION)
            .add(firebaseSearchHistory.toMap())
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Search history synced to cloud: " + searchHistory.getSearchQuery());
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error syncing search history to cloud", e);
                future.completeExceptionally(e);
            });
            
        return future;
    }

    public CompletableFuture<List<FirebaseSearchHistory>> getSearchHistoryFromCloud() {
        CompletableFuture<List<FirebaseSearchHistory>> future = new CompletableFuture<>();
        
        db.collection(SEARCH_HISTORY_COLLECTION)
            .orderBy("searchTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50) // Limit to recent 50 searches
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<FirebaseSearchHistory> searchHistories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        FirebaseSearchHistory searchHistory = document.toObject(FirebaseSearchHistory.class);
                        searchHistory.setDocumentId(document.getId());
                        searchHistories.add(searchHistory);
                    }
                    future.complete(searchHistories);
                } else {
                    Log.e(TAG, "Error getting search history from cloud", task.getException());
                    future.completeExceptionally(task.getException());
                }
            });
            
        return future;
    }

    // Batch operations for initial sync
    public CompletableFuture<Void> syncAllPlacesToCloud(List<Place> places) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        if (places.isEmpty()) {
            future.complete(null);
            return future;
        }

        List<CompletableFuture<Void>> syncTasks = new ArrayList<>();
        for (Place place : places) {
            syncTasks.add(syncPlaceToCloud(place));
        }

        CompletableFuture.allOf(syncTasks.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                Log.d(TAG, "All places synced to cloud");
                future.complete(null);
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Error syncing all places to cloud", throwable);
                future.completeExceptionally(throwable);
                return null;
            });
            
        return future;
    }

    public CompletableFuture<Void> syncAllFavoritesToCloud(List<Favorite> favorites) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        if (favorites.isEmpty()) {
            future.complete(null);
            return future;
        }

        List<CompletableFuture<Void>> syncTasks = new ArrayList<>();
        for (Favorite favorite : favorites) {
            syncTasks.add(syncFavoriteToCloud(favorite));
        }

        CompletableFuture.allOf(syncTasks.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                Log.d(TAG, "All favorites synced to cloud");
                future.complete(null);
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Error syncing all favorites to cloud", throwable);
                future.completeExceptionally(throwable);
                return null;
            });
            
        return future;
    }
}
