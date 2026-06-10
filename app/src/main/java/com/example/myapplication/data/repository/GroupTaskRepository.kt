package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.GroupTaskDao
import com.example.myapplication.data.model.GroupTask
import com.example.myapplication.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

class GroupTaskRepository(private val taskDao: GroupTaskDao) {
    fun getTasksByGroup(groupId: Int): Flow<List<GroupTask>> =
        taskDao.getTasksByGroup(groupId)
    
    fun getTasksByAssignee(groupId: Int, userId: Int): Flow<List<GroupTask>> =
        taskDao.getTasksByAssignee(groupId, userId)
    
    fun getTasksByStatus(groupId: Int, status: TaskStatus): Flow<List<GroupTask>> =
        taskDao.getTasksByStatus(groupId, status)
    
    suspend fun getTaskById(taskId: Int): GroupTask? =
        taskDao.getTaskById(taskId)
    
    suspend fun insertTask(task: GroupTask): Long =
        taskDao.insertTask(task)
    
    suspend fun updateTask(task: GroupTask) =
        taskDao.updateTask(task)
    
    suspend fun deleteTask(task: GroupTask) =
        taskDao.deleteTask(task)
    
    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus, completedAt: Long?) =
        taskDao.updateTaskStatus(taskId, status, completedAt)
}

