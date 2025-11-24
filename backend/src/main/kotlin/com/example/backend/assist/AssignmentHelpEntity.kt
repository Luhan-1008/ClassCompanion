package com.example.backend.assist

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "assignment_help")
class AssignmentHelpEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "help_id")
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "assignment_id")
    var assignmentId: Int? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    var question: String,

    @Column(name = "ai_response", columnDefinition = "TEXT")
    var aiResponse: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null
)

data class AssignmentHelpCreateRequest(
    val userId: Int,
    val assignmentId: Int? = null,
    val question: String,
    val aiResponse: String? = null
)
