package com.example.backend.group

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "study_groups")
class StudyGroupEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    var id: Int? = null,

    @Column(name = "creator_id", nullable = false)
    var creatorId: Int,

    @Column(name = "group_name", nullable = false, length = 100)
    var groupName: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "course_id")
    var courseId: Int? = null,

    @Column(length = 100)
    var topic: String? = null,

    @Column(name = "max_members")
    var maxMembers: Int? = 20,

    @Column(name = "is_public")
    var isPublic: Boolean? = true,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)

@Entity
@Table(name = "group_members")
class GroupMemberEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    var id: Int? = null,

    @Column(name = "group_id", nullable = false)
    var groupId: Int,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    // 角色/状态使用字符串保存，避免与中文枚举不同步
    @Column(length = 10)
    var role: String? = "成员",

    @Column(name = "joined_at")
    var joinedAt: LocalDateTime? = null,

    @Column(length = 10)
    var status: String? = "已加入"
)

@Entity
@Table(name = "group_messages")
class GroupMessageEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    var id: Int? = null,

    @Column(name = "group_id", nullable = false)
    var groupId: Int,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "message_type", length = 10)
    var messageType: String? = "文本",

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null
)
