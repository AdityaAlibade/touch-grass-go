package com.example.touchgrass.avatar;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.touchgrass.R;
import com.example.touchgrass.db.AppDatabase;
import com.example.touchgrass.db.DataMigrationHelper;
import com.example.touchgrass.db.PlayerProfileEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages character avatar selection and local persistence with Room database & SharedPreferences sync.
 */
public class AvatarManager {

    private static final String PREFS_NAME = "touch_grass_avatar_prefs";
    private static final String KEY_EQUIPPED_AVATAR_RES_ID = "equipped_avatar_res_id";

    private static AvatarManager instance;
    private final SharedPreferences preferences;
    private final AppDatabase database;

    public static class AvatarOption {
        public final int resId;
        public final String name;

        public AvatarOption(int resId, String name) {
            this.resId = resId;
            this.name = name;
        }
    }

    public AvatarManager(Context context) {
        Context appCtx = context.getApplicationContext();
        DataMigrationHelper.checkAndMigrate(appCtx);
        this.database = AppDatabase.getInstance(appCtx);
        this.preferences = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public AvatarManager(SharedPreferences preferences) {
        this.database = null;
        this.preferences = preferences;
    }

    public AvatarManager(AppDatabase database, SharedPreferences preferences) {
        this.database = database;
        this.preferences = preferences;
    }

    public static synchronized AvatarManager getInstance(Context context) {
        if (instance == null) {
            instance = new AvatarManager(context);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public int getEquippedAvatarResId() {
        if (database != null) {
            PlayerProfileEntity profile = database.playerProfileDao().getProfile();
            if (profile != null && profile.getEquippedAvatarResId() != 0) {
                return profile.getEquippedAvatarResId();
            }
        }
        return preferences != null ? preferences.getInt(KEY_EQUIPPED_AVATAR_RES_ID, R.drawable.ic_character_goblin) : R.drawable.ic_character_goblin;
    }

    public void setEquippedAvatarResId(int avatarResId) {
        if (database != null) {
            database.playerProfileDao().updateAvatar(avatarResId);
        }
        if (preferences != null) {
            preferences.edit().putInt(KEY_EQUIPPED_AVATAR_RES_ID, avatarResId).apply();
        }
    }

    public List<AvatarOption> getAvailableAvatarOptions() {
        List<AvatarOption> options = new ArrayList<>();
        options.add(new AvatarOption(R.drawable.ic_character_goblin, "Goblin Shield"));
        options.add(new AvatarOption(R.drawable.ic_grass_blade, "Grass Blade"));
        options.add(new AvatarOption(R.drawable.ic_sparkle, "Hero Sparkle"));
        options.add(new AvatarOption(R.drawable.ic_nav_map, "Compass Wanderer"));
        return options;
    }
}
