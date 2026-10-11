package com.example.touchgrass.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for Journal Entries in Room.
 */
@Dao
public interface JournalDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    List<JournalEntity> getAllEntries();

    @Query("SELECT * FROM journal_entries WHERE questId = :questId LIMIT 1")
    JournalEntity getEntryByQuestId(String questId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertEntry(JournalEntity entry);

    @Query("UPDATE journal_entries SET reflection = :reflection WHERE questId = :questId")
    void updateReflection(String questId, String reflection);

    @Query("SELECT COUNT(*) FROM journal_entries")
    int getEntryCount();
}
