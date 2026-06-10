package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.GroupAnnouncement
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupAnnouncementDao {
    @Query("SELECT * FROM group_announcements WHERE groupId = :groupId ORDER BY isPinned DESC, createdAt DESC")
    fun getAnnouncementsByGroup(groupId: Int): Flow<List<GroupAnnouncement>>
    
    @Query("SELECT * FROM group_announcements WHERE announcementId = :announcementId")
    suspend fun getAnnouncementById(announcementId: Int): GroupAnnouncement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: GroupAnnouncement): Long
    
    @Update
    suspend fun updateAnnouncement(announcement: GroupAnnouncement)
    
    @Delete
    suspend fun deleteAnnouncement(announcement: GroupAnnouncement)
    
    @Query("UPDATE group_announcements SET isPinned = :isPinned WHERE announcementId = :announcementId")
    suspend fun updatePinStatus(announcementId: Int, isPinned: Boolean)
}

