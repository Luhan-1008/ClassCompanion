package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.GroupInviteDao
import com.example.myapplication.data.model.GroupInvite
import kotlinx.coroutines.flow.Flow

class GroupInviteRepository(private val inviteDao: GroupInviteDao) {
    fun getInvitesByGroup(groupId: Int): Flow<List<GroupInvite>> =
        inviteDao.getInvitesByGroup(groupId)
    
    suspend fun getInviteByCode(inviteCode: String): GroupInvite? =
        inviteDao.getInviteByCode(inviteCode)
    
    suspend fun getInviteById(inviteId: Int): GroupInvite? =
        inviteDao.getInviteById(inviteId)
    
    suspend fun insertInvite(invite: GroupInvite): Long =
        inviteDao.insertInvite(invite)
    
    suspend fun updateInvite(invite: GroupInvite) =
        inviteDao.updateInvite(invite)
    
    suspend fun deleteInvite(invite: GroupInvite) =
        inviteDao.deleteInvite(invite)
    
    suspend fun incrementUseCount(inviteId: Int) =
        inviteDao.incrementUseCount(inviteId)
}

