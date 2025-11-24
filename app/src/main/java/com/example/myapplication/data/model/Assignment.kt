package com.example.myapplication.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assignments",
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
    indices = [Index(value = ["userId"]), Index(value = ["courseId"]), Index(value = ["dueDate"]), Index(value = ["status"])]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true)
    val assignmentId: Int = 0,
    val userId: Int,
    val courseId: Int? = null,
    val title: String,
    val description: String? = null,
    val type: AssignmentType = AssignmentType.HOMEWORK,
    val dueDate: Long, // 时间戳
    val reminderEnabled: Boolean = true,
    val reminderTime: Long? = null, // 时间戳
    val status: AssignmentStatus = AssignmentStatus.NOT_STARTED,
    val priority: Priority = Priority.MEDIUM,
    @ColumnInfo(defaultValue = "0")
    val progress: Int = 0
)

enum class AssignmentType {
    HOMEWORK, // 作业
    EXPERIMENT, // 实验
    OTHER // 其他
}

enum class AssignmentStatus {
    NOT_STARTED, // 未开始
    IN_PROGRESS, // 进行中
    COMPLETED, // 已完成
    OVERDUE // 已逾期
}

enum class Priority {
    LOW, // 低
    MEDIUM, // 中
    HIGH // 高
}

