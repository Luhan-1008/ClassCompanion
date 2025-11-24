package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE userId = :userId ORDER BY dayOfWeek, startTime")
    fun getCoursesByUser(userId: Int): Flow<List<Course>>
    
    @Query("SELECT * FROM courses WHERE userId = :userId AND dayOfWeek = :dayOfWeek ORDER BY startTime")
    fun getCoursesByDay(userId: Int, dayOfWeek: Int): Flow<List<Course>>
    
    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    suspend fun getCourseById(courseId: Int): Course?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long
    
    @Update
    suspend fun updateCourse(course: Course)
    
    @Delete
    suspend fun deleteCourse(course: Course)
    
    @Query("DELETE FROM courses WHERE courseId = :courseId")
    suspend fun deleteCourseById(courseId: Int)
}

