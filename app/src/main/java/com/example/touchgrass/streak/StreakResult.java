package com.example.touchgrass.streak;

/**
 * Result model representing daily streak state updates.
 */
public class StreakResult {
    public final int currentStreak;
    public final int longestStreak;
    public final boolean streakIncremented;
    public final boolean streakReset;
    public final boolean sameDay;

    public StreakResult(int currentStreak, int longestStreak, boolean streakIncremented, boolean streakReset, boolean sameDay) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.streakIncremented = streakIncremented;
        this.streakReset = streakReset;
        this.sameDay = sameDay;
    }
}
