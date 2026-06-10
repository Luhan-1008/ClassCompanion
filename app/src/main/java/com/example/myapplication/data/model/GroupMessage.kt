package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_messages",
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
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["userId"]),
        Index(value = ["createdAt"])
    ]
)
data class GroupMessage(
    @PrimaryKey(autoGenerate = true)
    val messageId: Int = 0,
    val groupId: Int,
    val userId: Int,
    val content: String,
    val messageType: MessageType = MessageType.TEXT,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MessageType {
    TEXT, // 文本
    IMAGE, // 图片
    FILE // 文件
}

