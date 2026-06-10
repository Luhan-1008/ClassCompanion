package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_announcements",
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
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["authorId"]),
        Index(value = ["createdAt"])
    ]
)
data class GroupAnnouncement(
    @PrimaryKey(autoGenerate = true)
    val announcementId: Int = 0,
    val groupId: Int,
    val authorId: Int,
    val title: String,
    val content: String,
    val isPinned: Boolean = false, // 是否置顶
    val createdAt: Long = System.currentTimeMillis()
)

