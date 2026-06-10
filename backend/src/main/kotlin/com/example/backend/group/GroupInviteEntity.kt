package com.example.backend.group

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "group_invites")
class GroupInviteEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invite_id")
    var id: Int? = null,

    @Column(name = "group_id", nullable = false)
    var groupId: Int,

    @Column(name = "creator_id", nullable = false)
    var creatorId: Int,

    @Column(name = "invite_code", nullable = false, unique = true, length = 6)
    var inviteCode: String,

    @Column(name = "max_uses")
    var maxUses: Int? = null,

    @Column(name = "current_uses")
    var currentUses: Int = 0,

    @Column(name = "expires_at")
    var expiresAt: Long? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null
)

