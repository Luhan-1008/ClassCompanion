package com.example.backend.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val userRepository: UserRepository) {
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<UserProfileResponse> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("用户不存在") }
        return ResponseEntity.ok(user.toProfileResponse())
    }

    @PutMapping("/{id}")
    fun updateById(@PathVariable id: Long, @RequestBody req: UpdateUserProfileRequest): ResponseEntity<UserProfileResponse> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("用户不存在") }
        user.studentId = req.studentId
        user.realName = req.realName
        user.email = req.email
        user.avatarUrl = req.avatarUrl
        val saved = userRepository.save(user)
        return ResponseEntity.ok(saved.toProfileResponse())
    }
}

data class UserProfileResponse(
    val userId: Long,
    val username: String,
    val studentId: String?,
    val realName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class UpdateUserProfileRequest(
    val studentId: String? = null,
    val realName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null
)

private fun UserEntity.toProfileResponse(): UserProfileResponse = UserProfileResponse(
    userId = id ?: 0L,
    username = username,
    studentId = studentId,
    realName = realName,
    email = email,
    avatarUrl = avatarUrl
)
