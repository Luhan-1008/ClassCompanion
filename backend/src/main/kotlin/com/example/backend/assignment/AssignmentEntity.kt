package com.example.backend.assignment

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "assignments")
class AssignmentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "course_id")
    var courseId: Int? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    // 表结构为中文枚举，这里用字符串存储类型以避免枚举名不一致
    @Column(name = "type", length = 20)
    var type: String? = null,

    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDateTime,

    @Column(name = "reminder_enabled")
    var reminderEnabled: Boolean? = true,

    @Column(name = "reminder_time")
    var reminderTime: LocalDateTime? = null,

    @Column(name = "status", length = 20)
    var status: String? = null,

    @Column(name = "priority", length = 10)
    var priority: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)

data class AssignmentCreateRequest(
    val userId: Int,
    val courseId: Int? = null,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val dueDate: String,
    val reminderEnabled: Boolean? = true,
    val reminderTime: String? = null,
    val status: String? = null,
    val priority: String? = null
)
