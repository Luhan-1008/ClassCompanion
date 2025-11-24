package com.example.backend.user

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, length = 255)
    var password: String,

    @Column(name = "student_id", unique = true)
    var studentId: String? = null,

    var realName: String? = null,
    var email: String? = null,
    var avatarUrl: String? = null
)
