package com.example.touchgrass.journal;

/**
 * Model class for an Adventure Journal entry recorded upon quest completion.
 */
public class JournalEntry {

    private final String id;
    private final String questId;
    private final String questName;
    private final String category;
    private final int xpEarned;
    private final long timestamp;
    private final String formattedDate;
    private final String unlockedTitle;
    private String reflection;

    public JournalEntry(String id, String questId, String questName, String category,
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

    public String getId() {
        return id;
    }

    public String getQuestId() {
        return questId;
    }

    public String getQuestName() {
        return questName;
    }

    public String getCategory() {
        return category;
    }

    public int getXpEarned() {
        return xpEarned;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String getUnlockedTitle() {
        return unlockedTitle;
    }

    public String getReflection() {
        return reflection;
    }

    public void setReflection(String reflection) {
        this.reflection = reflection;
    }
}
