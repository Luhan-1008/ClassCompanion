package com.example.backend.course

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/courses")
class CourseController(private val repo: CourseRepository) {
    @PostMapping
    fun create(@RequestBody req: CourseCreateRequest): ResponseEntity<CourseEntity> {
        val saved = repo.save(
            CourseEntity(
                userId = req.userId,
                courseName = req.courseName,
                courseCode = req.courseCode,
                teacherName = req.teacherName,
                location = req.location,
                dayOfWeek = req.dayOfWeek,
                startTime = req.startTime,
                endTime = req.endTime,
                startWeek = req.startWeek,
                endWeek = req.endWeek,
                reminderEnabled = req.reminderEnabled,
                reminderMinutes = req.reminderMinutes,
                color = req.color
            )
        )
        return ResponseEntity.ok(saved)
    }

    @GetMapping
    fun listByUser(@RequestParam userId: Int): ResponseEntity<List<CourseEntity>> =
        ResponseEntity.ok(repo.findByUserId(userId))
}
