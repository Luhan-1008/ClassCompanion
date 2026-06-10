package com.example.myapplication.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.Course
import com.example.myapplication.utils.ScheduleDateUtils
import com.example.myapplication.utils.ScheduleSettingsManager
import java.util.*

object PreciseReminderService {
    const val ACTION_COURSE_REMINDER = "com.example.myapplication.COURSE_REMINDER"
    const val ACTION_ASSIGNMENT_FIRST_REMINDER = "com.example.myapplication.ASSIGNMENT_FIRST_REMINDER"
    const val ACTION_ASSIGNMENT_URGENT_REMINDER = "com.example.myapplication.ASSIGNMENT_URGENT_REMINDER"
    
    const val EXTRA_COURSE_ID = "course_id"
    const val EXTRA_ASSIGNMENT_ID = "assignment_id"
    const val EXTRA_REMINDER_TYPE = "reminder_type"
    
    /**
     * 设置课程提醒（为未来4周内的课程设置提醒）
     */
    fun scheduleCourseReminders(context: Context, course: Course) {
        if (!course.reminderEnabled) return
        
        val settingsManager = ScheduleSettingsManager(context)
        val settings = settingsManager.getSettings()
        val currentWeek = ScheduleDateUtils.getCurrentWeek(settings)
        
        // 获取课程所在的周数列表
        val courseWeeks = if (!course.weeks.isNullOrBlank()) {
            course.weeks.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            (course.startWeek..course.endWeek).toList()
        }
        
        // 计算当前日期对应的周几（Calendar中周日是1，周一至周六是2-7）
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // 转换为1-7（周一是1）
        val todayDayOfWeek = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
        
        // 为未来4周内的课程设置提醒
        for (weekOffset in 0 until 4) {
            val targetWeek = currentWeek + weekOffset
            if (!courseWeeks.contains(targetWeek)) continue
            
            // 计算目标日期（课程所在周几）
            var daysUntilTarget = (course.dayOfWeek - todayDayOfWeek + weekOffset * 7)
            // 如果目标日期是今天或过去，且不是本周，则跳过
            if (daysUntilTarget < 0 && weekOffset == 0) continue
            // 如果daysUntilTarget为负数，说明是下周或更晚
            if (daysUntilTarget < 0) {
                daysUntilTarget += 7
            }
            
            val targetCalendar = Calendar.getInstance()
            targetCalendar.add(Calendar.DAY_OF_YEAR, daysUntilTarget)
            
            // 计算提醒时间（课程开始前 reminderMinutes 分钟）
            val startTime = course.startTime.split(":")
            val reminderCalendar = targetCalendar.clone() as Calendar
            reminderCalendar.set(Calendar.HOUR_OF_DAY, startTime[0].toInt())
            reminderCalendar.set(Calendar.MINUTE, startTime[1].toInt())
            reminderCalendar.set(Calendar.SECOND, 0)
            reminderCalendar.set(Calendar.MILLISECOND, 0)
            reminderCalendar.add(Calendar.MINUTE, -course.reminderMinutes)
            
            // 确保在整分钟发送（秒数和毫秒数已设为0）
            
            val reminderTime = reminderCalendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            
            // 只设置未来的提醒
            if (reminderTime > currentTime) {
                // 使用课程ID和日期组合作为唯一标识
                val requestCode = course.courseId * 1000 + daysUntilTarget
                
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_COURSE_REMINDER
                    putExtra(EXTRA_COURSE_ID, course.courseId)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime,
                                pendingIntent
                            )
                        } else {
                            // 如果没有精确闹钟权限，使用非精确闹钟或引导用户授权
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime,
                                pendingIntent
                            )
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    // 降级处理
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
            }
        }
    }
    
    /**
     * 设置任务首次提醒
     */
    fun scheduleAssignmentFirstReminder(context: Context, assignment: Assignment) {
        if (!assignment.reminderEnabled || assignment.reminderTime == null) return
        
        // 将提醒时间精确到分钟（移除秒和毫秒）
        val reminderCalendar = Calendar.getInstance().apply {
            timeInMillis = assignment.reminderTime
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val reminderTime = reminderCalendar.timeInMillis
        val currentTime = System.currentTimeMillis()
        
        // 只设置未来的提醒
        if (reminderTime > currentTime) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_ASSIGNMENT_FIRST_REMINDER
                putExtra(EXTRA_ASSIGNMENT_ID, assignment.assignmentId)
                putExtra(EXTRA_REMINDER_TYPE, "first")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                assignment.assignmentId * 10 + 1, // 使用唯一ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        }
    }
    
    /**
     * 设置任务紧急提醒
     */
    fun scheduleAssignmentUrgentReminder(context: Context, assignment: Assignment) {
        if (!assignment.reminderEnabled) return
        
        val urgentReminderTime = assignment.dueDate - assignment.urgentReminderHours * 60 * 60 * 1000L
        // 将提醒时间精确到分钟（移除秒和毫秒）
        val reminderCalendar = Calendar.getInstance().apply {
            timeInMillis = urgentReminderTime
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val reminderTime = reminderCalendar.timeInMillis
        val currentTime = System.currentTimeMillis()
        
        // 只设置未来的提醒，且要在截止时间之前
        if (reminderTime > currentTime && reminderTime < assignment.dueDate) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_ASSIGNMENT_URGENT_REMINDER
                putExtra(EXTRA_ASSIGNMENT_ID, assignment.assignmentId)
                putExtra(EXTRA_REMINDER_TYPE, "urgent")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                assignment.assignmentId * 10 + 2, // 使用唯一ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        }
    }
    
    /**
     * 取消课程提醒
     */
    fun cancelCourseReminder(context: Context, courseId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COURSE_REMINDER
            putExtra(EXTRA_COURSE_ID, courseId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            courseId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * 取消任务提醒
     */
    fun cancelAssignmentReminders(context: Context, assignmentId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 取消首次提醒
        val firstIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_ASSIGNMENT_FIRST_REMINDER
            putExtra(EXTRA_ASSIGNMENT_ID, assignmentId)
        }
        val firstPendingIntent = PendingIntent.getBroadcast(
            context,
            assignmentId * 10 + 1,
            firstIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(firstPendingIntent)
        
        // 取消紧急提醒
        val urgentIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_ASSIGNMENT_URGENT_REMINDER
            putExtra(EXTRA_ASSIGNMENT_ID, assignmentId)
        }
        val urgentPendingIntent = PendingIntent.getBroadcast(
            context,
            assignmentId * 10 + 2,
            urgentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(urgentPendingIntent)
    }
}

