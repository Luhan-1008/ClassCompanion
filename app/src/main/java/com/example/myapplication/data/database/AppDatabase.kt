package com.example.myapplication.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.model.*

@Database(
    entities = [
        User::class,
        Course::class,
        Assignment::class,
        StudyGroup::class,
        GroupMember::class,
        GroupMessage::class,
        GroupFile::class,
        GroupAnnouncement::class,
        GroupTask::class,
        GroupInvite::class,
        Note::class,
        Notification::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun studyGroupDao(): StudyGroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun groupFileDao(): GroupFileDao
    abstract fun groupAnnouncementDao(): GroupAnnouncementDao
    abstract fun groupTaskDao(): GroupTaskDao
    abstract fun groupInviteDao(): GroupInviteDao
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "course_companion_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

