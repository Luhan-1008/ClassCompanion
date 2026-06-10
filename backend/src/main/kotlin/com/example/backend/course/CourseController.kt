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

    @PutMapping("/{courseId}")
    fun update(@PathVariable courseId: Int, @RequestBody req: CourseCreateRequest): ResponseEntity<CourseEntity> {
        val existing = repo.findById(courseId).orElseThrow { RuntimeException("课程不存在") }
        existing.userId = req.userId
        existing.courseName = req.courseName
        existing.courseCode = req.courseCode
        existing.teacherName = req.teacherName
        existing.location = req.location
        existing.dayOfWeek = req.dayOfWeek
        existing.startTime = req.startTime
        existing.endTime = req.endTime
        existing.startWeek = req.startWeek
        existing.endWeek = req.endWeek
        existing.reminderEnabled = req.reminderEnabled
        existing.reminderMinutes = req.reminderMinutes
        existing.color = req.color
        return ResponseEntity.ok(repo.save(existing))
    }

    @DeleteMapping("/{courseId}")
    fun delete(@PathVariable courseId: Int): ResponseEntity<Void> {
        if (!repo.existsById(courseId)) {
            throw RuntimeException("课程不存在")
        }
        repo.deleteById(courseId)
        return ResponseEntity.noContent().build()
    }
}
