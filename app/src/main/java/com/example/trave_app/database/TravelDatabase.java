package com.example.trave_app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.trave_app.database.dao.FavoriteDao;
import com.example.trave_app.database.dao.PlaceDao;
import com.example.trave_app.database.dao.SearchHistoryDao;
import com.example.trave_app.database.dao.AINotificationDao;
import com.example.trave_app.database.dao.NotificationPreferenceDao;
import com.example.trave_app.database.dao.UserDao;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.database.entity.User;
import com.example.trave_app.notifications.model.AINotification;
import com.example.trave_app.notifications.model.NotificationPreference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {Place.class, SearchHistory.class, Favorite.class, AINotification.class, NotificationPreference.class, User.class},
        version = 3,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class TravelDatabase extends RoomDatabase {

    // Abstract methods to get DAOs
    public abstract PlaceDao placeDao();
    public abstract SearchHistoryDao searchHistoryDao();
    public abstract FavoriteDao favoriteDao();
    public abstract AINotificationDao aiNotificationDao();
    public abstract NotificationPreferenceDao notificationPreferenceDao();
    public abstract UserDao userDao();

    // Singleton instance
    private static volatile TravelDatabase INSTANCE;

    // Executor service for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Get database instance (Singleton pattern)
    public static TravelDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TravelDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    TravelDatabase.class, "travel_database")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .addCallback(roomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Alias method for getInstance (used by DataSyncService)
    public static TravelDatabase getInstance(final Context context) {
        return getDatabase(context);
    }

    // Database callback for initialization
    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            // If you want to keep data through app restarts,
            // comment out the following block
            databaseWriteExecutor.execute(() -> {
                // Populate the database in the background.
                // If you want to start with more data, just add it here.
                PlaceDao placeDao = INSTANCE.placeDao();
                SearchHistoryDao searchHistoryDao = INSTANCE.searchHistoryDao();
                FavoriteDao favoriteDao = INSTANCE.favoriteDao();
                NotificationPreferenceDao notificationPreferenceDao = INSTANCE.notificationPreferenceDao();

                // Initialize default notification preferences
                NotificationPreference defaultPreferences = new NotificationPreference();
                notificationPreferenceDao.insert(defaultPreferences);

                // Add any initial data here if needed
            });
        }
    };

    // Migration strategies (for future database updates)
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create AI notifications table
            database.execSQL("CREATE TABLE IF NOT EXISTS `ai_notifications` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT, " +
                    "`message` TEXT, " +
                    "`type` TEXT, " +
                    "`scheduledTime` INTEGER NOT NULL, " +
                    "`createdTime` INTEGER NOT NULL, " +
                    "`isDelivered` INTEGER NOT NULL, " +
                    "`isRead` INTEGER NOT NULL, " +
                    "`priority` INTEGER NOT NULL, " +
                    "`relatedPlaceId` TEXT, " +
                    "`actionData` TEXT, " +
                    "`relevanceScore` REAL NOT NULL, " +
                    "`category` TEXT)");

            // Create notification preferences table
            database.execSQL("CREATE TABLE IF NOT EXISTS `notification_preferences` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`smartRecommendationsEnabled` INTEGER NOT NULL, " +
                    "`patternAlertsEnabled` INTEGER NOT NULL, " +
                    "`locationAwareEnabled` INTEGER NOT NULL, " +
                    "`travelInsightsEnabled` INTEGER NOT NULL, " +
                    "`favoriteUpdatesEnabled` INTEGER NOT NULL, " +
                    "`smartRemindersEnabled` INTEGER NOT NULL, " +
                    "`weatherAlertsEnabled` INTEGER NOT NULL, " +
                    "`quietHoursStart` INTEGER NOT NULL, " +
                    "`quietHoursEnd` INTEGER NOT NULL, " +
                    "`respectQuietHours` INTEGER NOT NULL, " +
                    "`maxDailyNotifications` INTEGER NOT NULL, " +
                    "`minTimeBetweenNotifications` INTEGER NOT NULL, " +
                    "`minimumPriorityLevel` INTEGER NOT NULL, " +
                    "`minimumRelevanceScore` REAL NOT NULL, " +
                    "`enableLocationBasedTiming` INTEGER NOT NULL, " +
                    "`locationRadiusKm` REAL NOT NULL)");
        }
    };

    // Method to close the database
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
