package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_groups",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Assignment::class,
            parentColumns = ["assignmentId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["creatorId"]),
        Index(value = ["courseId"]),
        Index(value = ["taskId"])
    ]
)
data class StudyGroup(
    @PrimaryKey(autoGenerate = true)
    val groupId: Int = 0,
    val creatorId: Int,
    val groupName: String,
    val description: String? = null,
    val courseId: Int? = null,
    val taskId: Int? = null,
    val topic: String? = null,
    val maxMembers: Int = 20,
    val isPublic: Boolean = true
)

