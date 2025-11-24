package com.example.myapplication.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.service.NotificationService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
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
            val currentLocalTime = LocalTime.now()
            val today = LocalDate.now()
            
            // 检查即将到期的作业
            val upcomingAssignments = database.assignmentDao()
                .getUpcomingAssignments(userId, oneDayLater, AssignmentStatus.COMPLETED)
                .first()
            
            upcomingAssignments.forEach { assignment ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dueDateStr = dateFormat.format(Date(assignment.dueDate))
                notificationService.showAssignmentReminder(assignment.title, dueDateStr)
            }
            
            // 检查今日课程，提前 60 分钟提醒
            val todayCourses = database.courseDao()
                .getCoursesSnapshot(userId)
                .filter { it.dayOfWeek == today.dayOfWeek.value }

            todayCourses.forEach { course ->
                val startTime = runCatching { LocalTime.parse(course.startTime) }.getOrNull()
                if (startTime != null) {
                    val minutesUntilStart = Duration.between(currentLocalTime, startTime).toMinutes()
                    if (minutesUntilStart in 0..60) {
                        val location = course.location ?: "地点待定"
                        val timeRange = "${course.startTime}-${course.endTime}"
                        notificationService.showCourseReminder(course.courseName, location, timeRange)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

