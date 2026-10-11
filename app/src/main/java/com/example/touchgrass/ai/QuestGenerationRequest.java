package com.example.touchgrass.ai;

import java.util.List;

/**
 * Request payload for personalized AI quest generation.
 * Contains non-sensitive preference choices (interests, time, environment, character level).
 */
public class QuestGenerationRequest {

    private final int characterLevel;
    private final List<String> interests;
    private final int availableMinutes;
    private final String environmentType;

    public QuestGenerationRequest(int characterLevel, List<String> interests,
                                  int availableMinutes, String environmentType) {
        this.characterLevel = characterLevel;
        this.interests = interests;
        this.availableMinutes = availableMinutes;
        this.environmentType = environmentType;
    }

    public int getCharacterLevel() {
        return characterLevel;
    }

    public List<String> getInterests() {
        return interests;
    }

    public int getAvailableMinutes() {
        return availableMinutes;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public String getInterestsFormatted() {
        if (interests == null || interests.isEmpty()) {
            return "General Exploration";
        }
        return String.join(", ", interests);
    }
}
