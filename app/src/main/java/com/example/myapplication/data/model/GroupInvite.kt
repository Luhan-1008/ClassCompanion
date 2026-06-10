package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_invites",
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
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["creatorId"]),
        Index(value = ["inviteCode"], unique = true),
        Index(value = ["expiresAt"])
    ]
)
data class GroupInvite(
    @PrimaryKey(autoGenerate = true)
    val inviteId: Int = 0,
    val groupId: Int,
    val creatorId: Int,
    val inviteCode: String, // 邀请码
    val maxUses: Int? = null, // 最大使用次数，null表示无限制
    val currentUses: Int = 0, // 当前使用次数
    val expiresAt: Long? = null, // 过期时间，null表示永不过期
    val createdAt: Long = System.currentTimeMillis()
)

