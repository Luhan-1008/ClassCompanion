package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Int): User?
    
    @Query("SELECT * FROM users WHERE LOWER(studentId) = LOWER(:studentId) LIMIT 1")
    suspend fun getUserByStudentId(studentId: String): User?
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>
    
    // 模糊查询用户（用于邀请功能）
    @Query("SELECT * FROM users WHERE LOWER(username) LIKE LOWER(:keyword) OR LOWER(studentId) LIKE LOWER(:keyword) LIMIT 10")
    suspend fun searchUsers(keyword: String): List<User>
    
    @Query("SELECT * FROM users WHERE studentId = :studentId AND username = :username AND password = :password LIMIT 1")
    suspend fun login(studentId: String, username: String, password: String): User?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    suspend fun usernameExists(username: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun emailExists(email: String): Boolean
    
    @Delete
    suspend fun deleteUser(user: User)
}
