package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.CourseReview
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseReviewDao {
    @Query("SELECT * FROM course_reviews WHERE courseId = :courseId ORDER BY createdAt DESC")
    fun getReviewsByCourse(courseId: Int): Flow<List<CourseReview>>

    @Query("SELECT * FROM course_reviews WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReviewsByUser(userId: Int): Flow<List<CourseReview>>

    @Query("SELECT * FROM course_reviews WHERE courseId = :courseId ORDER BY rating DESC LIMIT :limit")
    suspend fun getTopReviews(courseId: Int, limit: Int): List<CourseReview>

    @Query("SELECT AVG(rating) FROM course_reviews WHERE courseId = :courseId")
    suspend fun getAverageRating(courseId: Int): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CourseReview): Long

    @Delete
    suspend fun deleteReview(review: CourseReview)
}

