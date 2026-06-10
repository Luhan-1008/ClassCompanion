package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["courseId"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val noteId: Int = 0,
    val userId: Int,
    val courseId: Int? = null,
    val title: String,
    val content: String? = null,
    val aiSummary: String? = null,
    val fileType: FileType? = null,
    val fileUrl: String? = null
)

