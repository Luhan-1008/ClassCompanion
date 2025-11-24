package com.example.myapplication.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.service.NotificationService
import java.text.SimpleDateFormat
import java.util.*

class AssignmentNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val assignmentId = inputData.getLong("assignment_id", -1)
        if (assignmentId == -1L) return Result.failure()

        val database = AppDatabase.getDatabase(applicationContext)
        val assignment = database.assignmentDao().getAssignmentById(assignmentId.toInt())

        if (assignment != null && assignment.status != AssignmentStatus.COMPLETED) {
            val notificationService = NotificationService(applicationContext)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dueDateStr = dateFormat.format(Date(assignment.dueDate))
            
            notificationService.showAssignmentReminder(assignment.title, dueDateStr)
            return Result.success()
        }

        return Result.success()
    }
}
