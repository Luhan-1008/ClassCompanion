package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.CourseDao
import com.example.myapplication.data.model.Course
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) {
    fun getCoursesByUser(userId: Int): Flow<List<Course>> = courseDao.getCoursesByUser(userId)
    
    fun getCoursesByDay(userId: Int, dayOfWeek: Int): Flow<List<Course>> = 
        courseDao.getCoursesByDay(userId, dayOfWeek)
    
    suspend fun getCourseById(courseId: Int): Course? = courseDao.getCourseById(courseId)
    
    suspend fun insertCourse(course: Course): Long = courseDao.insertCourse(course)
    
    suspend fun updateCourse(course: Course) = courseDao.updateCourse(course)
    
    suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course)
}

