package com.example.touchgrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.touchgrass.ai.AiQuestService;
import com.example.touchgrass.ai.QuestGenerationRequest;
import com.example.touchgrass.ai.QuestSafetyValidator;
import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Unit tests for AI Quest Safety Validator, XP Sanitization, and Personalized Fallback Quest Generation.
 */
public class AiQuestParserTest {

    private AiQuestService aiQuestService;

    @Before
    public void setUp() {
        aiQuestService = new AiQuestService();
    }

    @Test
    public void testSafetyValidatorRejectsUnsafeQuests() {
        // Test unsafe quest with ingestion/tasting instruction
        Quest unsafeTaste = new Quest("q1", "Mushroom Taste Test", "Taste wild berries or mushrooms in the forest", "Nature", "10m", "Easy", 30, 0, 0, QuestStatus.AVAILABLE, true, "Caution");
        assertFalse(QuestSafetyValidator.isSafe(unsafeTaste));

        // Test unsafe quest with trespassing instruction
        Quest unsafeTrespass = new Quest("q2", "Fence Climb", "Climb fence and enter private property", "Exploration", "10m", "Easy", 30, 0, 0, QuestStatus.AVAILABLE, true, "Caution");
        assertFalse(QuestSafetyValidator.isSafe(unsafeTrespass));

        // Test safe quest
        Quest safeQuest = new Quest("q3", "Tree Bark Sketch", "Observe tree bark patterns in a public park", "Nature", "10m", "Easy", 30, 0, 0, QuestStatus.AVAILABLE, true, "Stay on footpaths");
        assertTrue(QuestSafetyValidator.isSafe(safeQuest));
    }

    @Test
    public void testSanitizeXpBounds() {
        // Under lower bound -> capped to 15
        assertEquals(15, QuestSafetyValidator.sanitizeXp(5));

        // Within bounds -> preserved
        assertEquals(35, QuestSafetyValidator.sanitizeXp(35));

        // Over upper bound -> capped to 50
        assertEquals(50, QuestSafetyValidator.sanitizeXp(120));
    }

    @Test
    public void testPersonalizedFallbackQuestGeneration() {
        QuestGenerationRequest req = new QuestGenerationRequest(2, Collections.singletonList("Birds"), 10, "Urban Neighborhood");
        Quest fallback = aiQuestService.buildPersonalizedFallbackQuest(req);

        assertNotNull(fallback);
        assertTrue(fallback.getName().contains("AI Quest"));
        assertTrue(fallback.getDescription().contains("bird"));
        assertEquals("Audio Quest", fallback.getCategory());
        assertTrue(fallback.isAiGenerated());
        assertTrue(QuestSafetyValidator.isSafe(fallback));
    }

    @Test
    public void testParseQuestFromJson() throws Exception {
        org.json.JSONObject mockJson = new org.json.JSONObject();
        mockJson.put("name", "Banyan Canopy Contemplation");
        mockJson.put("description", "Find a banyan or neem tree in your neighborhood and study its aerial roots or canopy structure.");
        mockJson.put("category", "Nature Mindfulness");
        mockJson.put("estimatedDuration", "15 mins");
        mockJson.put("difficulty", "Easy");
        mockJson.put("suggestedXp", 35);
        mockJson.put("safetyNotes", "Stay on clear pedestrian footpaths.");

        QuestGenerationRequest req = new QuestGenerationRequest(1, Collections.singletonList("Nature"), 15, "Park / Garden");
        Quest quest = AiQuestService.parseQuestFromJson(mockJson, req);

        assertNotNull(quest);
        assertTrue(quest.getName().contains("AI"));
        assertTrue(quest.isAiGenerated());
        assertTrue(QuestSafetyValidator.isSafe(quest));
    }
}
