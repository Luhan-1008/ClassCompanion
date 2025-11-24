package com.example.backend.analytics

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/analytics")
class LearningAnalyticsController(private val repo: LearningAnalyticsRepository) {
    @PostMapping("/upsert")
    fun upsert(@RequestBody req: LearningAnalyticsUpsertRequest): ResponseEntity<LearningAnalyticsEntity> {
        val date = LocalDate.parse(req.reportDate)
        val existing = repo.findByUserIdAndReportDate(req.userId, date)
        val entity = existing?.apply {
            totalCourses = req.totalCourses
            completedAssignments = req.completedAssignments
            pendingAssignments = req.pendingAssignments
            overdueAssignments = req.overdueAssignments
            groupActivityScore = req.groupActivityScore
            suggestions = req.suggestions
        } ?: LearningAnalyticsEntity(
            userId = req.userId,
            reportDate = date,
            totalCourses = req.totalCourses,
            completedAssignments = req.completedAssignments,
            pendingAssignments = req.pendingAssignments,
            overdueAssignments = req.overdueAssignments,
            groupActivityScore = req.groupActivityScore,
            suggestions = req.suggestions
        )
        return ResponseEntity.ok(repo.save(entity))
    }

    @GetMapping
    fun list(@RequestParam userId: Int): ResponseEntity<List<LearningAnalyticsEntity>> =
        ResponseEntity.ok(repo.findByUserIdOrderByReportDateDesc(userId))
}
