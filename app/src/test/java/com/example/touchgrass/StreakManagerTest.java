package com.example.touchgrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import com.example.touchgrass.streak.StreakManager;
import com.example.touchgrass.streak.StreakResult;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for StreakManager date boundary calculations, streak incrementing,
 * missed-day reset, and longest streak preservation.
 */
public class StreakManagerTest {

    private FakeSharedPreferences streakPrefs;
    private StreakManager streakManager;

    @Before
    public void setUp() {
        streakPrefs = new FakeSharedPreferences();
        streakManager = new StreakManager(streakPrefs);
    }

    @Test
    public void testDateDifferenceCalculation() {
        // Same day
        assertEquals(0, StreakManager.calculateDayDifference("2026-10-24", "2026-10-24"));

        // Consecutive days
        assertEquals(1, StreakManager.calculateDayDifference("2026-10-24", "2026-10-25"));

        // Missed two days
        assertEquals(3, StreakManager.calculateDayDifference("2026-10-24", "2026-10-27"));

        // Month boundary transition (Feb 28 to Mar 1 non-leap year)
        assertEquals(1, StreakManager.calculateDayDifference("2026-02-28", "2026-03-01"));

        // Year boundary transition (Dec 31 to Jan 1)
        assertEquals(1, StreakManager.calculateDayDifference("2026-12-31", "2027-01-01"));
    }

    @Test
    public void testMultipleCompletionsSameDay() {
        StreakResult res1 = streakManager.recordQuestCompletionOnDateString("2026-10-24");
        assertEquals(1, res1.currentStreak);
        assertTrue(res1.streakIncremented);

        // Second completion on the same day -> streak stays 1
        StreakResult res2 = streakManager.recordQuestCompletionOnDateString("2026-10-24");
        assertEquals(1, res2.currentStreak);
        assertTrue(res2.sameDay);
        assertFalse(res2.streakReset);
    }

    @Test
    public void testConsecutiveDayIncrementsAndLongestStreak() {
        streakManager.recordQuestCompletionOnDateString("2026-10-24");

        // Day 2
        StreakResult res2 = streakManager.recordQuestCompletionOnDateString("2026-10-25");
        assertEquals(2, res2.currentStreak);
        assertEquals(2, res2.longestStreak);
        assertTrue(res2.streakIncremented);

        // Day 3
        StreakResult res3 = streakManager.recordQuestCompletionOnDateString("2026-10-26");
        assertEquals(3, res3.currentStreak);
        assertEquals(3, res3.longestStreak);
        assertTrue(res3.streakIncremented);
    }

    @Test
    public void testMissedDayResetsCurrentStreakAndPreservesLongest() {
        // Build 3-day streak
        streakManager.recordQuestCompletionOnDateString("2026-10-24");
        streakManager.recordQuestCompletionOnDateString("2026-10-25");
        streakManager.recordQuestCompletionOnDateString("2026-10-26");
        assertEquals(3, streakManager.getCurrentStreak());
        assertEquals(3, streakManager.getLongestStreak());

        // Miss Oct 27, complete on Oct 28
        StreakResult resReset = streakManager.recordQuestCompletionOnDateString("2026-10-28");
        assertEquals(1, resReset.currentStreak); // Reset to 1
        assertEquals(3, resReset.longestStreak); // Longest streak preserved as 3!
        assertTrue(resReset.streakReset);
    }

    // In-memory FakeSharedPreferences implementation for JUnit tests
    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, ?> getAll() { return map; }

        @Override
        public String getString(String key, String defValue) {
            return map.containsKey(key) ? (String) map.get(key) : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return map.containsKey(key) ? new HashSet<>((Set<String>) map.get(key)) : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            return map.containsKey(key) ? (Integer) map.get(key) : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            return map.containsKey(key) ? (Long) map.get(key) : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return map.containsKey(key) ? (Float) map.get(key) : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return map.containsKey(key) ? (Boolean) map.get(key) : defValue;
        }

        @Override
        public boolean contains(String key) { return map.containsKey(key); }

        @Override
        public Editor edit() { return new FakeEditor(); }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private class FakeEditor implements Editor {
            private final Map<String, Object> temp = new HashMap<>(map);

            @Override
            public Editor putString(String key, String value) { temp.put(key, value); return this; }
            @Override
            public Editor putStringSet(String key, Set<String> values) { temp.put(key, new HashSet<>(values)); return this; }
            @Override
            public Editor putInt(String key, int value) { temp.put(key, value); return this; }
            @Override
            public Editor putLong(String key, long value) { temp.put(key, value); return this; }
            @Override
            public Editor putFloat(String key, float value) { temp.put(key, value); return this; }
            @Override
            public Editor putBoolean(String key, boolean value) { temp.put(key, value); return this; }
            @Override
            public Editor remove(String key) { temp.remove(key); return this; }
            @Override
            public Editor clear() { temp.clear(); return this; }
            @Override
            public boolean commit() { map.clear(); map.putAll(temp); return true; }
            @Override
            public void apply() { commit(); }
        }
    }
}
