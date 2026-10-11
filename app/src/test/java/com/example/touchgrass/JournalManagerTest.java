package com.example.touchgrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import com.example.touchgrass.journal.JournalEntry;
import com.example.touchgrass.journal.JournalManager;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for JournalManager creation, duplicate prevention, and reflection updates.
 */
public class JournalManagerTest {

    private JournalManager journalManager;

    @Before
    public void setUp() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        journalManager = new JournalManager(prefs);
    }

    @Test
    public void testAddJournalEntryAndDuplicatePrevention() {
        assertEquals(0, journalManager.getAllJournalEntries().size());

        JournalEntry entry1 = new JournalEntry(
                "j1", "q1", "NPC Walk", "Mindfulness", 25, System.currentTimeMillis(), "Oct 24, 2026", "Certified Grass Toucher", "Fun walk!"
        );

        // First add -> success
        boolean added1 = journalManager.addJournalEntry(entry1);
        assertTrue(added1);
        assertEquals(1, journalManager.getAllJournalEntries().size());

        // Attempt duplicate add with same questId -> rejected
        boolean added2 = journalManager.addJournalEntry(entry1);
        assertFalse(added2);
        assertEquals(1, journalManager.getAllJournalEntries().size());
    }

    @Test
    public void testUpdatePersonalReflection() {
        JournalEntry entry = new JournalEntry(
                "j1", "q1", "NPC Walk", "Mindfulness", 25, System.currentTimeMillis(), "Oct 24, 2026", "", ""
        );
        journalManager.addJournalEntry(entry);

        // Update reflection note
        boolean updated = journalManager.updateReflection("q1", "Felt very relaxed!");
        assertTrue(updated);

        List<JournalEntry> list = journalManager.getAllJournalEntries();
        assertEquals(1, list.size());
        assertEquals("Felt very relaxed!", list.get(0).getReflection());
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
