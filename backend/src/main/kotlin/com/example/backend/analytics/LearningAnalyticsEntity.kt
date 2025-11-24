package com.example.backend.analytics

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "learning_analytics")
class LearningAnalyticsEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analytics_id")
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "report_date", nullable = false)
    var reportDate: LocalDate,

    @Column(name = "total_courses")
    var totalCourses: Int? = 0,

    @Column(name = "completed_assignments")
    var completedAssignments: Int? = 0,

    @Column(name = "pending_assignments")
    var pendingAssignments: Int? = 0,

    @Column(name = "overdue_assignments")
    var overdueAssignments: Int? = 0,

    @Column(name = "group_activity_score")
    var groupActivityScore: Int? = 0,

    @Column(columnDefinition = "TEXT")
    var suggestions: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null
)

data class LearningAnalyticsUpsertRequest(
    val userId: Int,
    val reportDate: String,
    val totalCourses: Int? = 0,
    val completedAssignments: Int? = 0,
    val pendingAssignments: Int? = 0,
    val overdueAssignments: Int? = 0,
    val groupActivityScore: Int? = 0,
    val suggestions: String? = null
)
