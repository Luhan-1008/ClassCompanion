package com.example.myapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PreciseReminderService.ACTION_COURSE_REMINDER -> {
                val courseId = intent.getIntExtra(PreciseReminderService.EXTRA_COURSE_ID, -1)
                if (courseId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = AppDatabase.getDatabase(context)
                        val course = database.courseDao().getCourseById(courseId)
                        course?.let {
                            val notificationService = NotificationService(context)
                            val location = it.location ?: "地点待定"
                            val timeRange = "${it.startTime}-${it.endTime}"
                            notificationService.showCourseReminder(it.courseName, location, timeRange)
                        }
                    }
                }
            }
            PreciseReminderService.ACTION_ASSIGNMENT_FIRST_REMINDER -> {
                val assignmentId = intent.getIntExtra(PreciseReminderService.EXTRA_ASSIGNMENT_ID, -1)
                if (assignmentId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = AppDatabase.getDatabase(context)
                        val assignment = database.assignmentDao().getAssignmentById(assignmentId)
                        assignment?.let {
                            val notificationService = NotificationService(context)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val dueDateStr = dateFormat.format(Date(it.dueDate))
                            notificationService.showAssignmentReminder(it.title, dueDateStr)
                        }
                    }
                }
            }
            PreciseReminderService.ACTION_ASSIGNMENT_URGENT_REMINDER -> {
                val assignmentId = intent.getIntExtra(PreciseReminderService.EXTRA_ASSIGNMENT_ID, -1)
                if (assignmentId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = AppDatabase.getDatabase(context)
                        val assignment = database.assignmentDao().getAssignmentById(assignmentId)
                        assignment?.let {
                            val notificationService = NotificationService(context)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val dueDateStr = dateFormat.format(Date(it.dueDate))
                            notificationService.showAssignmentReminder(
                                "紧急：${it.title}",
                                "$dueDateStr（${it.urgentReminderHours}小时后）"
                            )
                        }
                    }
                }
            }
        }
    }
}
