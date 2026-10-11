package com.example.touchgrass.streak;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Manages daily quest completion streaks, consecutive day tracking, date boundaries,
 * and longest streak persistence.
 */
public class StreakManager {

    private static final String PREFS_NAME = "touch_grass_streak_prefs";
    private static final String KEY_LAST_COMPLETION_DATE = "last_completion_date";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_LONGEST_STREAK = "longest_streak";

    private static StreakManager instance;
    private final SharedPreferences preferences;

    public StreakManager(Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    public StreakManager(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static synchronized StreakManager getInstance(Context context) {
        if (instance == null) {
            instance = new StreakManager(context);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public int getCurrentStreak() {
        return preferences.getInt(KEY_CURRENT_STREAK, 0);
    }

    public int getLongestStreak() {
        return preferences.getInt(KEY_LONGEST_STREAK, 0);
    }

    public String getLastCompletionDate() {
        return preferences.getString(KEY_LAST_COMPLETION_DATE, null);
    }

    /**
     * Records a quest completion on the specified date string (yyyy-MM-dd) or current date if null.
     */
    public StreakResult recordQuestCompletionOnDateString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            dateString = getCurrentFormattedDate();
        }

        String lastDate = getLastCompletionDate();
        int currentStreak = getCurrentStreak();
        int longestStreak = getLongestStreak();

        boolean streakIncremented = false;
        boolean streakReset = false;
        boolean sameDay = false;

        if (lastDate == null || lastDate.trim().isEmpty()) {
            // First completion ever
            currentStreak = 1;
            longestStreak = Math.max(longestStreak, 1);
            streakIncremented = true;
        } else {
            int dayDiff = calculateDayDifference(lastDate, dateString);

            if (dayDiff == 0) {
                // Same local calendar date -> streak remains unchanged
                sameDay = true;
            } else if (dayDiff == 1) {
                // Consecutive calendar day -> increment streak
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
                streakIncremented = true;
            } else if (dayDiff > 1) {
                // Missed day -> reset current streak to 1
                currentStreak = 1;
                longestStreak = Math.max(longestStreak, currentStreak);
                streakReset = true;
            }
            // If dayDiff < 0 (date set backwards), leave streak as is
        }

        // Save updated state
        preferences.edit()
                .putString(KEY_LAST_COMPLETION_DATE, dateString)
                .putInt(KEY_CURRENT_STREAK, currentStreak)
                .putInt(KEY_LONGEST_STREAK, longestStreak)
                .apply();

        return new StreakResult(currentStreak, longestStreak, streakIncremented, streakReset, sameDay);
    }

    /**
     * Helper to format current date as yyyy-MM-dd.
     */
    public static String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        return sdf.format(new Date());
    }

    /**
     * Calculates the difference in days between two yyyy-MM-dd date strings.
     * Handles month and year boundaries correctly.
     */
    public static int calculateDayDifference(String dateStr1, String dateStr2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d1 = sdf.parse(dateStr1);
            Date d2 = sdf.parse(dateStr2);
            if (d1 == null || d2 == null) return 0;
            long diffMs = d2.getTime() - d1.getTime();
            return (int) (diffMs / (24 * 60 * 60 * 1000L));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Clears streak preferences (useful for testing).
     */
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
