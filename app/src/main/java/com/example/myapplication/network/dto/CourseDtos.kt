package com.example.myapplication.network.dto

data class RemoteCourseDto(
    val id: Int?,
    val userId: Int,
    val courseName: String,
    val courseCode: String? = null,
    val teacherName: String? = null,
    val location: String? = null,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val startWeek: Int? = 1,
    val endWeek: Int? = 16,
    val reminderEnabled: Boolean? = true,
    val reminderMinutes: Int? = 15,
    val color: String? = "#2196F3"
)

data class CourseCreateRequest(
    val userId: Int,
    val courseName: String,
    val courseCode: String? = null,
    val teacherName: String? = null,
    val location: String? = null,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val startWeek: Int? = 1,
    val endWeek: Int? = 16,
    val reminderEnabled: Boolean? = true,
    val reminderMinutes: Int? = 15,
    val color: String? = "#2196F3"
)
