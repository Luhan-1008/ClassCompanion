package com.example.backend.assist

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignment-help")
class AssignmentHelpController(
    private val repository: AssignmentHelpRepository
) {

    @PostMapping
    fun create(@RequestBody req: AssignmentHelpCreateRequest): ResponseEntity<AssignmentHelpEntity> {
        val entity = AssignmentHelpEntity(
            userId = req.userId,
            assignmentId = req.assignmentId,
            question = req.question,
            aiResponse = req.aiResponse
        )
        return ResponseEntity.ok(repository.save(entity))
    }

    @GetMapping
    fun listByUser(@RequestParam userId: Int): ResponseEntity<List<AssignmentHelpEntity>> =
        ResponseEntity.ok(repository.findByUserIdOrderByCreatedAtDesc(userId))
}

