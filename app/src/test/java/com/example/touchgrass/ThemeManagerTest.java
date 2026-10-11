package com.example.touchgrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.SharedPreferences;

import com.example.touchgrass.theme.ThemeManager;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for ThemeManager mode configuration, persistence, and fallbacks.
 */
public class ThemeManagerTest {

    private FakeSharedPreferences fakePrefs;
    private ThemeManager themeManager;

    @Before
    public void setUp() {
        fakePrefs = new FakeSharedPreferences();
        themeManager = new ThemeManager(fakePrefs);
    }

    @Test
    public void testDefaultThemeIsDarkForest() {
        assertEquals(ThemeManager.MODE_DARK, themeManager.getThemeMode());
    }

    @Test
    public void testSwitchToDaylightMeadow() {
        themeManager.setThemeMode(ThemeManager.MODE_LIGHT);
        assertEquals(ThemeManager.MODE_LIGHT, themeManager.getThemeMode());
        assertEquals(ThemeManager.MODE_LIGHT, fakePrefs.getString("theme_mode", null));
    }

    @Test
    public void testSwitchToSystemDefault() {
        themeManager.setThemeMode(ThemeManager.MODE_SYSTEM);
        assertEquals(ThemeManager.MODE_SYSTEM, themeManager.getThemeMode());
        assertEquals(ThemeManager.MODE_SYSTEM, fakePrefs.getString("theme_mode", null));
    }

    @Test
    public void testSwitchBackToDarkForest() {
        themeManager.setThemeMode(ThemeManager.MODE_LIGHT);
        assertEquals(ThemeManager.MODE_LIGHT, themeManager.getThemeMode());

        themeManager.setThemeMode(ThemeManager.MODE_DARK);
        assertEquals(ThemeManager.MODE_DARK, themeManager.getThemeMode());
    }

    @Test
    public void testNullModeDefaultsToDark() {
        themeManager.setThemeMode(null);
        assertEquals(ThemeManager.MODE_DARK, themeManager.getThemeMode());
    }

    @Test
    public void testMapCoordinateNullSafety() {
        // coordinateMapTheme must safely return without throwing NPE when MapView is null
        ThemeManager.coordinateMapTheme(null, true);
        ThemeManager.coordinateMapTheme(null, false);
    }

    // In-memory FakeSharedPreferences implementation for JUnit tests
    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override public Map<String, ?> getAll() { return map; }
        @Override public String getString(String key, String def) { return map.containsKey(key) ? (String) map.get(key) : def; }
        @Override @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> def) { return map.containsKey(key) ? new HashSet<>((Set<String>) map.get(key)) : def; }
        @Override public int getInt(String key, int def) { return map.containsKey(key) ? (Integer) map.get(key) : def; }
        @Override public long getLong(String key, long def) { return map.containsKey(key) ? (Long) map.get(key) : def; }
        @Override public float getFloat(String key, float def) { return map.containsKey(key) ? (Float) map.get(key) : def; }
        @Override public boolean getBoolean(String key, boolean def) { return map.containsKey(key) ? (Boolean) map.get(key) : def; }
        @Override public boolean contains(String key) { return map.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}

        private class FakeEditor implements Editor {
            private final Map<String, Object> temp = new HashMap<>(map);
            @Override public Editor putString(String k, String v) { temp.put(k, v); return this; }
            @Override public Editor putStringSet(String k, Set<String> v) { temp.put(k, new HashSet<>(v)); return this; }
            @Override public Editor putInt(String k, int v) { temp.put(k, v); return this; }
            @Override public Editor putLong(String k, long v) { temp.put(k, v); return this; }
            @Override public Editor putFloat(String k, float v) { temp.put(k, v); return this; }
            @Override public Editor putBoolean(String k, boolean v) { temp.put(k, v); return this; }
            @Override public Editor remove(String k) { temp.remove(k); return this; }
            @Override public Editor clear() { temp.clear(); return this; }
            @Override public boolean commit() { map.clear(); map.putAll(temp); return true; }
            @Override public void apply() { commit(); }
        }
    }
}
