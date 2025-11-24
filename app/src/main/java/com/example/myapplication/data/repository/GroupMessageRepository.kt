package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.GroupMessageDao
import com.example.myapplication.data.model.GroupMessage
import kotlinx.coroutines.flow.Flow

class GroupMessageRepository(private val groupMessageDao: GroupMessageDao) {
    fun getMessagesByGroup(groupId: Int): Flow<List<GroupMessage>> =
        groupMessageDao.getMessagesByGroup(groupId)
    
    suspend fun insertMessage(message: GroupMessage): Long =
        groupMessageDao.insertMessage(message)
    
    suspend fun deleteMessage(messageId: Int) =
        groupMessageDao.deleteMessage(messageId)
}

