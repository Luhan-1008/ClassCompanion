package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.GroupMemberDao
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberStatus
import kotlinx.coroutines.flow.Flow

class GroupMemberRepository(private val groupMemberDao: GroupMemberDao) {
    fun getMembersByGroup(groupId: Int, status: MemberStatus = MemberStatus.JOINED): Flow<List<GroupMember>> =
        groupMemberDao.getMembersByGroup(groupId, status)
    
    fun getGroupsByMember(userId: Int, status: MemberStatus = MemberStatus.JOINED): Flow<List<GroupMember>> =
        groupMemberDao.getGroupsByMember(userId, status)
    
    suspend fun getMember(groupId: Int, userId: Int): GroupMember? =
        groupMemberDao.getMember(groupId, userId)
    
    suspend fun insertMember(member: GroupMember): Long =
        groupMemberDao.insertMember(member)
    
    suspend fun updateMember(member: GroupMember) =
        groupMemberDao.updateMember(member)
    
    suspend fun deleteMember(member: GroupMember) =
        groupMemberDao.deleteMember(member)
    
    suspend fun updateMemberStatus(groupId: Int, userId: Int, status: MemberStatus) =
        groupMemberDao.updateMemberStatus(groupId, userId, status)
}

