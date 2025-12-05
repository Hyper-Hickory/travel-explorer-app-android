package com.example.trave_app.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.trave_app.database.entity.Favorite;

import java.util.List;

@Dao
public interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Favorite favorite);

    @Update
    void update(Favorite favorite);

    @Delete
    void delete(Favorite favorite);

    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    LiveData<List<Favorite>> getAllFavorites();

    @Query("SELECT * FROM favorites WHERE category = :category ORDER BY added_at DESC")
    LiveData<List<Favorite>> getFavoritesByCategory(String category);

    @Query("SELECT * FROM favorites WHERE place_id = :placeId LIMIT 1")
    Favorite getFavoriteByPlaceId(String placeId);

    @Query("SELECT * FROM favorites WHERE name LIKE '%' || :searchQuery || '%' ORDER BY added_at DESC")
    LiveData<List<Favorite>> searchFavoritesByName(String searchQuery);

    @Query("DELETE FROM favorites WHERE place_id = :placeId")
    void deleteByPlaceId(String placeId);

    @Query("DELETE FROM favorites")
    void deleteAllFavorites();

    @Query("SELECT COUNT(*) FROM favorites")
    int getFavoriteCount();

    @Query("SELECT COUNT(*) FROM favorites WHERE category = :category")
    int getFavoriteCountByCategory(String category);

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE place_id = :placeId)")
    boolean isFavorite(String placeId);

    // Synchronous methods for Firebase sync
    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    List<Favorite> getAllFavoritesSync();

    @Query("SELECT * FROM favorites WHERE place_id = :placeId LIMIT 1")
    Favorite getFavoriteByPlaceIdSync(String placeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(Favorite favorite);
}
