package com.example.myapplication.util

import java.util.regex.Pattern

object ValidationUtils {
    // 密码强度需求：长度 8-16 位，包含大写字母、小写字母、数字、特殊符号（!@#$%^&*()_+-）中的至少 3 类
    fun isValidPassword(password: String): Boolean {
        if (password.length !in 8..16) {
            return false
        }

        var categories = 0
        if (password.any { it.isUpperCase() }) categories++
        if (password.any { it.isLowerCase() }) categories++
        if (password.any { it.isDigit() }) categories++
        if (password.any { "!@#$%^&*()_+-".contains(it) }) categories++

        return categories >= 3
    }

    fun getPasswordError(password: String): String? {
        if (password.isEmpty()) return null
        if (password.length !in 8..16) {
            return "密码长度需为 8-16 位"
        }
        var categories = 0
        if (password.any { it.isUpperCase() }) categories++
        if (password.any { it.isLowerCase() }) categories++
        if (password.any { it.isDigit() }) categories++
        if (password.any { "!@#$%^&*()_+-".contains(it) }) categories++

        if (categories < 3) {
            return "需包含大写、小写、数字、特殊符号中的至少 3 类"
        }
        return null
    }

    // 邮箱格式验证
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true // 允许为空，如果不必填
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        return emailPattern.matcher(email).matches()
    }
}
