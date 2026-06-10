package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_members",
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
    indices = [Index(value = ["groupId"]), Index(value = ["userId"])]
)
data class GroupMember(
    @PrimaryKey(autoGenerate = true)
    val memberId: Int = 0,
    val groupId: Int,
    val userId: Int,
    val role: MemberRole = MemberRole.MEMBER,
    val status: MemberStatus = MemberStatus.JOINED,
    val invitedBy: Int? = null // 记录邀请人，null表示用户自主申请
)

enum class MemberRole {
    CREATOR, // 创建者
    ADMIN, // 管理员
    MEMBER // 成员
}

enum class MemberStatus {
    PENDING, // 待审核
    JOINED, // 已加入
    LEFT // 已退出
}

