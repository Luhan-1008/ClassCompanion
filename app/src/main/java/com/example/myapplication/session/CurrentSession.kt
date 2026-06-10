package com.example.myapplication.session

// 内存里的当前会话状态 - App 当前运行时临时用
object CurrentSession {
    @Volatile
    var token: String? = null

    @Volatile
    var userId: Long? = null

    val userIdInt: Int?
        get() = userId?.toInt()
}
