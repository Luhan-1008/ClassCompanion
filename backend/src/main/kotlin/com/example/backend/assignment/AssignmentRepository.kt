package com.example.backend.assignment

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface AssignmentRepository : JpaRepository<AssignmentEntity, Int> {
    fun findByUserId(userId: Int): List<AssignmentEntity>
    fun findByUserIdAndDueDateBeforeAndStatusNot(userId: Int, dueDate: LocalDateTime, status: String): List<AssignmentEntity>
}
