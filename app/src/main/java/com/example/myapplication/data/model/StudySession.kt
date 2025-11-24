package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
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
    indices = [
        Index(value = ["userId"]),
        Index(value = ["courseId"]),
        Index(value = ["sessionDate"])
    ]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Int = 0,
    val userId: Int,
    val courseId: Int? = null,
    val sessionType: StudySessionType = StudySessionType.REVIEW,
    val durationMinutes: Int,
    val sessionDate: Long = System.currentTimeMillis(),
    val focusTopic: String? = null,
    val qualityScore: Int = 4
)

enum class StudySessionType {
    PREVIEW,
    REVIEW,
    ASSIGNMENT,
    DISCUSSION,
    EXAM_PREP
}

