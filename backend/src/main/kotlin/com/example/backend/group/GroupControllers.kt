package com.example.backend.group

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class StudyGroupCreateRequest(
    val creatorId: Int,
    val groupName: String,
    val description: String? = null,
    val courseId: Int? = null,
    val topic: String? = null,
    val maxMembers: Int? = 20,
    val isPublic: Boolean? = true
)

data class GroupMemberCreateRequest(
    val groupId: Int,
    val userId: Int,
    val role: String? = "成员",
    val status: String? = "已加入"
)

data class GroupMessageCreateRequest(
    val groupId: Int,
    val userId: Int,
    val content: String,
    val messageType: String? = "文本"
)

@RestController
@RequestMapping("/api/groups")
class StudyGroupController(
    private val groupRepo: StudyGroupRepository,
    private val memberRepo: GroupMemberRepository,
    private val messageRepo: GroupMessageRepository
) {
    @PostMapping
    fun createGroup(@RequestBody req: StudyGroupCreateRequest): ResponseEntity<StudyGroupEntity> {
        val saved = groupRepo.save(
            StudyGroupEntity(
                creatorId = req.creatorId,
                groupName = req.groupName,
                description = req.description,
                courseId = req.courseId,
                topic = req.topic,
                maxMembers = req.maxMembers,
                isPublic = req.isPublic
            )
        )
        return ResponseEntity.ok(saved)
    }

    @GetMapping
    fun listByCreator(@RequestParam creatorId: Int): ResponseEntity<List<StudyGroupEntity>> =
        ResponseEntity.ok(groupRepo.findByCreatorId(creatorId))

    @PostMapping("/members")
    fun addMember(@RequestBody req: GroupMemberCreateRequest): ResponseEntity<GroupMemberEntity> =
        ResponseEntity.ok(
            memberRepo.save(
                GroupMemberEntity(
                    groupId = req.groupId,
                    userId = req.userId,
                    role = req.role,
                    status = req.status
                )
            )
        )

    @GetMapping("/members")
    fun listMembers(@RequestParam groupId: Int): ResponseEntity<List<GroupMemberEntity>> =
        ResponseEntity.ok(memberRepo.findByGroupId(groupId))

    @PostMapping("/messages")
    fun addMessage(@RequestBody req: GroupMessageCreateRequest): ResponseEntity<GroupMessageEntity> =
        ResponseEntity.ok(
            messageRepo.save(
                GroupMessageEntity(
                    groupId = req.groupId,
                    userId = req.userId,
                    content = req.content,
                    messageType = req.messageType
                )
            )
        )

    @GetMapping("/messages")
    fun listMessages(@RequestParam groupId: Int): ResponseEntity<List<GroupMessageEntity>> =
        ResponseEntity.ok(messageRepo.findByGroupIdOrderByCreatedAtDesc(groupId))
}
