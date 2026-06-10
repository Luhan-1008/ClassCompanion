package com.example.backend.note

import org.springframework.data.jpa.repository.JpaRepository

interface NoteRepository : JpaRepository<NoteEntity, Int> {
    fun findByUserId(userId: Int): List<NoteEntity>
}
