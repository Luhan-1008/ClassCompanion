package com.example.backend.notification

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "type", length = 20, nullable = false)
    var type: String,

    @Column(name = "title", length = 200, nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "related_id")
    var relatedId: Int? = null,

    @Column(name = "is_read")
    var isRead: Boolean? = false,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null
)

data class NotificationCreateRequest(
    val userId: Int,
    val type: String,
    val title: String,
    val content: String? = null,
    val relatedId: Int? = null
)
