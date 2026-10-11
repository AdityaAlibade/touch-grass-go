package com.example.touchgrass.ai;

import com.example.touchgrass.model.Quest;

import java.util.Locale;

/**
 * Safety & Quality Validator for AI-generated outdoor quests.
 * Enforces strict guidelines to ensure user safety and fair progression rules.
 */
public class QuestSafetyValidator {

    private static final String[] PROHIBITED_KEYWORDS = {
            "taste", "eat", "ingest", "flavor", "chew", "mushroom", "berry",
            "trespass", "private property", "climb fence", "off-limits", "restricted area",
            "highway", "busy traffic", "cross expressway", "track",
            "catch bird", "touch animal", "disturb nest", "capture wildlife",
            "buy", "purchase", "specialized equipment", "ticket"
    };

    /**
     * Validates an AI-generated quest against strict outdoor safety rules.
     */
    public static boolean isSafe(Quest quest) {
        if (quest == null) return false;

        String combinedText = (quest.getName() + " " + quest.getDescription() + " " + quest.getSafetyNotes())
                .toLowerCase(Locale.ROOT);

        for (String word : PROHIBITED_KEYWORDS) {
            if (combinedText.contains(word)) {
                return false;
            }
        }

        // Validate basic fields presence
        if (quest.getName() == null || quest.getName().trim().isEmpty()) return false;
        if (quest.getDescription() == null || quest.getDescription().trim().isEmpty()) return false;

        return true;
    }

    /**
     * Sanitizes and caps suggested XP reward to prevent game inflation (15 XP min, 50 XP max).
     */
    public static int sanitizeXp(int suggestedXp) {
        if (suggestedXp < 15) return 15;
        if (suggestedXp > 50) return 50;
        return suggestedXp;
    }
}
