package com.example.backend.note

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notes")
class NoteEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "course_id")
    var courseId: Int? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    var aiSummary: String? = null,

    @Column(name = "file_type", length = 10)
    var fileType: String? = null,

    @Column(name = "file_url", length = 255)
    var fileUrl: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)

data class NoteCreateRequest(
    val userId: Int,
    val courseId: Int? = null,
    val title: String,
    val content: String? = null,
    val aiSummary: String? = null,
    val fileType: String? = null,
    val fileUrl: String? = null
)
