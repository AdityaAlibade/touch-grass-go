package com.example.touchgrass.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Data Access Object for Streak records in Room.
 */
@Dao
public interface StreakDao {

    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    StreakEntity getStreak();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateStreak(StreakEntity streak);

    @Query("UPDATE streaks SET currentStreak = :currentStreak, longestStreak = :longestStreak, lastCompletionDate = :date WHERE id = 1")
    void updateStreak(int currentStreak, int longestStreak, String date);
}
