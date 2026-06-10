package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.CourseDao
import com.example.myapplication.data.model.Course
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.dto.CourseCreateRequest
import com.example.myapplication.network.dto.RemoteCourseDto
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) {
    private val api = RetrofitClient.api

    fun getCoursesByUser(userId: Int): Flow<List<Course>> = courseDao.getCoursesByUser(userId)
    
    fun getCoursesByDay(userId: Int, dayOfWeek: Int): Flow<List<Course>> = 
        courseDao.getCoursesByDay(userId, dayOfWeek)
    
    suspend fun getCourseById(courseId: Int): Course? = courseDao.getCourseById(courseId)

    suspend fun getCoursesSnapshot(userId: Int): List<Course> = courseDao.getCoursesSnapshot(userId)
    
    suspend fun insertCourse(course: Course): Long {
        val remote = api.createCourse(course.toRemoteRequest())
        return courseDao.insertCourse(remote.toLocalCourse())
    }
    
    suspend fun updateCourse(course: Course) {
        api.updateCourse(course.courseId, course.toRemoteRequest())
        courseDao.updateCourse(course)
    }
    
    suspend fun deleteCourse(course: Course) {
        api.deleteCourse(course.courseId)
        courseDao.deleteCourse(course)
    }
    
    suspend fun deleteCourseById(courseId: Int) {
        api.deleteCourse(courseId)
        courseDao.deleteCourseById(courseId)
    }

    suspend fun deleteAllCourses(userId: Int) {
        val remoteCourses = api.listCourses(userId)
        remoteCourses.forEach { remote ->
            remote.id?.let { api.deleteCourse(it) }
        }
        courseDao.deleteAllCourses(userId)
    }

    suspend fun syncCourses(userId: Int) {
        val remoteCourses = api.listCourses(userId)
        courseDao.deleteAllCourses(userId)
        remoteCourses.forEach { remote ->
            courseDao.insertCourse(remote.toLocalCourse())
        }
    }

    private fun Course.toRemoteRequest(): CourseCreateRequest = CourseCreateRequest(
        userId = userId,
        courseName = courseName,
        courseCode = courseCode,
        teacherName = teacherName,
        location = location,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        startWeek = startWeek,
        endWeek = endWeek,
        reminderEnabled = reminderEnabled,
        reminderMinutes = reminderMinutes,
        color = color
    )

    private fun RemoteCourseDto.toLocalCourse(): Course = Course(
        courseId = id ?: 0,
        userId = userId,
        courseName = courseName,
        courseCode = courseCode,
        teacherName = teacherName,
        location = location,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        startWeek = startWeek ?: 1,
        endWeek = endWeek ?: 16,
        weeks = null,
        reminderEnabled = reminderEnabled ?: true,
        reminderMinutes = reminderMinutes ?: 15,
        color = color ?: "#2196F3",
        textColor = null
    )
}
