package com.example.backend.course

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "courses")
class CourseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "course_name", nullable = false, length = 100)
    var courseName: String,

    @Column(name = "course_code", length = 20)
    var courseCode: String? = null,

    @Column(name = "teacher_name", length = 50)
    var teacherName: String? = null,

    @Column(name = "location", length = 100)
    var location: String? = null,

    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: Int,

    @Column(name = "start_time", nullable = false)
    var startTime: String,

    @Column(name = "end_time", nullable = false)
    var endTime: String,

    @Column(name = "start_week")
    var startWeek: Int? = 1,

    @Column(name = "end_week")
    var endWeek: Int? = 16,

    @Column(name = "reminder_enabled")
    var reminderEnabled: Boolean? = true,

    @Column(name = "reminder_minutes")
    var reminderMinutes: Int? = 15,

    @Column(name = "color", length = 7)
    var color: String? = "#2196F3",

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
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
