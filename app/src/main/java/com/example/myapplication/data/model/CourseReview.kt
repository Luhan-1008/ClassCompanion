package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_reviews",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["userId"])
    ]
)
data class CourseReview(
    @PrimaryKey(autoGenerate = true)
    val reviewId: Int = 0,
    val courseId: Int,
    val userId: Int,
    val rating: Int,
    val difficulty: CourseDifficultyTag = CourseDifficultyTag.MEDIUM,
    val workloadHoursPerWeek: Int = 4,
    val highlightTags: String = "",
    val comment: String,
    val suggestion: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CourseDifficultyTag {
    EASY,
    MEDIUM,
    HARD
}

