package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.GroupTask
import com.example.myapplication.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupTaskDao {
    @Query("SELECT * FROM group_tasks WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getTasksByGroup(groupId: Int): Flow<List<GroupTask>>
    
    @Query("SELECT * FROM group_tasks WHERE groupId = :groupId AND assigneeId = :userId ORDER BY createdAt DESC")
    fun getTasksByAssignee(groupId: Int, userId: Int): Flow<List<GroupTask>>
    
    @Query("SELECT * FROM group_tasks WHERE groupId = :groupId AND status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(groupId: Int, status: TaskStatus): Flow<List<GroupTask>>
    
    @Query("SELECT * FROM group_tasks WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: Int): GroupTask?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: GroupTask): Long
    
    @Update
    suspend fun updateTask(task: GroupTask)
    
    @Delete
    suspend fun deleteTask(task: GroupTask)
    
    @Query("UPDATE group_tasks SET status = :status, completedAt = :completedAt WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus, completedAt: Long?)
}

