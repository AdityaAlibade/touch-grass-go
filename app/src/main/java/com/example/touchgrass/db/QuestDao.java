package com.example.touchgrass.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for Quests in Room.
 */
@Dao
public interface QuestDao {

    @Query("SELECT * FROM quests")
    List<QuestEntity> getAllQuests();

    @Query("SELECT * FROM quests WHERE status = :status")
    List<QuestEntity> getQuestsByStatus(String status);

    @Query("SELECT * FROM quests WHERE id = :id LIMIT 1")
    QuestEntity getQuestById(String id);

    @Query("SELECT id FROM quests WHERE status = 'ACTIVE'")
    List<String> getActiveQuestIds();

    @Query("SELECT id FROM quests WHERE status = 'COMPLETED'")
    List<String> getCompletedQuestIds();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuests(List<QuestEntity> quests);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuest(QuestEntity quest);

    @Update
    void updateQuest(QuestEntity quest);

    @Query("UPDATE quests SET status = :status WHERE id = :id")
    void updateQuestStatus(String id, String status);

    @Query("UPDATE quests SET status = 'COMPLETED', completedTimestamp = :timestamp WHERE id = :id")
    void markQuestCompleted(String id, long timestamp);

    @Query("SELECT COUNT(*) FROM quests")
    int getQuestCount();
}
