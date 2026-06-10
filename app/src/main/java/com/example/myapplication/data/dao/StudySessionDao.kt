package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY sessionDate DESC")
    fun getSessionsByUser(userId: Int): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND sessionDate BETWEEN :start AND :end ORDER BY sessionDate DESC")
    suspend fun getSessionsWithin(userId: Int, start: Long, end: Long): List<StudySession>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE userId = :userId AND courseId = :courseId")
    suspend fun getTotalMinutesForCourse(userId: Int, courseId: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Delete
    suspend fun deleteSession(session: StudySession)
}

