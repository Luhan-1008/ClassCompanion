package com.example.backend.note

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notes")
class NoteController(private val repo: NoteRepository) {
    @PostMapping
    fun create(@RequestBody req: NoteCreateRequest): ResponseEntity<NoteEntity> =
        ResponseEntity.ok(
            repo.save(
                NoteEntity(
                    userId = req.userId,
                    courseId = req.courseId,
                    title = req.title,
                    content = req.content,
                    aiSummary = req.aiSummary,
                    fileType = req.fileType,
                    fileUrl = req.fileUrl
                )
            )
        )

    @GetMapping
    fun listByUser(@RequestParam userId: Int): ResponseEntity<List<NoteEntity>> =
        ResponseEntity.ok(repo.findByUserId(userId))
}
