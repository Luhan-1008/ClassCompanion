package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.CourseReviewDao
import com.example.myapplication.data.model.CourseReview
import kotlinx.coroutines.flow.Flow

class CourseReviewRepository(private val reviewDao: CourseReviewDao) {
    fun getReviewsByCourse(courseId: Int): Flow<List<CourseReview>> =
        reviewDao.getReviewsByCourse(courseId)

    fun getReviewsByUser(userId: Int): Flow<List<CourseReview>> =
        reviewDao.getReviewsByUser(userId)

    suspend fun getTopReviews(courseId: Int, limit: Int): List<CourseReview> =
        reviewDao.getTopReviews(courseId, limit)

    suspend fun getAverageRating(courseId: Int): Double =
        reviewDao.getAverageRating(courseId) ?: 0.0

    suspend fun insertReview(review: CourseReview): Long = reviewDao.insertReview(review)

    suspend fun deleteReview(review: CourseReview) = reviewDao.deleteReview(review)
}

