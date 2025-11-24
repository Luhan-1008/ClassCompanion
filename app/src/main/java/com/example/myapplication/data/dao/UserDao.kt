package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Int): User?
    
    @Query("SELECT * FROM users WHERE studentId = :studentId LIMIT 1")
    suspend fun getUserByStudentId(studentId: String): User?
    
    @Query("SELECT * FROM users WHERE studentId = :studentId AND username = :username AND password = :password LIMIT 1")
    suspend fun login(studentId: String, username: String, password: String): User?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    suspend fun usernameExists(username: String): Boolean
    
    @Delete
    suspend fun deleteUser(user: User)
}

