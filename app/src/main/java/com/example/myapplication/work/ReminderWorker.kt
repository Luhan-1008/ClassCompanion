package com.example.myapplication.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.service.NotificationService
import com.example.myapplication.utils.ScheduleDateUtils
import com.example.myapplication.utils.ScheduleSettingsManager
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import android.content.SharedPreferences

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val notificationService = NotificationService(applicationContext)
            val settingsManager = ScheduleSettingsManager(applicationContext)
            val settings = settingsManager.getSettings()
            val userId = com.example.myapplication.session.CurrentSession.userIdInt
                ?: return Result.success() // 未登录则不提醒
            
            // 检查通知权限（Android 13+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                if (!hasPermission) {
                    // 没有通知权限，直接返回成功，避免重复尝试
                    return Result.success()
                }
            }
            
            val currentTime = System.currentTimeMillis()
            val oneDayLater = currentTime + 24 * 60 * 60 * 1000
            val currentLocalTime = LocalTime.now()
            val today = LocalDate.now()
            val calendar = Calendar.getInstance()
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // Calendar中周日是1，周一至周六是2-7，转换为1-7（周一是1）
            val dayOfWeek = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
            
            // 计算当前周数
            val currentWeek = ScheduleDateUtils.getCurrentWeek(settings)
            
            // 使用SharedPreferences记录已发送的提醒，避免重复通知
            val prefs = applicationContext.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // 获取所有未完成的作业
            val allAssignments = database.assignmentDao()
                .getUpcomingAssignments(userId, Long.MAX_VALUE, AssignmentStatus.COMPLETED)
                .first()
            
            // 检查作业提醒（只提醒两次：首次提醒和紧急提醒）
            allAssignments.forEach { assignment ->
                if (!assignment.reminderEnabled) return@forEach
                
                val dueDate = assignment.dueDate
                val urgentReminderTime = dueDate - assignment.urgentReminderHours * 60 * 60 * 1000L
                
                // 检查首次提醒时间（如果设置了）- 精准提醒
                if (assignment.reminderTime != null) {
                    val reminderKey = "first_reminder_${assignment.assignmentId}"
                    val hasSentFirstReminder = prefs.getBoolean(reminderKey, false)
                    
                    // 精准时间判断：在当前分钟且未发送过时提醒
                    val reminderCalendar = Calendar.getInstance().apply {
                        timeInMillis = assignment.reminderTime
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val currentCalendar = Calendar.getInstance().apply {
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val isExactMinute = reminderCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                            reminderCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR) &&
                            reminderCalendar.get(Calendar.HOUR_OF_DAY) == currentCalendar.get(Calendar.HOUR_OF_DAY) &&
                            reminderCalendar.get(Calendar.MINUTE) == currentCalendar.get(Calendar.MINUTE)
                    if (!hasSentFirstReminder && isExactMinute) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val dueDateStr = dateFormat.format(Date(dueDate))
                        notificationService.showAssignmentReminder(assignment.title, dueDateStr)
                        // 标记已发送首次提醒
                        prefs.edit().putBoolean(reminderKey, true).apply()
                    }
                }
                
                // 检查紧急提醒时间（截止时间前N小时）- 精准提醒
                val urgentKey = "urgent_reminder_${assignment.assignmentId}"
                val hasSentUrgentReminder = prefs.getBoolean(urgentKey, false)
                
                    // 精准时间判断：在当前分钟且未发送过时提醒
                    val urgentCalendar = Calendar.getInstance().apply {
                        timeInMillis = urgentReminderTime
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val currentCalendar = Calendar.getInstance().apply {
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val isExactMinute = urgentCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                            urgentCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR) &&
                            urgentCalendar.get(Calendar.HOUR_OF_DAY) == currentCalendar.get(Calendar.HOUR_OF_DAY) &&
                            urgentCalendar.get(Calendar.MINUTE) == currentCalendar.get(Calendar.MINUTE)
                    if (!hasSentUrgentReminder && isExactMinute && currentTime < dueDate) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val dueDateStr = dateFormat.format(Date(dueDate))
                    notificationService.showAssignmentReminder(
                        "紧急：${assignment.title}",
                        "$dueDateStr（${assignment.urgentReminderHours}小时后）"
                    )
                    // 标记已发送紧急提醒
                    prefs.edit().putBoolean(urgentKey, true).apply()
                }
            }
            
            // 检查今日课程，使用课程的提醒设置
            val allCourses = database.courseDao().getCoursesSnapshot(userId)
            val todayCourses = allCourses.filter { course ->
                // 检查是否是今天的课程
                course.dayOfWeek == dayOfWeek &&
                // 检查提醒是否启用
                course.reminderEnabled &&
                // 检查课程是否在当前周
                if (!course.weeks.isNullOrBlank()) {
                    // 如果weeks字段不为空，使用精确的周数列表
                    course.weeks.split(",").mapNotNull { week -> week.trim().toIntOrNull() }.contains(currentWeek)
                } else {
                    // 否则使用startWeek和endWeek的范围
                    currentWeek >= course.startWeek && currentWeek <= course.endWeek
                }
            }
            
            todayCourses.forEach { course ->
                val startTime = runCatching { LocalTime.parse(course.startTime) }.getOrNull()
                if (startTime != null) {
                    val reminderMinutes = course.reminderMinutes
                    
                    // 计算提醒时间点（课程开始前 reminderMinutes 分钟）
                    val reminderTime = startTime.minusMinutes(reminderMinutes.toLong())
                    
                    // 计算当前时间距离课程开始时间的分钟数
                    val minutesUntilStart = Duration.between(currentLocalTime, startTime).toMinutes()
                    
                    // 精准时间判断：在当前分钟且课程开始之前
                    // 确保只提醒一次
                    val currentHour = currentLocalTime.hour
                    val currentMinute = currentLocalTime.minute
                    val reminderHour = reminderTime.hour
                    val reminderMinute = reminderTime.minute
                    val isExactMinute = currentHour == reminderHour && currentMinute == reminderMinute
                    if (isExactMinute && minutesUntilStart >= 0) {
                        val reminderKey = "${todayKey}_course_${course.courseId}_${course.startTime}"
                        val hasSentReminder = prefs.getBoolean(reminderKey, false)
                        
                        // 如果还没有发送过提醒，则发送（只提醒一次）
                        if (!hasSentReminder) {
                            val location = course.location ?: "地点待定"
                            val timeRange = "${course.startTime}-${course.endTime}"
                            notificationService.showCourseReminder(course.courseName, location, timeRange)
                            // 标记已发送提醒
                            prefs.edit().putBoolean(reminderKey, true).apply()
                        }
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

