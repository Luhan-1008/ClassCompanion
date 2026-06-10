package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.StudyGroupDao
import com.example.myapplication.data.model.StudyGroup
import kotlinx.coroutines.flow.Flow

class StudyGroupRepository(private val studyGroupDao: StudyGroupDao) {
    fun getGroupsByUser(userId: Int): Flow<List<StudyGroup>> = 
        studyGroupDao.getGroupsByUser(userId)
    
    suspend fun getGroupById(groupId: Int): StudyGroup? = studyGroupDao.getGroupById(groupId)
    
    fun searchPublicGroups(courseId: Int?, query: String): Flow<List<StudyGroup>> = 
        studyGroupDao.searchPublicGroups(courseId, if (query.isBlank()) "%%" else "%$query%")
    
    fun getGroupsByCourse(courseId: Int): Flow<List<StudyGroup>> = 
        studyGroupDao.getGroupsByCourse(courseId)
    
    suspend fun insertGroup(group: StudyGroup): Long = studyGroupDao.insertGroup(group)
    
    suspend fun updateGroup(group: StudyGroup) = studyGroupDao.updateGroup(group)
    
    suspend fun deleteGroup(group: StudyGroup) = studyGroupDao.deleteGroup(group)
}

