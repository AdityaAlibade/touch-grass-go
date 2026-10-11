package com.example.touchgrass.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.touchgrass.journal.JournalEntry;

/**
 * Room Entity representing an Adventure Journal entry.
 */
@Entity(tableName = "journal_entries")
public class JournalEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String questId;
    private String questName;
    private String category;
    private int xpEarned;
    private long timestamp;
    private String formattedDate;
    private String unlockedTitle;
    private String reflection;

    public JournalEntity(@NonNull String id, String questId, String questName, String category,
                         int xpEarned, long timestamp, String formattedDate,
                         String unlockedTitle, String reflection) {
        this.id = id;
        this.questId = questId;
        this.questName = questName;
        this.category = category;
        this.xpEarned = xpEarned;
        this.timestamp = timestamp;
        this.formattedDate = formattedDate;
        this.unlockedTitle = unlockedTitle;
        this.reflection = reflection;
    }

    public static JournalEntity fromJournalEntry(JournalEntry entry) {
        return new JournalEntity(
                entry.getId(),
                entry.getQuestId(),
                entry.getQuestName(),
                entry.getCategory(),
                entry.getXpEarned(),
                entry.getTimestamp(),
                entry.getFormattedDate(),
                entry.getUnlockedTitle(),
                entry.getReflection()
        );
    }

    public JournalEntry toJournalEntry() {
        return new JournalEntry(
                id,
                questId,
                questName,
                category,
                xpEarned,
                timestamp,
                formattedDate,
                unlockedTitle,
                reflection
        );
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }

    public String getQuestName() { return questName; }
    public void setQuestName(String questName) { this.questName = questName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedDate() { return formattedDate; }
    public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }

    public String getUnlockedTitle() { return unlockedTitle; }
    public void setUnlockedTitle(String unlockedTitle) { this.unlockedTitle = unlockedTitle; }

    public String getReflection() { return reflection; }
    public void setReflection(String reflection) { this.reflection = reflection; }
}
