package com.example.touchgrass.journal;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages adventure journal persistence using SharedPreferences.
 * Uses pure Java string serialization for 100% compatibility across Android and JVM unit tests.
 */
public class JournalManager {

    private static final String PREFS_NAME = "touch_grass_journal_prefs";
    private static final String KEY_JOURNAL_RAW_DATA = "journal_raw_data";
    private static final String ENTRY_DELIMITER = "___ENTRY_SEP___";
    private static final String FIELD_DELIMITER = "___FIELD_SEP___";

    private static JournalManager instance;
    private final SharedPreferences preferences;

    public JournalManager(Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    public JournalManager(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static synchronized JournalManager getInstance(Context context) {
        if (instance == null) {
            instance = new JournalManager(context);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    /**
     * Retrieves all journal entries sorted chronologically (newest first).
     */
    public List<JournalEntry> getAllJournalEntries() {
        List<JournalEntry> entries = new ArrayList<>();
        String rawData = preferences.getString(KEY_JOURNAL_RAW_DATA, "");

        if (rawData == null || rawData.trim().isEmpty()) {
            return entries;
        }

        String[] entryStrings = rawData.split(ENTRY_DELIMITER);
        for (int i = entryStrings.length - 1; i >= 0; i--) {
            String str = entryStrings[i];
            if (str != null && !str.trim().isEmpty()) {
                JournalEntry entry = deserializeEntry(str);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    /**
     * Records a new journal entry upon quest completion. Prevents duplicate entries for the same quest.
     */
    public boolean addJournalEntry(JournalEntry entry) {
        if (entry == null || entry.getQuestId() == null) return false;

        if (hasJournalEntryForQuest(entry.getQuestId())) {
            return false; // Already recorded
        }

        String rawData = preferences.getString(KEY_JOURNAL_RAW_DATA, "");
        String serialized = serializeEntry(entry);

        if (rawData != null && !rawData.isEmpty()) {
            rawData = rawData + ENTRY_DELIMITER + serialized;
        } else {
            rawData = serialized;
        }

        preferences.edit().putString(KEY_JOURNAL_RAW_DATA, rawData).apply();
        return true;
    }

    /**
     * Updates optional personal reflection note for an existing quest entry.
     */
    public boolean updateReflection(String questId, String reflection) {
        if (questId == null) return false;
        List<JournalEntry> currentList = getAllJournalEntries();
        boolean updated = false;

        // Reconstruct entries with updated reflection (chronological order)
        List<JournalEntry> updatedList = new ArrayList<>();
        for (int i = currentList.size() - 1; i >= 0; i--) {
            JournalEntry e = currentList.get(i);
            if (questId.equals(e.getQuestId())) {
                e.setReflection(reflection != null ? reflection : "");
                updated = true;
            }
            updatedList.add(e);
        }

        if (updated) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < updatedList.size(); i++) {
                if (i > 0) sb.append(ENTRY_DELIMITER);
                sb.append(serializeEntry(updatedList.get(i)));
            }
            preferences.edit().putString(KEY_JOURNAL_RAW_DATA, sb.toString()).apply();
        }

        return updated;
    }

    public boolean hasJournalEntryForQuest(String questId) {
        for (JournalEntry entry : getAllJournalEntries()) {
            if (questId.equals(entry.getQuestId())) {
                return true;
            }
        }
        return false;
    }

    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.ROOT);
        return sdf.format(new Date(timestamp));
    }

    private String serializeEntry(JournalEntry e) {
        return sanitize(e.getId()) + FIELD_DELIMITER +
                sanitize(e.getQuestId()) + FIELD_DELIMITER +
                sanitize(e.getQuestName()) + FIELD_DELIMITER +
                sanitize(e.getCategory()) + FIELD_DELIMITER +
                e.getXpEarned() + FIELD_DELIMITER +
                e.getTimestamp() + FIELD_DELIMITER +
                sanitize(e.getFormattedDate()) + FIELD_DELIMITER +
                sanitize(e.getUnlockedTitle()) + FIELD_DELIMITER +
                sanitize(e.getReflection());
    }

    private JournalEntry deserializeEntry(String str) {
        try {
            String[] parts = str.split(FIELD_DELIMITER, -1);
            if (parts.length < 9) return null;

            String id = desanitize(parts[0]);
            String questId = desanitize(parts[1]);
            String questName = desanitize(parts[2]);
            String category = desanitize(parts[3]);
            int xpEarned = Integer.parseInt(parts[4]);
            long timestamp = Long.parseLong(parts[5]);
            String formattedDate = desanitize(parts[6]);
            String unlockedTitle = desanitize(parts[7]);
            String reflection = desanitize(parts[8]);

            return new JournalEntry(id, questId, questName, category, xpEarned, timestamp, formattedDate, unlockedTitle, reflection);
        } catch (Exception e) {
            return null;
        }
    }

    private String sanitize(String str) {
        if (str == null) return "";
        return str.replace(ENTRY_DELIMITER, "").replace(FIELD_DELIMITER, "");
    }

    private String desanitize(String str) {
        return str != null ? str : "";
    }

    /**
     * Clears journal preferences (useful for testing).
     */
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
