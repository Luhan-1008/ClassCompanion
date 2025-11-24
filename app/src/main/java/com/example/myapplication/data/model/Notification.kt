package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["isRead"]), Index(value = ["createdAt"])]
)
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val notificationId: Int = 0,
    val userId: Int,
    val type: NotificationType,
    val title: String,
    val content: String? = null,
    val relatedId: Int? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    COURSE_REMINDER, // 课程提醒
    ASSIGNMENT_REMINDER, // 作业提醒
    GROUP_MESSAGE, // 小组消息
    SYSTEM_NOTIFICATION // 系统通知
}

