package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_resources",
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
data class CourseResource(
    @PrimaryKey(autoGenerate = true)
    val resourceId: Int = 0,
    val courseId: Int,
    val userId: Int,
    val title: String,
    val description: String? = null,
    val resourceUrl: String,
    val resourceType: CourseResourceType = CourseResourceType.NOTE,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CourseResourceType {
    NOTE,
    PPT,
    PRACTICE,
    VIDEO,
    DISCUSSION,
    OTHER
}

