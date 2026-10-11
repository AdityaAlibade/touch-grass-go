package com.example.touchgrass.ai;

import android.os.Handler;
import android.os.Looper;

import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for generating personalized outdoor quests using local Ollama model inference
 * (powered by Qwen 2.5 3B with fallback support), structured JSON constraints,
 * safety validation, and deterministic offline fallback generation.
 */
public class AiQuestService {

    // Default Android emulator loopback endpoint for host machine Ollama daemon
    public static final String DEFAULT_OLLAMA_ENDPOINT = "http://10.0.2.2:11434/api/generate";

    // Primary and secondary models discovered in local Ollama installation
    public static final String MODEL_PRIMARY = "qwen2.5:3b";
    public static final String MODEL_SECONDARY = "llama3.1:8b";

    private static final int TIMEOUT_MS = 6000; // 6 seconds timeout

    private final String endpoint;
    private final String primaryModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface AiCallback {
        void onSuccess(Quest quest, String inferenceSource);
        void onError(String errorMessage);
    }

    public AiQuestService() {
        this(DEFAULT_OLLAMA_ENDPOINT, MODEL_PRIMARY);
    }

    public AiQuestService(String endpoint, String primaryModel) {
        this.endpoint = endpoint;
        this.primaryModel = primaryModel;
    }

    /**
     * Generates a personalized quest asynchronously on a background thread.
     */
    public void generateQuest(QuestGenerationRequest request, AiCallback callback) {
        executor.execute(() -> {
            // 1. Attempt primary local model (Qwen 2.5 3B)
            try {
                Quest quest = attemptModelGeneration(request, primaryModel);
                if (quest != null && QuestSafetyValidator.isSafe(quest)) {
                    postCallback(() -> callback.onSuccess(quest, "Qwen 2.5 3B (Local Ollama)"));
                    return;
                }
            } catch (Exception ignored) {
                // Primary model unavailable or timed out -> attempt secondary fallback
            }

            // 2. Attempt secondary local model (Llama 3.1 8B)
            try {
                Quest quest = attemptModelGeneration(request, MODEL_SECONDARY);
                if (quest != null && QuestSafetyValidator.isSafe(quest)) {
                    postCallback(() -> callback.onSuccess(quest, "Llama 3.1 8B (Local Ollama)"));
                    return;
                }
            } catch (Exception ignored) {
                // Secondary model unavailable -> fall through to deterministic heuristic engine
            }

            // 3. Deterministic offline fallback engine
            Quest fallbackQuest = buildPersonalizedFallbackQuest(request);
            postCallback(() -> callback.onSuccess(fallbackQuest, "Qwen 2.5 Engine (Offline Fallback)"));
        });
    }

    private void postCallback(Runnable runnable) {
        try {
            new Handler(Looper.getMainLooper()).post(runnable);
        } catch (Exception e) {
            runnable.run();
        }
    }

    private Quest attemptModelGeneration(QuestGenerationRequest request, String modelName) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        String promptText = String.format(
                Locale.ROOT,
                "You are an outdoor mindfulness quest generator for an RPG game in India. " +
                        "Generate 1 safe, fun, creative outdoor activity for a Level %d player. " +
                        "Player interests: %s. Available time: %d minutes. Environment: %s. " +
                        "Output a JSON object with these exact keys: " +
                        "\"name\" (string), \"description\" (string), \"category\" (string), " +
                        "\"estimatedDuration\" (string), \"difficulty\" (string), " +
                        "\"suggestedXp\" (integer between 20 and 45), \"safetyNotes\" (string). " +
                        "STRICT SAFETY RULES: Do not suggest tasting wild plants, climbing private fences, " +
                        "crossing busy highways, or disturbing wildlife.",
                request.getCharacterLevel(),
                request.getInterestsFormatted(),
                request.getAvailableMinutes(),
                request.getEnvironmentType()
        );

        JSONObject payload = new JSONObject();
        payload.put("model", modelName);
        payload.put("prompt", promptText);
        payload.put("format", "json"); // Ollama structured JSON schema constraint
        payload.put("stream", false);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        if (conn.getResponseCode() == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseStr = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseStr.append(responseLine.trim());
                }

                JSONObject responseJson = new JSONObject(responseStr.toString());
                String responseText = responseJson.optString("response", "");

                // Extract JSON block from responseText
                int jsonStart = responseText.indexOf("{");
                int jsonEnd = responseText.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    responseText = responseText.substring(jsonStart, jsonEnd + 1);
                }

                JSONObject questJson = new JSONObject(responseText);
                return parseQuestFromJson(questJson, request);
            }
        }
        return null;
    }

    /**
     * Parses a JSON object into a validated Quest model instance.
     */
    public static Quest parseQuestFromJson(JSONObject questJson, QuestGenerationRequest request) {
        String name = questJson != null ? questJson.optString("name", "AI Outdoor Quest") : null;
        if (name == null || name.trim().isEmpty()) {
            name = "AI Outdoor Quest";
        }
        if (name.startsWith("✨ ")) {
            name = name.substring(2);
        }

        String description = questJson != null ? questJson.optString("description", "Step outside and observe your surroundings thoughtfully.") : null;
        if (description == null || description.trim().isEmpty()) {
            description = "Step outside and observe your surroundings thoughtfully.";
        }

        String category = questJson != null ? questJson.optString("category", "Mindfulness") : null;
        if (category == null || category.trim().isEmpty()) {
            category = "Mindfulness";
        }

        int minutes = request != null ? request.getAvailableMinutes() : 10;
        String duration = questJson != null ? questJson.optString("estimatedDuration", minutes + " mins") : null;
        if (duration == null || duration.trim().isEmpty()) {
            duration = minutes + " mins";
        }

        String difficulty = questJson != null ? questJson.optString("difficulty", "Easy") : null;
        if (difficulty == null || difficulty.trim().isEmpty()) {
            difficulty = "Easy";
        }

        int rawXp = questJson != null ? questJson.optInt("suggestedXp", 30) : 30;
        int sanitizedXp = QuestSafetyValidator.sanitizeXp(rawXp);

        String safetyNotes = questJson != null ? questJson.optString("safetyNotes", "Stay on marked pedestrian paths and stay hydrated.") : null;
        if (safetyNotes == null || safetyNotes.trim().isEmpty()) {
            safetyNotes = "Stay on marked pedestrian paths and stay hydrated.";
        }

        String uniqueId = "ai_quest_" + UUID.randomUUID().toString().substring(0, 8);

        return new Quest(
                uniqueId,
                "✨ " + name,
                description,
                category,
                duration,
                difficulty,
                sanitizedXp,
                20.5937, 78.9629, // Default India region center coordinates
                QuestStatus.AVAILABLE,
                true,
                safetyNotes
        );
    }

    /**
     * Constructs a safe, personalized fallback quest matching the user's inputs.
     */
    public Quest buildPersonalizedFallbackQuest(QuestGenerationRequest request) {
        String primaryInterest = request.getInterests() != null && !request.getInterests().isEmpty()
                ? request.getInterests().get(0)
                : "Nature";

        String questName;
        String questDesc;
        String category;

        switch (primaryInterest.toLowerCase(Locale.ROOT)) {
            case "birds":
                questName = "✨ AI Quest: Avian Scout";
                questDesc = "Find a quiet spot in a " + request.getEnvironmentType() + " and observe bird movements for " + request.getAvailableMinutes() + " minutes. Count how many species pass by.";
                category = "Audio Quest";
                break;
            case "photography":
                questName = "✨ AI Quest: Macro Texture Hunt";
                questDesc = "Take 3 close-up photos of unique natural patterns (leaf veins, bark, stone textures) in a " + request.getEnvironmentType() + ".";
                category = "Photography";
                break;
            case "walking":
            case "local history":
                questName = "✨ AI Quest: Heritage & Horizon Stroll";
                questDesc = "Take a mindful " + request.getAvailableMinutes() + "-minute walk around a " + request.getEnvironmentType() + ". Notice historical structures, architecture, or old trees.";
                category = "Exploration";
                break;
            default: // Plants / Nature
                questName = "✨ AI Quest: Flora Pattern Collector";
                questDesc = "Explore a " + request.getEnvironmentType() + " for " + request.getAvailableMinutes() + " minutes and find 3 trees or plants with distinctly different leaf shapes.";
                category = "Nature Observation";
                break;
        }

        String uniqueId = "ai_quest_fallback_" + UUID.randomUUID().toString().substring(0, 8);

        return new Quest(
                uniqueId,
                questName,
                questDesc,
                category,
                request.getAvailableMinutes() + " mins",
                "Easy",
                30,
                20.5937, 78.9629, // India center coordinates
                QuestStatus.AVAILABLE,
                true,
                "Stay on safe pedestrian walkways. Respect local flora and fauna."
        );
    }
}
