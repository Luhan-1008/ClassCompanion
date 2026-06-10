package com.example.backend.assist

import org.springframework.data.jpa.repository.JpaRepository

interface AssignmentHelpRepository : JpaRepository<AssignmentHelpEntity, Int> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Int): List<AssignmentHelpEntity>
}

