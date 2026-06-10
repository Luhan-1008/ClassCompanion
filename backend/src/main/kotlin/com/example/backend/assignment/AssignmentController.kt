package com.example.backend.assignment

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/assignments")
class AssignmentController(private val repo: AssignmentRepository) {
    @PostMapping
    fun create(@RequestBody req: AssignmentCreateRequest): ResponseEntity<AssignmentEntity> {
        val entity = AssignmentEntity(
            userId = req.userId,
            courseId = req.courseId,
            title = req.title,
            description = req.description,
            type = req.type,
            dueDate = LocalDateTime.parse(req.dueDate),
            reminderEnabled = req.reminderEnabled,
            reminderTime = req.reminderTime?.let { LocalDateTime.parse(it) },
            status = req.status,
            priority = req.priority
        )
        return ResponseEntity.ok(repo.save(entity))
    }

    @GetMapping
    fun listByUser(@RequestParam userId: Int): ResponseEntity<List<AssignmentEntity>> =
        ResponseEntity.ok(repo.findByUserId(userId))

    @GetMapping("/upcoming")
    fun upcoming(
        @RequestParam userId: Int,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) before: LocalDateTime,
        @RequestParam excludeStatus: String = "ÒÑÍê³É"
    ): ResponseEntity<List<AssignmentEntity>> =
        ResponseEntity.ok(repo.findByUserIdAndDueDateBeforeAndStatusNot(userId, before, excludeStatus))
}
