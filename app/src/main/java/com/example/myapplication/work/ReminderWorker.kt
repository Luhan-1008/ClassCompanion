package com.example.myapplication.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.service.NotificationService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val notificationService = NotificationService(applicationContext)
            val userId = com.example.myapplication.session.CurrentSession.userIdInt
                ?: return Result.success() // 未登录则不提醒
            
            val currentTime = System.currentTimeMillis()
            val oneDayLater = currentTime + 24 * 60 * 60 * 1000
            
            // 检查即将到期的作业
            val upcomingAssignments = database.assignmentDao()
                .getUpcomingAssignments(userId, oneDayLater, AssignmentStatus.COMPLETED)
                .first()
            
            upcomingAssignments.forEach { assignment ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dueDateStr = dateFormat.format(Date(assignment.dueDate))
                notificationService.showAssignmentReminder(assignment.title, dueDateStr)
            }
            
            // 检查今日课程（这里简化处理，实际应该检查当前时间和课程时间）
            // TODO: 实现课程提醒逻辑
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

