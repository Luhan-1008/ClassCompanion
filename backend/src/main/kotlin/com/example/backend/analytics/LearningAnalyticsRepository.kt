package com.example.backend.analytics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface LearningAnalyticsRepository : JpaRepository<LearningAnalyticsEntity, Int> {
    fun findByUserIdAndReportDate(userId: Int, reportDate: LocalDate): LearningAnalyticsEntity?
    fun findByUserIdOrderByReportDateDesc(userId: Int): List<LearningAnalyticsEntity>
}
