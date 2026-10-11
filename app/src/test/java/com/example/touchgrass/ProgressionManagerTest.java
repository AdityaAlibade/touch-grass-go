package com.example.touchgrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;
import com.example.touchgrass.progression.ProgressionManager;
import com.example.touchgrass.repository.QuestRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for ProgressionManager calculation logic, leveling thresholds,
 * double-completion prevention, title milestones, and persistence survival.
 */
public class ProgressionManagerTest {

    private FakeSharedPreferences questPrefs;
    private FakeSharedPreferences progressionPrefs;
    private QuestRepository questRepository;
    private ProgressionManager progressionManager;

    @Before
    public void setUp() {
        questPrefs = new FakeSharedPreferences();
        progressionPrefs = new FakeSharedPreferences();
        questRepository = new QuestRepository(questPrefs);
        progressionManager = new ProgressionManager(progressionPrefs, questRepository);
    }

    @Test
    public void testLevelCalculationThresholds() {
        // Level 1: 0 to 99 XP
        assertEquals(1, ProgressionManager.calculateLevel(0));
        assertEquals(1, ProgressionManager.calculateLevel(50));
        assertEquals(1, ProgressionManager.calculateLevel(99));

        // Level 2: 100 to 249 XP
        assertEquals(2, ProgressionManager.calculateLevel(100));
        assertEquals(2, ProgressionManager.calculateLevel(200));
        assertEquals(2, ProgressionManager.calculateLevel(249));

        // Level 3: 250 to 449 XP
        assertEquals(3, ProgressionManager.calculateLevel(250));
        assertEquals(3, ProgressionManager.calculateLevel(449));

        // Level 4: 450 to 699 XP
        assertEquals(4, ProgressionManager.calculateLevel(450));
        assertEquals(4, ProgressionManager.calculateLevel(699));

        // Level 5+: 700+ XP
        assertEquals(5, ProgressionManager.calculateLevel(700));
        assertEquals(6, ProgressionManager.calculateLevel(1000));
    }

    @Test
    public void testAwardXpAndPreventDuplicateCompletion() {
        Quest quest = new Quest("q1", "Test Quest", "Desc", "Mindfulness", "10m", "Easy", 25, 0, 0, QuestStatus.AVAILABLE);

        // Cannot complete AVAILABLE quest directly
        ProgressionManager.CompletionResult result1 = progressionManager.completeQuest(quest);
        assertFalse(result1.success);
        assertEquals(0, progressionManager.getTotalXp());

        // Accept quest first
        questRepository.acceptQuest(quest.getId());
        quest.setStatus(QuestStatus.ACTIVE);

        // Complete quest -> XP awarded exactly once
        ProgressionManager.CompletionResult result2 = progressionManager.completeQuest(quest);
        assertTrue(result2.success);
        assertEquals(25, result2.xpGained);
        assertEquals(25, progressionManager.getTotalXp());

        // Attempting to complete the same quest AGAIN must NOT award duplicate XP
        ProgressionManager.CompletionResult result3 = progressionManager.completeQuest(quest);
        assertFalse(result3.success);
        assertEquals(25, progressionManager.getTotalXp());
    }

    @Test
    public void testLevelUpAcrossThresholds() {
        Quest q1 = new Quest("q1", "Quest 1", "Desc", "Cat", "10m", "Easy", 60, 0, 0, QuestStatus.AVAILABLE);
        Quest q2 = new Quest("q2", "Quest 2", "Desc", "Cat", "10m", "Easy", 50, 0, 0, QuestStatus.AVAILABLE);

        questRepository.acceptQuest(q1.getId());
        q1.setStatus(QuestStatus.ACTIVE);
        ProgressionManager.CompletionResult res1 = progressionManager.completeQuest(q1);
        assertEquals(60, progressionManager.getTotalXp());
        assertEquals(1, progressionManager.getCurrentLevel());
        assertFalse(res1.leveledUp);

        questRepository.acceptQuest(q2.getId());
        q2.setStatus(QuestStatus.ACTIVE);
        ProgressionManager.CompletionResult res2 = progressionManager.completeQuest(q2);
        assertEquals(110, progressionManager.getTotalXp());
        assertEquals(2, progressionManager.getCurrentLevel());
        assertTrue(res2.leveledUp);
        assertEquals(1, res2.previousLevel);
        assertEquals(2, res2.newLevel);
    }

    @Test
    public void testMultiThresholdJump() {
        Quest bigQuest = new Quest("q_big", "Big Quest", "Desc", "Cat", "30m", "Hard", 300, 0, 0, QuestStatus.AVAILABLE);
        questRepository.acceptQuest(bigQuest.getId());
        bigQuest.setStatus(QuestStatus.ACTIVE);

        ProgressionManager.CompletionResult res = progressionManager.completeQuest(bigQuest);
        assertTrue(res.success);
        assertEquals(300, progressionManager.getTotalXp());
        assertEquals(1, res.previousLevel);
        assertEquals(3, res.newLevel); // 300 XP is Level 3 (250-449 XP)
        assertTrue(res.leveledUp);
    }

    @Test
    public void testTitleUnlockingMilestones() {
        Quest q1 = new Quest("q1", "Q1", "Desc", "Cat", "5m", "Easy", 10, 0, 0, QuestStatus.AVAILABLE);
        questRepository.acceptQuest(q1.getId());
        q1.setStatus(QuestStatus.ACTIVE);

        ProgressionManager.CompletionResult res = progressionManager.completeQuest(q1);
        assertTrue(res.newlyUnlockedTitles.contains(ProgressionManager.TITLE_GRASS_TOUCHER));
        assertEquals(ProgressionManager.TITLE_GRASS_TOUCHER, progressionManager.getEquippedTitle());
    }

    @Test
    public void testPersistenceSurvivesAppRestart() {
        Quest q1 = new Quest("q1", "Q1", "Desc", "Cat", "5m", "Easy", 100, 0, 0, QuestStatus.AVAILABLE);
        questRepository.acceptQuest(q1.getId());
        q1.setStatus(QuestStatus.ACTIVE);
        progressionManager.completeQuest(q1);

        // Simulate app restart by instantiating NEW objects with the same SharedPreferences
        QuestRepository newRepo = new QuestRepository(questPrefs);
        ProgressionManager newManager = new ProgressionManager(progressionPrefs, newRepo);

        assertEquals(100, newManager.getTotalXp());
        assertEquals(2, newManager.getCurrentLevel());
        assertEquals(ProgressionManager.TITLE_GRASS_TOUCHER, newManager.getEquippedTitle());
        assertTrue(newRepo.getCompletedQuestIds().contains("q1"));
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
