package com.example.myapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationService(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        // Android 8.0+ 需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 使用 IMPORTANCE_HIGH 确保通知能够显示
            val channel = NotificationChannel(
                CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "课程和作业提醒通知"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showCourseReminder(courseName: String, location: String, time: String) {
        try {
            // 使用课程名称和时间组合的hashCode作为通知ID，避免重复通知
            val notificationId = ("course_${courseName}_$time").hashCode()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("课程提醒：$courseName")
                .setContentText("$time 在 $location")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // 添加声音、振动等默认设置
                .build()
            
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun showAssignmentReminder(title: String, dueDate: String) {
        // 使用作业标题的hashCode作为通知ID，避免重复通知
        val notificationId = ("assignment_$title").hashCode()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("作业提醒：$title")
            .setContentText("截止时间：$dueDate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "course_companion_channel"
    }
}

