package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.model.*

/**
 * Room 数据库总入口
 *
 * 你可以把它理解成：
 * 1. 声明这个 App 里有哪些本地数据表（entities）
 * 2. 对外提供操作这些表的 Dao
 * 3. 负责创建并返回数据库实例
 */

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
        Notification::class,
        CourseReview::class,
        CourseResource::class,
        StudySession::class
    ],
    // 数据库版本号表结构发生变化时，通常需要修改这个值。
    version = 11,
    // 是否导出数据库 schema 文件（当前关闭 更适合学习和快速开发阶段）
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {

    // 每个 Dao 一般对应一类数据表操作
    // Repository 或 ViewModel 会通过这些 Dao 读写本地数据库
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
    abstract fun courseReviewDao(): CourseReviewDao
    abstract fun courseResourceDao(): CourseResourceDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        
        /**
         * 保存数据库单例实例。
         *
         * @Volatile 可以保证多线程下读取到的 INSTANCE 是最新值。
         */
        
        @Volatile
        private var INSTANCE: AppDatabase? = null


        /**
         * 获取数据库实例
         *
         * 这里使用单例模式，目的是让整个 App 运行期间通常只持有一个数据库对象，
         * 避免重复创建数据库带来的性能开销和状态不一致问题。
         */

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    // 使用 applicationContext，避免持有 Activity 导致内存泄漏。
                    context.applicationContext,
                    AppDatabase::class.java,
                    // 本地数据库文件名
                    "course_companion_database"
                )
                    // 当数据库版本变了、但你没有提供迁移方案时，
                    // 直接删除旧库并重建新库。
                    // 这样开发阶段比较方便，但会清空本地数据。
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
