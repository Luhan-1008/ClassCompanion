package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.AssignmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE userId = :userId ORDER BY dueDate ASC")
    fun getAssignmentsByUser(userId: Int): Flow<List<Assignment>>
    
    @Query("SELECT * FROM assignments WHERE userId = :userId AND status = :status ORDER BY dueDate ASC")
    fun getAssignmentsByStatus(userId: Int, status: AssignmentStatus): Flow<List<Assignment>>
    
    @Query("SELECT * FROM assignments WHERE userId = :userId AND courseId = :courseId ORDER BY dueDate ASC")
    fun getAssignmentsByCourse(userId: Int, courseId: Int): Flow<List<Assignment>>
    
    @Query("SELECT * FROM assignments WHERE assignmentId = :assignmentId")
    suspend fun getAssignmentById(assignmentId: Int): Assignment?
    
    @Query("SELECT * FROM assignments WHERE userId = :userId AND dueDate <= :timestamp AND status != :completedStatus ORDER BY dueDate ASC")
    fun getUpcomingAssignments(userId: Int, timestamp: Long, completedStatus: AssignmentStatus): Flow<List<Assignment>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long
    
    @Update
    suspend fun updateAssignment(assignment: Assignment)
    
    @Delete
    suspend fun deleteAssignment(assignment: Assignment)
    
    @Query("UPDATE assignments SET status = :status WHERE assignmentId = :assignmentId")
    suspend fun updateAssignmentStatus(assignmentId: Int, status: AssignmentStatus)

    @Query("UPDATE assignments SET progress = :progress WHERE assignmentId = :assignmentId")
    suspend fun updateAssignmentProgress(assignmentId: Int, progress: Int)
}

