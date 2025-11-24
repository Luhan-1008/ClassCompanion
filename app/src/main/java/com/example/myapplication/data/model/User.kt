package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val username: String,
    val password: String,
    val studentId: String? = null,
    val realName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null
)

