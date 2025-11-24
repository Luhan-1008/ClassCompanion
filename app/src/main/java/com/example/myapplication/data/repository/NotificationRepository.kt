package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.NotificationDao
import com.example.myapplication.data.model.Notification
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {
    fun getNotificationsByUser(userId: Int): Flow<List<Notification>> =
        notificationDao.getNotificationsByUser(userId)
    
    fun getUnreadNotifications(userId: Int): Flow<List<Notification>> =
        notificationDao.getUnreadNotifications(userId)
    
    suspend fun getNotificationById(notificationId: Int): Notification? =
        notificationDao.getNotificationById(notificationId)
    
    suspend fun insertNotification(notification: Notification): Long =
        notificationDao.insertNotification(notification)
    
    suspend fun updateNotification(notification: Notification) =
        notificationDao.updateNotification(notification)
    
    suspend fun markAsRead(notificationId: Int) =
        notificationDao.markAsRead(notificationId)
    
    suspend fun markAllAsRead(userId: Int) =
        notificationDao.markAllAsRead(userId)
    
    suspend fun deleteNotification(notification: Notification) =
        notificationDao.deleteNotification(notification)
}

