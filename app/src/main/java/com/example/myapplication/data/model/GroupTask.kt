package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StudyGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["assigneeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["creatorId"]),
        Index(value = ["assigneeId"]),
        Index(value = ["status"]),
        Index(value = ["dueDate"])
    ]
)
data class GroupTask(
    @PrimaryKey(autoGenerate = true)
    val taskId: Int = 0,
    val groupId: Int,
    val creatorId: Int,
    val assigneeId: Int? = null, // 分配给谁，null表示未分配
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null, // 截止时间
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null // 完成时间
)

enum class TaskStatus {
    TODO,        // 待办
    IN_PROGRESS, // 进行中
    COMPLETED,   // 已完成
    CANCELLED    // 已取消
}

enum class TaskPriority {
    LOW,    // 低
    MEDIUM, // 中
    HIGH    // 高
}

