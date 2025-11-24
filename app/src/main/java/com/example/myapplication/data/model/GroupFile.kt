package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_files",
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
            childColumns = ["uploaderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["uploaderId"]),
        Index(value = ["createdAt"])
    ]
)
data class GroupFile(
    @PrimaryKey(autoGenerate = true)
    val fileId: Int = 0,
    val groupId: Int,
    val uploaderId: Int,
    val fileName: String,
    val filePath: String, // 本地文件路径或URL
    val fileType: FileType = FileType.OTHER,
    val fileSize: Long = 0, // 文件大小（字节）
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

