package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.StudySessionDao
import com.example.myapplication.data.model.StudySession
import kotlinx.coroutines.flow.Flow

class StudySessionRepository(private val dao: StudySessionDao) {
    fun getSessionsByUser(userId: Int): Flow<List<StudySession>> = dao.getSessionsByUser(userId)

    suspend fun getSessionsWithin(userId: Int, start: Long, end: Long): List<StudySession> =
        dao.getSessionsWithin(userId, start, end)

    suspend fun getTotalMinutesForCourse(userId: Int, courseId: Int): Int =
        dao.getTotalMinutesForCourse(userId, courseId) ?: 0

    suspend fun insertSession(session: StudySession): Long = dao.insertSession(session)

    suspend fun deleteSession(session: StudySession) = dao.deleteSession(session)
}

