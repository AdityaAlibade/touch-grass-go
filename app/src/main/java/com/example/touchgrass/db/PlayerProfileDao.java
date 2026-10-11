package com.example.touchgrass.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Data Access Object for Player Profile in Room.
 */
@Dao
public interface PlayerProfileDao {

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    PlayerProfileEntity getProfile();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateProfile(PlayerProfileEntity profile);

    @Query("UPDATE player_profile SET totalXp = :totalXp WHERE id = 1")
    void updateTotalXp(int totalXp);

    @Query("UPDATE player_profile SET equippedTitle = :title WHERE id = 1")
    void updateTitle(String title);

    @Query("UPDATE player_profile SET equippedAvatarResId = :avatarResId WHERE id = 1")
    void updateAvatar(int avatarResId);

    @Query("UPDATE player_profile SET themeMode = :themeMode WHERE id = 1")
    void updateThemeMode(String themeMode);
}
