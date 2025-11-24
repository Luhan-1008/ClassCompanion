package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["dayOfWeek"])]
)
data class Course(
    @PrimaryKey(autoGenerate = true)
    val courseId: Int = 0,
    val userId: Int,
    val courseName: String,
    val courseCode: String? = null,
    val teacherName: String? = null,
    val location: String? = null,
    val dayOfWeek: Int, // 1-7, 1为周一
    val startTime: String, // HH:mm格式
    val endTime: String, // HH:mm格式
    val startWeek: Int = 1,
    val endWeek: Int = 16,
    val reminderEnabled: Boolean = true,
    val reminderMinutes: Int = 15,
    val color: String = "#2196F3"
)

