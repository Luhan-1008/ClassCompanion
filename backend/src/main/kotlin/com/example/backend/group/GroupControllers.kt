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
    val role: String? = "��Ա",
    val status: String? = "�Ѽ���"
)

data class GroupMessageCreateRequest(
    val groupId: Int,
    val userId: Int,
    val content: String,
    val messageType: String? = "�ı�"
)

data class GroupInviteCreateRequest(
    val groupId: Int,
    val creatorId: Int,
    val inviteCode: String,
    val maxUses: Int? = null,
    val expiresAt: Long? = null
)

@RestController
@RequestMapping("/api/groups")
class StudyGroupController(
    private val groupRepo: StudyGroupRepository,
    private val memberRepo: GroupMemberRepository,
    private val messageRepo: GroupMessageRepository,
    private val inviteRepo: GroupInviteRepository
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

    // 邀请码相关API
    @PostMapping("/invites")
    fun createInvite(@RequestBody req: GroupInviteCreateRequest): ResponseEntity<GroupInviteEntity> {
        println("创建邀请码请求: groupId=${req.groupId}, creatorId=${req.creatorId}, inviteCode=${req.inviteCode}")
        val invite = GroupInviteEntity(
            groupId = req.groupId,
            creatorId = req.creatorId,
            inviteCode = req.inviteCode,
            maxUses = req.maxUses,
            expiresAt = req.expiresAt,
            currentUses = 0,
            createdAt = java.time.LocalDateTime.now()
        )
        val saved = inviteRepo.save(invite)
        println("邀请码创建成功: id=${saved.id}, inviteCode=${saved.inviteCode}")
        return ResponseEntity.ok(saved)
    }

    @GetMapping("/invites")
    fun getInviteByCode(@RequestParam inviteCode: String): ResponseEntity<GroupInviteEntity?> {
        println("查询邀请码: $inviteCode")
        val invite = inviteRepo.findByInviteCode(inviteCode)
        return if (invite != null) {
            println("找到邀请码: id=${invite.id}, groupId=${invite.groupId}")
            ResponseEntity.ok(invite)
        } else {
            println("邀请码不存在: $inviteCode")
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/invites/group")
    fun listInvitesByGroup(@RequestParam groupId: Int): ResponseEntity<List<GroupInviteEntity>> =
        ResponseEntity.ok(inviteRepo.findByGroupId(groupId))

    @PostMapping("/invites/{inviteId}/increment")
    fun incrementUseCount(@PathVariable inviteId: Int): ResponseEntity<GroupInviteEntity> {
        val invite = inviteRepo.findById(inviteId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        invite.currentUses = invite.currentUses + 1
        return ResponseEntity.ok(inviteRepo.save(invite))
    }
}
