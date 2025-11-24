package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.GroupMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMessageDao {
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY createdAt ASC")
    fun getMessagesByGroup(groupId: Int): Flow<List<GroupMessage>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: GroupMessage): Long
    
    @Query("DELETE FROM group_messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: Int)
}

