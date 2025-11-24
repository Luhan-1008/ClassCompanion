package com.example.myapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class NotificationService(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "课程提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "课程和作业提醒通知"
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    fun showCourseReminder(courseName: String, location: String, time: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("课程提醒：$courseName")
            .setContentText("$time 在 $location")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    fun showAssignmentReminder(title: String, dueDate: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("作业提醒：$title")
            .setContentText("截止时间：$dueDate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "course_companion_channel"
    }
}

