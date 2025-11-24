package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND status = :status")
    fun getMembersByGroup(groupId: Int, status: MemberStatus = MemberStatus.JOINED): Flow<List<GroupMember>>
    
    @Query("SELECT * FROM group_members WHERE userId = :userId AND status = :status")
    fun getGroupsByMember(userId: Int, status: MemberStatus = MemberStatus.JOINED): Flow<List<GroupMember>>
    
    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun getMember(groupId: Int, userId: Int): GroupMember?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember): Long
    
    @Update
    suspend fun updateMember(member: GroupMember)
    
    @Delete
    suspend fun deleteMember(member: GroupMember)
    
    @Query("UPDATE group_members SET status = :status WHERE groupId = :groupId AND userId = :userId")
    suspend fun updateMemberStatus(groupId: Int, userId: Int, status: MemberStatus)
}

