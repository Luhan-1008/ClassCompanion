package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.CourseResourceDao
import com.example.myapplication.data.model.CourseResource
import kotlinx.coroutines.flow.Flow

class CourseResourceRepository(private val resourceDao: CourseResourceDao) {
    fun getResourcesByCourse(courseId: Int): Flow<List<CourseResource>> =
        resourceDao.getResourcesByCourse(courseId)

    fun getResourcesByUser(userId: Int): Flow<List<CourseResource>> =
        resourceDao.getResourcesByUser(userId)

    suspend fun insertResource(resource: CourseResource): Long = resourceDao.insertResource(resource)

    suspend fun deleteResource(resource: CourseResource) = resourceDao.deleteResource(resource)
}

