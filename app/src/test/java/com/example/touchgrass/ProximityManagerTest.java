package com.example.touchgrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import com.example.touchgrass.location.ProximityManager;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for GPS Haversine distance calculations, proximity boundary checks,
 * formatters, and null location fallback handling.
 */
public class ProximityManagerTest {

    private FakeSharedPreferences fakePrefs;
    private ProximityManager proximityManager;

    @Before
    public void setUp() {
        fakePrefs = new FakeSharedPreferences();
        proximityManager = new ProximityManager(fakePrefs);
    }

    @Test
    public void testSameLocationDistanceIsZero() {
        double distance = ProximityManager.calculateDistanceMeters(12.9763, 77.5929, 12.9763, 77.5929);
        assertEquals(0.0, distance, 0.01);
    }

    @Test
    public void testShortDistanceCalculation() {
        // ~100 meters difference in latitude (approx 0.0009 degrees)
        double lat1 = 12.9763;
        double lon1 = 77.5929;
        double lat2 = 12.9772;
        double lon2 = 77.5929;

        double distance = ProximityManager.calculateDistanceMeters(lat1, lon1, lat2, lon2);
        assertTrue("Distance should be around 100 meters", distance >= 90.0 && distance <= 110.0);
    }

    @Test
    public void testFormatDistanceMetric() {
        assertEquals("450 m", ProximityManager.formatDistance(450.2));
        assertEquals("0 m", ProximityManager.formatDistance(0.0));
        assertEquals("1.2 km", ProximityManager.formatDistance(1240.0));
        assertEquals("15.5 km", ProximityManager.formatDistance(15500.0));
    }

    @Test
    public void testEvaluateProximityWithinRadius() {
        proximityManager.setProximityRadiusMeters(500);

        // User is 100 meters away
        double userLat = 12.9772;
        double userLon = 77.5929;
        double questLat = 12.9763;
        double questLon = 77.5929;

        ProximityManager.ProximityResult result = proximityManager.evaluateProximity(userLat, userLon, questLat, questLon);
        assertTrue(result.locationAvailable);
        assertTrue(result.isWithinRadius);
        assertTrue(result.statusMessage.contains("Within exploration radius"));
    }

    @Test
    public void testEvaluateProximityOutsideRadius() {
        proximityManager.setProximityRadiusMeters(200);

        // User is ~1.5 km away (Bengaluru center to a distant point)
        double userLat = 12.9900;
        double userLon = 77.5929;
        double questLat = 12.9763;
        double questLon = 77.5929;

        ProximityManager.ProximityResult result = proximityManager.evaluateProximity(userLat, userLon, questLat, questLon);
        assertTrue(result.locationAvailable);
        assertFalse(result.isWithinRadius);
        assertTrue(result.statusMessage.contains("Target radius: 200 m"));
    }

    @Test
    public void testEvaluateProximityNullLocationHandling() {
        ProximityManager.ProximityResult result = proximityManager.evaluateProximity(null, null, 12.9763, 77.5929);
        assertNotNull(result);
        assertFalse(result.locationAvailable);
        assertFalse(result.isWithinRadius);
        assertEquals("Unknown distance", result.formattedDistance);
        assertTrue(result.statusMessage.contains("unavailable"));
    }

    @Test
    public void testConfigureProximitySettings() {
        assertFalse(proximityManager.isProximityVerificationEnabled());
        assertEquals(500, proximityManager.getProximityRadiusMeters());

        proximityManager.setProximityVerificationEnabled(true);
        proximityManager.setProximityRadiusMeters(1000);

        assertTrue(proximityManager.isProximityVerificationEnabled());
        assertEquals(1000, proximityManager.getProximityRadiusMeters());
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
