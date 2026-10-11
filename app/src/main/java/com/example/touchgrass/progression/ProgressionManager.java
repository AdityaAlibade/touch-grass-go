package com.example.touchgrass.progression;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;
import com.example.touchgrass.repository.QuestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ProgressionManager handles XP, leveling, thresholds, titles, and persistence.
 * Separated from UI code for testability.
 */
public class ProgressionManager {

    private static final String PREFS_NAME = "touch_grass_progression_prefs";
    private static final String KEY_TOTAL_XP = "total_xp";
    private static final String KEY_EQUIPPED_TITLE = "equipped_title";

    // Title Constants
    public static final String TITLE_DEFAULT = "Certified Indoor Goblin";
    public static final String TITLE_GRASS_TOUCHER = "Certified Grass Toucher";
    public static final String TITLE_LEAF_ENTHUSIAST = "Leaf Enthusiast";
    public static final String TITLE_PIGEON_DIPLOMAT = "Pigeon Diplomat";
    public static final String TITLE_GOBLIN_BOTANIST = "Goblin Botanist";

    private static ProgressionManager instance;
    private final SharedPreferences preferences;
    private final QuestRepository questRepository;

    public ProgressionManager(Context context, QuestRepository questRepository) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE), questRepository);
    }

    public ProgressionManager(SharedPreferences preferences, QuestRepository questRepository) {
        this.preferences = preferences;
        this.questRepository = questRepository;
    }

    public static synchronized ProgressionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProgressionManager(context, QuestRepository.getInstance(context));
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    // ================= LEVEL THRESHOLD CALCULATIONS =================

    /**
     * Calculates character level based on total cumulative XP.
     * Thresholds:
     * - Level 1: 0 XP
     * - Level 2: 100 XP
     * - Level 3: 250 XP
     * - Level 4: 450 XP
     * - Level 5: 700 XP
     */
    public static int calculateLevel(int totalXp) {
        if (totalXp < 0) return 1;
        if (totalXp < 100) return 1;
        if (totalXp < 250) return 2;
        if (totalXp < 450) return 3;
        if (totalXp < 700) return 4;
        return 5 + (totalXp - 700) / 300;
    }

    /**
     * Returns total XP required for the start of the specified level.
     */
    public static int getLevelBaseXp(int level) {
        if (level <= 1) return 0;
        if (level == 2) return 100;
        if (level == 3) return 250;
        if (level == 4) return 450;
        if (level == 5) return 700;
        return 700 + (level - 5) * 300;
    }

    /**
     * Returns total XP required to reach the NEXT level.
     */
    public static int getNextLevelThreshold(int currentLevel) {
        return getLevelBaseXp(currentLevel + 1);
    }

    // ================= PROGRESSION & PERSISTENCE =================

    public int getTotalXp() {
        return preferences.getInt(KEY_TOTAL_XP, 0);
    }

    public int getCurrentLevel() {
        return calculateLevel(getTotalXp());
    }

    public String getEquippedTitle() {
        return preferences.getString(KEY_EQUIPPED_TITLE, TITLE_DEFAULT);
    }

    public boolean setEquippedTitle(String title) {
        if (getUnlockedTitles().contains(title)) {
            preferences.edit().putString(KEY_EQUIPPED_TITLE, title).apply();
            return true;
        }
        return false;
    }

    /**
     * Calculates unlocked titles deterministically from completed quest count.
     */
    public List<String> getUnlockedTitles() {
        int completedCount = questRepository.getCompletedQuestIds().size();
        List<String> titles = new ArrayList<>();
        titles.add(TITLE_DEFAULT); // Default at 0 quests

        if (completedCount >= 1) {
            titles.add(TITLE_GRASS_TOUCHER);
        }
        if (completedCount >= 3) {
            titles.add(TITLE_LEAF_ENTHUSIAST);
        }
        if (completedCount >= 5) {
            titles.add(TITLE_PIGEON_DIPLOMAT);
        }
        if (completedCount >= 10) {
            titles.add(TITLE_GOBLIN_BOTANIST);
        }

        return titles;
    }

    /**
     * Atomically completes a quest, awards XP exactly once, updates level, and unlocks titles.
     */
    public CompletionResult completeQuest(Quest quest) {
        if (quest == null || quest.getStatus() != QuestStatus.ACTIVE) {
            return new CompletionResult(false, 0, getTotalXp(), getTotalXp(), getCurrentLevel(), getCurrentLevel(), false, new ArrayList<>());
        }

        int previousCompletedCount = questRepository.getCompletedQuestIds().size();
        boolean marked = questRepository.completeQuest(quest.getId());

        if (!marked) {
            // Already completed or invalid state
            return new CompletionResult(false, 0, getTotalXp(), getTotalXp(), getCurrentLevel(), getCurrentLevel(), false, new ArrayList<>());
        }

        int previousXp = getTotalXp();
        int previousLevel = calculateLevel(previousXp);
        int xpReward = quest.getXpReward();
        int newXp = previousXp + xpReward;
        int newLevel = calculateLevel(newXp);
        boolean leveledUp = newLevel > previousLevel;

        // Save new XP
        preferences.edit().putInt(KEY_TOTAL_XP, newXp).apply();

        // Check newly unlocked titles
        int newCompletedCount = questRepository.getCompletedQuestIds().size();
        List<String> newlyUnlocked = getNewlyUnlockedTitles(previousCompletedCount, newCompletedCount);

        // Auto-equip latest unlocked title if still equipped with default
        if (!newlyUnlocked.isEmpty() && TITLE_DEFAULT.equals(getEquippedTitle())) {
            String latest = newlyUnlocked.get(newlyUnlocked.size() - 1);
            setEquippedTitle(latest);
        }

        return new CompletionResult(true, xpReward, previousXp, newXp, previousLevel, newLevel, leveledUp, newlyUnlocked);
    }

    private List<String> getNewlyUnlockedTitles(int prevCount, int newCount) {
        List<String> newlyUnlocked = new ArrayList<>();
        if (prevCount < 1 && newCount >= 1) newlyUnlocked.add(TITLE_GRASS_TOUCHER);
        if (prevCount < 3 && newCount >= 3) newlyUnlocked.add(TITLE_LEAF_ENTHUSIAST);
        if (prevCount < 5 && newCount >= 5) newlyUnlocked.add(TITLE_PIGEON_DIPLOMAT);
        if (prevCount < 10 && newCount >= 10) newlyUnlocked.add(TITLE_GOBLIN_BOTANIST);
        return newlyUnlocked;
    }

    /**
     * Clears progression data (useful for testing or reset).
     */
    public void clearAll() {
        preferences.edit().clear().apply();
    }

    // ================= RESULT MODEL =================

    public static class CompletionResult {
        public final boolean success;
        public final int xpGained;
        public final int previousXp;
        public final int newXp;
        public final int previousLevel;
        public final int newLevel;
        public final boolean leveledUp;
        public final List<String> newlyUnlockedTitles;

        public CompletionResult(boolean success, int xpGained, int previousXp, int newXp,
                                int previousLevel, int newLevel, boolean leveledUp,
                                List<String> newlyUnlockedTitles) {
            this.success = success;
            this.xpGained = xpGained;
            this.previousXp = previousXp;
            this.newXp = newXp;
            this.previousLevel = previousLevel;
            this.newLevel = newLevel;
            this.leveledUp = leveledUp;
            this.newlyUnlockedTitles = newlyUnlockedTitles;
        }
    }
}
