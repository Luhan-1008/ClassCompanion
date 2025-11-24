package com.example.myapplication.session

object CurrentSession {
    @Volatile
    var token: String? = null

    @Volatile
    var userId: Long? = null

    val userIdInt: Int?
        get() = userId?.toInt()
}
