package com.example.touchgrass.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.touchgrass.db.AppDatabase;
import com.example.touchgrass.db.PlayerProfileEntity;

import org.osmdroid.views.MapView;

/**
 * Manages persistent Dark / Light / System theme modes and coordinates map tile overlays.
 */
public class ThemeManager {

    public static final String MODE_SYSTEM = "SYSTEM";
    public static final String MODE_LIGHT = "LIGHT";
    public static final String MODE_DARK = "DARK";

    private static final String PREFS_NAME = "touch_grass_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private static ThemeManager instance;
    private final SharedPreferences preferences;
    private final AppDatabase database;

    public ThemeManager(Context context) {
        Context appCtx = context.getApplicationContext();
        this.preferences = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.database = AppDatabase.getInstance(appCtx);
    }

    public ThemeManager(SharedPreferences preferences) {
        this.preferences = preferences;
        this.database = null;
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public String getThemeMode() {
        if (database != null) {
            PlayerProfileEntity profile = database.playerProfileDao().getProfile();
            if (profile != null && profile.getThemeMode() != null) {
                return profile.getThemeMode();
            }
        }
        return preferences != null ? preferences.getString(KEY_THEME_MODE, MODE_DARK) : MODE_DARK;
    }

    public void setThemeMode(String mode) {
        if (mode == null) mode = MODE_DARK;

        if (database != null) {
            database.playerProfileDao().updateThemeMode(mode);
        }
        if (preferences != null) {
            preferences.edit().putString(KEY_THEME_MODE, mode).apply();
        }

        applyTheme(mode);
    }

    /**
     * Applies the configured night mode via AppCompatDelegate.
     */
    public static void applyTheme(String mode) {
        if (MODE_LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (MODE_SYSTEM.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /**
     * Determines if dark mode is active in the current environment.
     */
    public static boolean isNightModeActive(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Coordinates OpenStreetMap tile styling according to day/night mode.
     */
    public static void coordinateMapTheme(MapView mapView, boolean isNight) {
        if (mapView == null || mapView.getOverlayManager() == null || mapView.getOverlayManager().getTilesOverlay() == null) {
            return;
        }

        if (isNight) {
            // Apply night invert filter with balanced contrast for comfortable outdoor night navigation
            ColorMatrix matrix = new ColorMatrix();
            matrix.set(new float[]{
                    -0.85f, 0, 0, 0, 230,
                    0, -0.85f, 0, 0, 230,
                    0, 0, -0.85f, 0, 230,
                    0, 0, 0, 1.0f, 0
            });
            mapView.getOverlayManager().getTilesOverlay().setColorFilter(new ColorMatrixColorFilter(matrix));
        } else {
            // Restore standard daylight crisp OpenStreetMap tiles
            mapView.getOverlayManager().getTilesOverlay().setColorFilter(null);
        }

        mapView.invalidate();
    }
}
