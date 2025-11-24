package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.GroupAnnouncementDao
import com.example.myapplication.data.model.GroupAnnouncement
import kotlinx.coroutines.flow.Flow

class GroupAnnouncementRepository(private val announcementDao: GroupAnnouncementDao) {
    fun getAnnouncementsByGroup(groupId: Int): Flow<List<GroupAnnouncement>> =
        announcementDao.getAnnouncementsByGroup(groupId)
    
    suspend fun getAnnouncementById(announcementId: Int): GroupAnnouncement? =
        announcementDao.getAnnouncementById(announcementId)
    
    suspend fun insertAnnouncement(announcement: GroupAnnouncement): Long =
        announcementDao.insertAnnouncement(announcement)
    
    suspend fun updateAnnouncement(announcement: GroupAnnouncement) =
        announcementDao.updateAnnouncement(announcement)
    
    suspend fun deleteAnnouncement(announcement: GroupAnnouncement) =
        announcementDao.deleteAnnouncement(announcement)
    
    suspend fun updatePinStatus(announcementId: Int, isPinned: Boolean) =
        announcementDao.updatePinStatus(announcementId, isPinned)
}

