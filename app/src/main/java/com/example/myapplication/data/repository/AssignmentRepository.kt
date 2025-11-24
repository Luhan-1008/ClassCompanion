package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.AssignmentDao
import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.AssignmentStatus
import kotlinx.coroutines.flow.Flow

class AssignmentRepository(private val assignmentDao: AssignmentDao) {
    fun getAssignmentsByUser(userId: Int): Flow<List<Assignment>> = 
        assignmentDao.getAssignmentsByUser(userId)
    
    fun getAssignmentsByStatus(userId: Int, status: AssignmentStatus): Flow<List<Assignment>> = 
        assignmentDao.getAssignmentsByStatus(userId, status)
    
    fun getAssignmentsByCourse(userId: Int, courseId: Int): Flow<List<Assignment>> = 
        assignmentDao.getAssignmentsByCourse(userId, courseId)
    
    fun getUpcomingAssignments(userId: Int, timestamp: Long): Flow<List<Assignment>> = 
        assignmentDao.getUpcomingAssignments(userId, timestamp, AssignmentStatus.COMPLETED)
    
    suspend fun getAssignmentById(assignmentId: Int): Assignment? = 
        assignmentDao.getAssignmentById(assignmentId)
    
    suspend fun insertAssignment(assignment: Assignment): Long = 
        assignmentDao.insertAssignment(assignment)
    
    suspend fun updateAssignment(assignment: Assignment) = 
        assignmentDao.updateAssignment(assignment)
    
    suspend fun deleteAssignment(assignment: Assignment) = 
        assignmentDao.deleteAssignment(assignment)
    
    suspend fun updateAssignmentStatus(assignmentId: Int, status: AssignmentStatus) = 
        assignmentDao.updateAssignmentStatus(assignmentId, status)

    suspend fun updateAssignmentProgress(assignmentId: Int, progress: Int) =
        assignmentDao.updateAssignmentProgress(assignmentId, progress)
}

