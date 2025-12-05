package com.example.trave_app.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.trave_app.database.entity.Place;

import java.util.List;

@Dao
public interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Place place);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Place> places);

    @Update
    void update(Place place);

    @Delete
    void delete(Place place);

    @Query("SELECT * FROM places ORDER BY created_at DESC")
    LiveData<List<Place>> getAllPlaces();

    @Query("SELECT * FROM places WHERE category = :category ORDER BY created_at DESC")
    LiveData<List<Place>> getPlacesByCategory(String category);

    @Query("SELECT * FROM places WHERE is_favorite = 1 ORDER BY created_at DESC")
    LiveData<List<Place>> getFavoritePlaces();

    @Query("SELECT * FROM places WHERE place_id = :placeId LIMIT 1")
    Place getPlaceById(String placeId);

    @Query("SELECT * FROM places WHERE name LIKE '%' || :searchQuery || '%' ORDER BY created_at DESC")
    LiveData<List<Place>> searchPlacesByName(String searchQuery);

    @Query("UPDATE places SET is_favorite = :isFavorite WHERE place_id = :placeId")
    void updateFavoriteStatus(String placeId, boolean isFavorite);

    @Query("DELETE FROM places WHERE category = :category")
    void deleteByCategory(String category);

    @Query("DELETE FROM places")
    void deleteAllPlaces();

    @Query("SELECT COUNT(*) FROM places")
    int getPlaceCount();

    @Query("SELECT COUNT(*) FROM places WHERE category = :category")
    int getPlaceCountByCategory(String category);

    // Synchronous methods for Firebase sync
    @Query("SELECT * FROM places ORDER BY created_at DESC")
    List<Place> getAllPlacesSync();

    @Query("SELECT * FROM places WHERE place_id = :placeId LIMIT 1")
    Place getPlaceByPlaceIdSync(String placeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(Place place);
}
