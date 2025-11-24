package com.example.backend.group

import org.springframework.data.jpa.repository.JpaRepository

interface StudyGroupRepository : JpaRepository<StudyGroupEntity, Int> {
    fun findByCreatorId(creatorId: Int): List<StudyGroupEntity>
}

interface GroupMemberRepository : JpaRepository<GroupMemberEntity, Int> {
    fun findByGroupId(groupId: Int): List<GroupMemberEntity>
    fun findByUserId(userId: Int): List<GroupMemberEntity>
}

interface GroupMessageRepository : JpaRepository<GroupMessageEntity, Int> {
    fun findByGroupIdOrderByCreatedAtDesc(groupId: Int): List<GroupMessageEntity>
}
