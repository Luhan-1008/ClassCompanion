package com.example.backend.notification

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<NotificationEntity, Int> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Int): List<NotificationEntity>
}
