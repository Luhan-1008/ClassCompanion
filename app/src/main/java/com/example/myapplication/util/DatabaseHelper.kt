package com.example.myapplication.util

import android.util.Log
import com.example.myapplication.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据库查看辅助工具类
 * 用于在开发时快速查看数据库内容
 * 
 * 使用方法：
 * 1. 在Activity或ViewModel中调用：DatabaseHelper.printUserData(context, userId)
 * 2. 查看Logcat，过滤标签 "DatabaseHelper"
 */
object DatabaseHelper {
    private const val TAG = "DatabaseHelper"
    
    /**
     * 打印用户数据
     */
    suspend fun printUserData(context: android.content.Context, userId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val user = db.userDao().getUserById(userId)
            Log.d(TAG, "========== 用户数据 ==========")
            Log.d(TAG, "用户: $user")
            Log.d(TAG, "==============================")
        }
    }
    
    /**
     * 打印用户的课程数据
     */
    suspend fun printCourses(context: android.content.Context, userId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.courseDao().getCoursesByUser(userId).collect { courses ->
                Log.d(TAG, "========== 课程数据 (${courses.size}条) ==========")
                courses.forEach { course ->
                    Log.d(TAG, "课程: id=${course.courseId}, name=${course.courseName}, code=${course.courseCode}, day=${course.dayOfWeek}")
                }
                Log.d(TAG, "==============================")
            }
        }
    }
    
    /**
     * 打印用户的作业数据
     */
    suspend fun printAssignments(context: android.content.Context, userId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.assignmentDao().getAssignmentsByUser(userId).collect { assignments ->
                Log.d(TAG, "========== 作业数据 (${assignments.size}条) ==========")
                assignments.forEach { assignment ->
                    Log.d(TAG, "作业: id=${assignment.assignmentId}, title=${assignment.title}, status=${assignment.status}, dueDate=${assignment.dueDate}")
                }
                Log.d(TAG, "==============================")
            }
        }
    }
    
    /**
     * 打印学习小组数据
     */
    suspend fun printGroups(context: android.content.Context, userId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.studyGroupDao().getGroupsByUser(userId).collect { groups ->
                Log.d(TAG, "========== 学习小组数据 (${groups.size}条) ==========")
                groups.forEach { group ->
                    Log.d(TAG, "小组: id=${group.groupId}, name=${group.groupName}, creator=${group.creatorId}, isPublic=${group.isPublic}")
                }
                Log.d(TAG, "==============================")
            }
        }
    }
    
    /**
     * 打印群消息数据
     */
    suspend fun printMessages(context: android.content.Context, groupId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.groupMessageDao().getMessagesByGroup(groupId).collect { messages ->
                Log.d(TAG, "========== 群消息数据 (${messages.size}条) ==========")
                messages.forEach { message ->
                    Log.d(TAG, "消息: id=${message.messageId}, userId=${message.userId}, content=${message.content}, type=${message.messageType}")
                }
                Log.d(TAG, "==============================")
            }
        }
    }
    
    /**
     * 打印文件数据
     */
    suspend fun printFiles(context: android.content.Context, groupId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.groupFileDao().getFilesByGroup(groupId).collect { files ->
                Log.d(TAG, "========== 文件数据 (${files.size}条) ==========")
                files.forEach { file ->
                    Log.d(TAG, "文件: id=${file.fileId}, name=${file.fileName}, type=${file.fileType}, size=${file.fileSize}")
                }
                Log.d(TAG, "==============================")
            }
        }
    }
}

