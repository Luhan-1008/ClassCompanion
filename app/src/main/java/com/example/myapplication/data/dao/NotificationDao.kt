package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsByUser(userId: Int): Flow<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(userId: Int): Flow<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE notificationId = :notificationId")
    suspend fun getNotificationById(notificationId: Int): Notification?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long
    
    @Update
    suspend fun updateNotification(notification: Notification)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE notificationId = :notificationId")
    suspend fun markAsRead(notificationId: Int)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)
    
    @Delete
    suspend fun deleteNotification(notification: Notification)
}

