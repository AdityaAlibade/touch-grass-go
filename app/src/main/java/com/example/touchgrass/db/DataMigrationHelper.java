package com.example.touchgrass.db;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.touchgrass.R;
import com.example.touchgrass.journal.JournalEntry;
import com.example.touchgrass.journal.JournalManager;
import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;
import com.example.touchgrass.progression.ProgressionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles seamless, one-time migration of user progress from legacy SharedPreferences
 * to the Room SQLite database, ensuring no duplicate XP or lost journal entries.
 */
public class DataMigrationHelper {

    private static final String MIGRATION_PREFS = "touch_grass_migration_prefs";
    private static final String KEY_MIGRATED_TO_ROOM = "migrated_to_room_v1";

    public static synchronized void checkAndMigrate(Context context) {
        SharedPreferences migrationPrefs = context.getApplicationContext().getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE);
        if (migrationPrefs.getBoolean(KEY_MIGRATED_TO_ROOM, false)) {
            // Already migrated
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context);

        // 1. Migrate Player Profile & XP
        SharedPreferences progPrefs = context.getApplicationContext().getSharedPreferences("touch_grass_progression_prefs", Context.MODE_PRIVATE);
        SharedPreferences avatarPrefs = context.getApplicationContext().getSharedPreferences("touch_grass_avatar_prefs", Context.MODE_PRIVATE);

        int legacyXp = progPrefs.getInt("total_xp", 0);
        String legacyTitle = progPrefs.getString("equipped_title", ProgressionManager.TITLE_DEFAULT);
        int legacyAvatar = avatarPrefs.getInt("equipped_avatar_res_id", R.drawable.ic_character_goblin);

        PlayerProfileEntity existingProfile = db.playerProfileDao().getProfile();
        if (existingProfile == null) {
            db.playerProfileDao().insertOrUpdateProfile(new PlayerProfileEntity(
                    1,
                    "Grass Rookie",
                    legacyXp,
                    legacyTitle,
                    legacyAvatar,
                    "SYSTEM"
            ));
        }

        // 2. Migrate Daily Streak
        SharedPreferences streakPrefs = context.getApplicationContext().getSharedPreferences("touch_grass_streak_prefs", Context.MODE_PRIVATE);
        int legacyCurrentStreak = streakPrefs.getInt("current_streak", 0);
        int legacyLongestStreak = streakPrefs.getInt("longest_streak", 0);
        String legacyLastDate = streakPrefs.getString("last_completion_date", null);

        StreakEntity existingStreak = db.streakDao().getStreak();
        if (existingStreak == null) {
            db.streakDao().insertOrUpdateStreak(new StreakEntity(
                    1,
                    legacyCurrentStreak,
                    legacyLongestStreak,
                    legacyLastDate
            ));
        }

        // 3. Migrate Journal Entries
        SharedPreferences journalPrefs = context.getApplicationContext().getSharedPreferences("touch_grass_journal_prefs", Context.MODE_PRIVATE);
        String rawJournalData = journalPrefs.getString("journal_raw_data", "");
        if (rawJournalData != null && !rawJournalData.trim().isEmpty()) {
            JournalManager legacyManager = new JournalManager(journalPrefs);
            List<JournalEntry> legacyEntries = legacyManager.getAllJournalEntries();
            for (JournalEntry entry : legacyEntries) {
                if (entry != null && db.journalDao().getEntryByQuestId(entry.getQuestId()) == null) {
                    db.journalDao().insertEntry(JournalEntity.fromJournalEntry(entry));
                }
            }
        }

        // 4. Migrate Quests (Demo quests + saved AI Quests + Statuses)
        SharedPreferences questPrefs = context.getApplicationContext().getSharedPreferences("touch_grass_quests_prefs", Context.MODE_PRIVATE);
        Set<String> activeIds = questPrefs.getStringSet("active_quest_ids", new HashSet<>());
        Set<String> completedIds = questPrefs.getStringSet("completed_quest_ids", new HashSet<>());

        // Ensure default seeded quests are in Room
        seedDefaultQuests(db, activeIds, completedIds);

        // Migrate AI quests from JSON
        String aiJson = questPrefs.getString("ai_quests_json", "");
        if (aiJson != null && !aiJson.trim().isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(aiJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String id = obj.optString("id");
                    if (db.questDao().getQuestById(id) == null) {
                        String statusStr = obj.optString("status", QuestStatus.AVAILABLE.name());
                        if (completedIds != null && completedIds.contains(id)) {
                            statusStr = QuestStatus.COMPLETED.name();
                        } else if (activeIds != null && activeIds.contains(id)) {
                            statusStr = QuestStatus.ACTIVE.name();
                        }

                        QuestEntity aiQuestEntity = new QuestEntity(
                                id,
                                obj.optString("name", "✨ AI Quest"),
                                obj.optString("description", "Outdoor observation"),
                                obj.optString("category", "Mindfulness"),
                                obj.optString("estimatedDuration", "10 mins"),
                                obj.optString("difficulty", "Easy"),
                                obj.optInt("xpReward", 30),
                                obj.optDouble("latitude", 20.5937),
                                obj.optDouble("longitude", 78.9629),
                                statusStr,
                                true,
                                obj.optString("safetyNotes", "Stay on safe paths"),
                                0L
                        );
                        db.questDao().insertQuest(aiQuestEntity);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Mark migration completed
        migrationPrefs.edit().putBoolean(KEY_MIGRATED_TO_ROOM, true).apply();
    }

    /**
     * Seeds initial Indian landmark quests into Room if not already populated.
     */
    public static void seedDefaultQuests(AppDatabase db, Set<String> activeIds, Set<String> completedIds) {
        if (db.questDao().getQuestCount() > 0) {
            // Already seeded, just update statuses if needed
            if (activeIds != null) {
                for (String id : activeIds) {
                    db.questDao().updateQuestStatus(id, QuestStatus.ACTIVE.name());
                }
            }
            if (completedIds != null) {
                for (String id : completedIds) {
                    db.questDao().updateQuestStatus(id, QuestStatus.COMPLETED.name());
                }
            }
            return;
        }

        List<QuestEntity> seeds = new ArrayList<>();

        // Quest 1: NPC Walk
        seeds.add(new QuestEntity(
                "quest_npc_walk",
                "NPC Walk",
                "Walk for 10 minutes without checking social media or screens. Observe your surroundings as a peaceful NPC taking in the scenery.",
                "Mindfulness",
                "10 mins",
                "Easy",
                25,
                12.9763, 77.5929, // Cubbon Park, Bengaluru
                determineStatus("quest_npc_walk", activeIds, completedIds),
                false,
                "Stay on designated footpaths and stay hydrated.",
                0L
        ));

        // Quest 2: Leaf Detective
        seeds.add(new QuestEntity(
                "quest_leaf_detective",
                "Leaf Detective",
                "Observe three distinctly different leaf shapes on outdoor trees or plants and notice their unique textures and patterns.",
                "Nature Observation",
                "10 mins",
                "Easy",
                30,
                28.5931, 77.2197, // Lodhi Garden, New Delhi
                determineStatus("quest_leaf_detective", activeIds, completedIds),
                false,
                "Avoid touching unknown nettles or thorns.",
                0L
        ));

        // Quest 3: Bird Diplomat
        seeds.add(new QuestEntity(
                "quest_bird_diplomat",
                "Bird Diplomat",
                "Listen carefully for bird calls for five minutes without talking. Count how many different bird calls and songs you can spot.",
                "Audio Quest",
                "5 mins",
                "Easy",
                20,
                19.2307, 72.8567, // Sanjay Gandhi National Park, Mumbai
                determineStatus("quest_bird_diplomat", activeIds, completedIds),
                false,
                "Do not feed or disturb wild birds.",
                0L
        ));

        // Quest 4: Main Character Moment
        seeds.add(new QuestEntity(
                "quest_main_character",
                "Main Character Moment",
                "Find a pleasant bench or shade tree. Sit for 5 full minutes observing life flow around you without opening any smartphone apps.",
                "Mindfulness",
                "5 mins",
                "Easy",
                25,
                13.0827, 80.2707, // Semmozhi Poonga, Chennai
                determineStatus("quest_main_character", activeIds, completedIds),
                false,
                "Choose a well-lit, public seating area.",
                0L
        ));

        // Quest 5: Cloud Cartographer
        seeds.add(new QuestEntity(
                "quest_cloud_cartographer",
                "Cloud Cartographer",
                "Look up at the open sky for 5 minutes. Notice cloud movement and patterns, or appreciate the open sky horizon.",
                "Sky Gazing",
                "5 mins",
                "Easy",
                20,
                26.9124, 75.7873, // Central Park, Jaipur
                determineStatus("quest_cloud_cartographer", activeIds, completedIds),
                false,
                "Never look directly into the sun.",
                0L
        ));

        db.questDao().insertQuests(seeds);
    }

    private static String determineStatus(String questId, Set<String> activeIds, Set<String> completedIds) {
        if (completedIds != null && completedIds.contains(questId)) {
            return QuestStatus.COMPLETED.name();
        }
        if (activeIds != null && activeIds.contains(questId)) {
            return QuestStatus.ACTIVE.name();
        }
        return QuestStatus.AVAILABLE.name();
    }
}
