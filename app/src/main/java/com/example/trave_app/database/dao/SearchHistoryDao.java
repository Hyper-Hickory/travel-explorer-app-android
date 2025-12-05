package com.example.trave_app.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.trave_app.database.entity.SearchHistory;

import java.util.List;

@Dao
public interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SearchHistory searchHistory);

    @Delete
    void delete(SearchHistory searchHistory);

    @Query("SELECT * FROM search_history ORDER BY search_timestamp DESC")
    LiveData<List<SearchHistory>> getAllSearchHistory();

    @Query("SELECT * FROM search_history WHERE category = :category ORDER BY search_timestamp DESC")
    LiveData<List<SearchHistory>> getSearchHistoryByCategory(String category);

    @Query("SELECT * FROM search_history ORDER BY search_timestamp DESC LIMIT :limit")
    LiveData<List<SearchHistory>> getRecentSearchHistory(int limit);

    @Query("SELECT DISTINCT category FROM search_history ORDER BY search_timestamp DESC")
    LiveData<List<String>> getSearchCategories();

    @Query("DELETE FROM search_history WHERE search_timestamp < :timestamp")
    void deleteOldSearchHistory(long timestamp);

    @Query("DELETE FROM search_history")
    void deleteAllSearchHistory();

    @Query("SELECT COUNT(*) FROM search_history")
    int getSearchHistoryCount();

    // Synchronous methods for Firebase sync
    @Query("SELECT * FROM search_history ORDER BY search_timestamp DESC")
    List<SearchHistory> getAllSearchHistorySync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(SearchHistory searchHistory);
}
