package com.example.myapplication.network.dto

data class GroupMemberCreateRequest(
    val groupId: Int,
    val userId: Int,
    val role: String? = "成员",
    val status: String? = "待审核"
)

data class GroupMemberResponse(
    val id: Int?,
    val groupId: Int,
    val userId: Int,
    val role: String?,
    val status: String?,
    val joinedAt: String?
)

