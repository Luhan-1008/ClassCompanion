package com.example.myapplication.network.dto

data class GroupInviteCreateRequest(
    val groupId: Int,
    val creatorId: Int,
    val inviteCode: String,
    val maxUses: Int? = null,
    val expiresAt: Long? = null
)

data class GroupInviteResponse(
    val id: Int?,
    val groupId: Int,
    val creatorId: Int,
    val inviteCode: String,
    val maxUses: Int?,
    val currentUses: Int,
    val expiresAt: Long?,
    val createdAt: String?
)

