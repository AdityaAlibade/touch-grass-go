package com.example.touchgrass.avatar;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.touchgrass.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages character avatar selection and local persistence.
 */
public class AvatarManager {

    private static final String PREFS_NAME = "touch_grass_avatar_prefs";
    private static final String KEY_EQUIPPED_AVATAR_RES_ID = "equipped_avatar_res_id";

    private static AvatarManager instance;
    private final SharedPreferences preferences;

    public static class AvatarOption {
        public final int resId;
        public final String name;

        public AvatarOption(int resId, String name) {
            this.resId = resId;
            this.name = name;
        }
    }

    public AvatarManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AvatarManager getInstance(Context context) {
        if (instance == null) {
            instance = new AvatarManager(context);
        }
        return instance;
    }

    public int getEquippedAvatarResId() {
        return preferences.getInt(KEY_EQUIPPED_AVATAR_RES_ID, R.drawable.ic_character_goblin);
    }

    public void setEquippedAvatarResId(int avatarResId) {
        preferences.edit().putInt(KEY_EQUIPPED_AVATAR_RES_ID, avatarResId).apply();
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
