package com.example.touchgrass.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.touchgrass.db.AppDatabase;
import com.example.touchgrass.db.DataMigrationHelper;
import com.example.touchgrass.db.QuestEntity;
import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository for managing quests in Touch Grass Go.
 * Uses Room SQLite database as primary persistence with legacy SharedPreferences sync & fallback.
 */
public class QuestRepository {

    private static final String PREFS_NAME = "touch_grass_quests_prefs";
    private static final String KEY_ACTIVE_QUEST_IDS = "active_quest_ids";
    private static final String KEY_COMPLETED_QUEST_IDS = "completed_quest_ids";
    private static final String KEY_AI_QUESTS_JSON = "ai_quests_json";

    private static QuestRepository instance;
    private final SharedPreferences preferences;
    private final AppDatabase database;

    private QuestRepository(Context context) {
        Context appCtx = context.getApplicationContext();
        DataMigrationHelper.checkAndMigrate(appCtx);
        this.database = AppDatabase.getInstance(appCtx);
        this.preferences = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public QuestRepository(SharedPreferences preferences) {
        this.database = null;
        this.preferences = preferences;
    }

    public QuestRepository(AppDatabase database, SharedPreferences preferences) {
        this.database = database;
        this.preferences = preferences;
    }

    public static synchronized QuestRepository getInstance(Context context) {
        if (instance == null) {
            instance = new QuestRepository(context);
        }
        return instance;
    }

    /**
     * Resets repository instance (useful for unit testing).
     */
    public static synchronized void resetInstance() {
        instance = null;
    }

    /**
     * Gets all quests (both predefined and persisted AI-generated quests).
     */
    public List<Quest> getAllQuests() {
        if (database != null) {
            List<QuestEntity> entities = database.questDao().getAllQuests();
            if (entities != null && !entities.isEmpty()) {
                List<Quest> quests = new ArrayList<>();
                for (QuestEntity entity : entities) {
                    quests.add(entity.toQuest());
                }
                return quests;
            }
        }

        // Fallback / legacy path using SharedPreferences
        Set<String> activeIds = getActiveQuestIds();
        Set<String> completedIds = getCompletedQuestIds();
        List<Quest> quests = new ArrayList<>();

        // Quest 1: NPC Walk
        quests.add(createQuest(
                "quest_npc_walk",
                "NPC Walk",
                "Walk for 10 minutes without checking social media or screens. Observe your surroundings as a peaceful NPC taking in the scenery.",
                "Mindfulness",
                "10 mins",
                "Easy",
                25,
                12.9763, 77.5929, // Cubbon Park, Bengaluru
                activeIds, completedIds, false, "Stay on designated footpaths and stay hydrated."
        ));

        // Quest 2: Leaf Detective
        quests.add(createQuest(
                "quest_leaf_detective",
                "Leaf Detective",
                "Observe three distinctly different leaf shapes on outdoor trees or plants and notice their unique textures and patterns.",
                "Nature Observation",
                "10 mins",
                "Easy",
                30,
                28.5931, 77.2197, // Lodhi Garden, New Delhi
                activeIds, completedIds, false, "Avoid touching unknown nettles or thorns."
        ));

        // Quest 3: Bird Diplomat
        quests.add(createQuest(
                "quest_bird_diplomat",
                "Bird Diplomat",
                "Listen carefully for bird calls for five minutes without talking. Count how many different bird calls and songs you can spot.",
                "Audio Quest",
                "5 mins",
                "Easy",
                20,
                19.2307, 72.8567, // Sanjay Gandhi National Park, Mumbai
                activeIds, completedIds, false, "Do not feed or disturb wild birds."
        ));

        // Quest 4: Main Character Moment
        quests.add(createQuest(
                "quest_main_character",
                "Main Character Moment",
                "Explore a safe, familiar outdoor area for 15 minutes. Take in the fresh air and feel like the hero of your own outdoor journey.",
                "Exploration",
                "15 mins",
                "Medium",
                40,
                26.9038, 75.8078, // Central Park, Jaipur
                activeIds, completedIds, false, "Stay aware of your surroundings."
        ));

        // Load Persisted AI Quests
        List<Quest> aiQuests = getSavedAiQuests(activeIds, completedIds);
        quests.addAll(aiQuests);

        return quests;
    }

    private Quest createQuest(String id, String name, String desc, String cat, String dur, String diff, int xp,
                              double lat, double lng, Set<String> activeIds, Set<String> completedIds,
                              boolean isAiGenerated, String safetyNotes) {
        QuestStatus status;
        if (completedIds.contains(id)) {
            status = QuestStatus.COMPLETED;
        } else if (activeIds.contains(id)) {
            status = QuestStatus.ACTIVE;
        } else {
            status = QuestStatus.AVAILABLE;
        }
        return new Quest(id, name, desc, cat, dur, diff, xp, lat, lng, status, isAiGenerated, safetyNotes);
    }

    /**
     * Persists an AI-generated quest to Room and SharedPreferences.
     */
    public void saveAiQuest(Quest quest) {
        if (quest == null) return;

        if (database != null) {
            database.questDao().insertQuest(QuestEntity.fromQuest(quest));
        }

        if (preferences != null) {
            try {
                String jsonStr = preferences.getString(KEY_AI_QUESTS_JSON, "[]");
                JSONArray array = new JSONArray(jsonStr);

                JSONObject obj = new JSONObject();
                obj.put("id", quest.getId());
                obj.put("name", quest.getName());
                obj.put("desc", quest.getDescription());
                obj.put("cat", quest.getCategory());
                obj.put("dur", quest.getEstimatedDuration());
                obj.put("diff", quest.getDifficulty());
                obj.put("xp", quest.getXpReward());
                obj.put("lat", quest.getLatitude());
                obj.put("lng", quest.getLongitude());
                obj.put("safety", quest.getSafetyNotes());

                array.put(obj);
                preferences.edit().putString(KEY_AI_QUESTS_JSON, array.toString()).apply();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Loads persisted AI quests from SharedPreferences.
     */
    public List<Quest> getSavedAiQuests(Set<String> activeIds, Set<String> completedIds) {
        List<Quest> aiList = new ArrayList<>();
        if (preferences == null) return aiList;
        try {
            String jsonStr = preferences.getString(KEY_AI_QUESTS_JSON, "[]");
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id");
                String name = obj.optString("name");
                String desc = obj.optString("desc");
                String cat = obj.optString("cat");
                String dur = obj.optString("dur");
                String diff = obj.optString("diff");
                int xp = obj.optInt("xp");
                double lat = obj.optDouble("lat", 20.5937);
                double lng = obj.optDouble("lng", 78.9629);
                String safety = obj.optString("safety");

                aiList.add(createQuest(id, name, desc, cat, dur, diff, xp, lat, lng, activeIds, completedIds, true, safety));
            }
        } catch (Exception ignored) {}
        return aiList;
    }

    /**
     * Persists a quest as accepted/active in Room and SharedPreferences.
     */
    public boolean acceptQuest(String questId) {
        Set<String> activeIds = new HashSet<>(getActiveQuestIds());
        Set<String> completedIds = getCompletedQuestIds();

        // If already completed, do not accept again
        if (completedIds.contains(questId)) {
            return false;
        }

        if (database != null) {
            database.questDao().updateQuestStatus(questId, QuestStatus.ACTIVE.name());
        }

        boolean added = activeIds.add(questId);
        if (added && preferences != null) {
            preferences.edit().putStringSet(KEY_ACTIVE_QUEST_IDS, activeIds).apply();
        }
        return true;
    }

    /**
     * Marks a quest as COMPLETED and removes it from ACTIVE list.
     */
    public boolean completeQuest(String questId) {
        Set<String> completedIds = new HashSet<>(getCompletedQuestIds());
        Set<String> activeIds = new HashSet<>(getActiveQuestIds());

        // Already completed
        if (completedIds.contains(questId)) {
            return false;
        }

        if (database != null) {
            database.questDao().markQuestCompleted(questId, System.currentTimeMillis());
        }

        boolean added = completedIds.add(questId);
        activeIds.remove(questId);

        if (added && preferences != null) {
            preferences.edit()
                    .putStringSet(KEY_COMPLETED_QUEST_IDS, completedIds)
                    .putStringSet(KEY_ACTIVE_QUEST_IDS, activeIds)
                    .apply();
        }
        return added;
    }

    /**
     * Retrieves the set of accepted quest IDs.
     */
    public Set<String> getActiveQuestIds() {
        if (database != null) {
            return new HashSet<>(database.questDao().getActiveQuestIds());
        }
        return preferences != null ? preferences.getStringSet(KEY_ACTIVE_QUEST_IDS, new HashSet<>()) : new HashSet<>();
    }

    /**
     * Retrieves the set of completed quest IDs.
     */
    public Set<String> getCompletedQuestIds() {
        if (database != null) {
            return new HashSet<>(database.questDao().getCompletedQuestIds());
        }
        return preferences != null ? preferences.getStringSet(KEY_COMPLETED_QUEST_IDS, new HashSet<>()) : new HashSet<>();
    }

    /**
     * Retrieves only ACTIVE quests.
     */
    public List<Quest> getActiveQuests() {
        List<Quest> activeList = new ArrayList<>();
        for (Quest q : getAllQuests()) {
            if (q.getStatus() == QuestStatus.ACTIVE) {
                activeList.add(q);
            }
        }
        return activeList;
    }

    /**
     * Retrieves only AVAILABLE quests.
     */
    public List<Quest> getAvailableQuests() {
        List<Quest> availableList = new ArrayList<>();
        for (Quest q : getAllQuests()) {
            if (q.getStatus() == QuestStatus.AVAILABLE) {
                availableList.add(q);
            }
        }
        return availableList;
    }

    /**
     * Retrieves only COMPLETED quests.
     */
    public List<Quest> getCompletedQuests() {
        List<Quest> completedList = new ArrayList<>();
        for (Quest q : getAllQuests()) {
            if (q.getStatus() == QuestStatus.COMPLETED) {
                completedList.add(q);
            }
        }
        return completedList;
    }

    /**
     * Clears repository preferences and database (useful for testing or reset).
     */
    public void clearAll() {
        if (preferences != null) {
            preferences.edit().clear().apply();
        }
    }
}
