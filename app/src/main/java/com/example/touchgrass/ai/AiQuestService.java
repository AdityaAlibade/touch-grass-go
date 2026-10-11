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
 * Service for generating personalized outdoor quests using an open-weight Gemma model endpoint,
 * with structured JSON parsing, strict safety validation, and fallback generation.
 */
public class AiQuestService {

    // Local Gemma 2 2B developer inference endpoint (e.g. Ollama dev server or local runtime)
    private static final String DEFAULT_GEMMA_ENDPOINT = "http://10.0.2.2:11434/api/generate";
    private static final int TIMEOUT_MS = 4000; // 4 seconds timeout

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface AiCallback {
        void onSuccess(Quest quest, String inferenceSource);
        void onError(String errorMessage);
    }

    /**
     * Generates a personalized quest asynchronously on a background thread.
     */
    public void generateQuest(QuestGenerationRequest request, AiCallback callback) {
        executor.execute(() -> {
            try {
                // Attempt Gemma open-weight model generation
                Quest quest = attemptGemmaModelGeneration(request);
                if (quest != null && QuestSafetyValidator.isSafe(quest)) {
                    postCallback(() -> callback.onSuccess(quest, "Gemma 2 2B (Local Dev Model)"));
                    return;
                }
            } catch (Exception ignored) {
                // Endpoint offline, timeout, or model uninstalled -> fallback safely
            }

            // Fallback generation: Construct a safe, personalized quest locally
            Quest fallbackQuest = buildPersonalizedFallbackQuest(request);
            postCallback(() -> callback.onSuccess(fallbackQuest, "Gemma Engine (Offline Fallback)"));
        });
    }

    private void postCallback(Runnable runnable) {
        try {
            new Handler(Looper.getMainLooper()).post(runnable);
        } catch (Exception e) {
            runnable.run();
        }
    }

    private Quest attemptGemmaModelGeneration(QuestGenerationRequest request) throws Exception {
        URL url = new URL(DEFAULT_GEMMA_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        String promptText = String.format(
                Locale.ROOT,
                "Generate 1 safe, fun outdoor quest in India for a Level %d player. " +
                        "Interests: %s. Available time: %d minutes. Environment: %s. " +
                        "Return ONLY a JSON object with keys: name, description, category, estimatedDuration, difficulty, suggestedXp, safetyNotes. " +
                        "Do not suggest trespassing, tasting plants, or crossing dangerous roads.",
                request.getCharacterLevel(),
                request.getInterestsFormatted(),
                request.getAvailableMinutes(),
                request.getEnvironmentType()
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "gemma2:2b");
        payload.put("prompt", promptText);
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

                // Extract JSON block from responseText if wrapped in markdown
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
     * Parses JSON object into a Quest model instance.
     */
    public static Quest parseQuestFromJson(JSONObject questJson, QuestGenerationRequest request) {
        String name = questJson.optString("name", "✨ AI Outdoor Quest");
        String description = questJson.optString("description", "Step outside and observe your surroundings thoughtfully.");
        String category = questJson.optString("category", "Mindfulness");
        String duration = questJson.optString("estimatedDuration", request.getAvailableMinutes() + " mins");
        String difficulty = questJson.optString("difficulty", "Easy");
        int rawXp = questJson.optInt("suggestedXp", 30);
        int sanitizedXp = QuestSafetyValidator.sanitizeXp(rawXp);
        String safetyNotes = questJson.optString("safetyNotes", "Stay on marked pedestrian paths and stay hydrated.");

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

        switch (primaryInterest.toLowerCase()) {
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
                20.5937, 78.9629, // Center coordinates
                QuestStatus.AVAILABLE,
                true,
                "Stay on safe pedestrian walkways. Respect local flora and fauna."
        );
    }
}
