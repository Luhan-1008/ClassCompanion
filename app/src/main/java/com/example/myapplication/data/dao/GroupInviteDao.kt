package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.GroupInvite
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupInviteDao {
    @Query("SELECT * FROM group_invites WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getInvitesByGroup(groupId: Int): Flow<List<GroupInvite>>
    
    @Query("SELECT * FROM group_invites WHERE inviteCode = :inviteCode")
    suspend fun getInviteByCode(inviteCode: String): GroupInvite?
    
    @Query("SELECT * FROM group_invites WHERE inviteId = :inviteId")
    suspend fun getInviteById(inviteId: Int): GroupInvite?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(invite: GroupInvite): Long
    
    @Update
    suspend fun updateInvite(invite: GroupInvite)
    
    @Delete
    suspend fun deleteInvite(invite: GroupInvite)
    
    @Query("UPDATE group_invites SET currentUses = currentUses + 1 WHERE inviteId = :inviteId")
    suspend fun incrementUseCount(inviteId: Int)
}

