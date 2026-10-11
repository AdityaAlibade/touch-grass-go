package com.example.touchgrass.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * AppDatabase is the central Room SQLite database for Touch Grass Go.
 * Provides persistent storage for quests, adventure journal, player profile, and streaks.
 */
@Database(
        entities = {
                QuestEntity.class,
                JournalEntity.class,
                PlayerProfileEntity.class,
                StreakEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "touch_grass_go.db";
    private static volatile AppDatabase instance;

    public abstract QuestDao questDao();
    public abstract JournalDao journalDao();
    public abstract PlayerProfileDao playerProfileDao();
    public abstract StreakDao streakDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .allowMainThreadQueries() // Allowed for deterministic offline operations
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }

    /**
     * Replaces the instance (used for in-memory databases in testing).
     */
    public static void setInstance(AppDatabase testDatabase) {
        instance = testDatabase;
    }
}
