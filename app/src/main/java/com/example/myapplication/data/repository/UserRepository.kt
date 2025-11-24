package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.UserDao
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)
    
    suspend fun getUserById(userId: Int): User? = userDao.getUserById(userId)
    
    suspend fun getUserByStudentId(studentId: String): User? = userDao.getUserByStudentId(studentId)
    
    suspend fun login(studentId: String, username: String, password: String): User? = 
        userDao.login(studentId, username, password)
    
    suspend fun register(user: User): Long = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun usernameExists(username: String): Boolean = userDao.usernameExists(username)
    
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
}

