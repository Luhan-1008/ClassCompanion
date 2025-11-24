package com.example.backend.notification

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val repo: NotificationRepository) {
    @PostMapping
    fun create(@RequestBody req: NotificationCreateRequest): ResponseEntity<NotificationEntity> =
        ResponseEntity.ok(
            repo.save(
                NotificationEntity(
                    userId = req.userId,
                    type = req.type,
                    title = req.title,
                    content = req.content,
                    relatedId = req.relatedId
                )
            )
        )

    @GetMapping
    fun listByUser(@RequestParam userId: Int): ResponseEntity<List<NotificationEntity>> =
        ResponseEntity.ok(repo.findByUserIdOrderByCreatedAtDesc(userId))
}
