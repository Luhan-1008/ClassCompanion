package com.example.backend.course

import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<CourseEntity, Int> {
    fun findByUserId(userId: Int): List<CourseEntity>
}
