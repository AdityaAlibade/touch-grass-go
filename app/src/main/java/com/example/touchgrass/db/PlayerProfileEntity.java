package com.example.touchgrass.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing the player's profile and character sheet state.
 */
@Entity(tableName = "player_profile")
public class PlayerProfileEntity {

    @PrimaryKey
    private int id = 1;
    private String characterName;
    private int totalXp;
    private String equippedTitle;
    private int equippedAvatarResId;
    private String themeMode; // "SYSTEM", "LIGHT", "DARK"

    public PlayerProfileEntity(int id, String characterName, int totalXp,
                               String equippedTitle, int equippedAvatarResId, String themeMode) {
        this.id = id;
        this.characterName = characterName;
        this.totalXp = totalXp;
        this.equippedTitle = equippedTitle;
        this.equippedAvatarResId = equippedAvatarResId;
        this.themeMode = themeMode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    public String getEquippedTitle() { return equippedTitle; }
    public void setEquippedTitle(String equippedTitle) { this.equippedTitle = equippedTitle; }

    public int getEquippedAvatarResId() { return equippedAvatarResId; }
    public void setEquippedAvatarResId(int equippedAvatarResId) { this.equippedAvatarResId = equippedAvatarResId; }

    public String getThemeMode() { return themeMode; }
    public void setThemeMode(String themeMode) { this.themeMode = themeMode; }
}
