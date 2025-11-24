package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.CourseResource
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseResourceDao {
    @Query("SELECT * FROM course_resources WHERE courseId = :courseId ORDER BY createdAt DESC")
    fun getResourcesByCourse(courseId: Int): Flow<List<CourseResource>>

    @Query("SELECT * FROM course_resources WHERE userId = :userId ORDER BY createdAt DESC")
    fun getResourcesByUser(userId: Int): Flow<List<CourseResource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: CourseResource): Long

    @Delete
    suspend fun deleteResource(resource: CourseResource)
}

