package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.StudyGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyGroupDao {
    @Query("SELECT * FROM study_groups WHERE creatorId = :userId OR groupId IN (SELECT groupId FROM group_members WHERE userId = :userId AND status = 'JOINED')")
    fun getGroupsByUser(userId: Int): Flow<List<StudyGroup>>
    
    @Query("SELECT * FROM study_groups WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: Int): StudyGroup?
    
    @Query("SELECT * FROM study_groups WHERE isPublic = 1 AND (:courseId IS NULL OR courseId = :courseId) AND (groupName LIKE :query OR topic LIKE :query OR description LIKE :query)")
    fun searchPublicGroups(courseId: Int?, query: String): Flow<List<StudyGroup>>
    
    @Query("SELECT * FROM study_groups WHERE courseId = :courseId")
    fun getGroupsByCourse(courseId: Int): Flow<List<StudyGroup>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StudyGroup): Long
    
    @Update
    suspend fun updateGroup(group: StudyGroup)
    
    @Delete
    suspend fun deleteGroup(group: StudyGroup)
}

