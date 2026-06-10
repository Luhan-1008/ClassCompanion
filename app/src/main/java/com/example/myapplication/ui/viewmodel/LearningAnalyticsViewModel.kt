package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.StudySession
import com.example.myapplication.data.model.StudySessionType
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.data.repository.StudySessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class ReportRange(val days: Long) {
    WEEK(7),
    MONTH(30),
    QUARTER(90)
}

data class CourseDistribution(
    val course: Course,
    val minutes: Int
)

data class DailyStudyData(
    val date: String,
    val minutes: Int
)

data class WeeklyHeatmapData(
    val dayOfWeek: Int, // 0=周一, 6=周日
    val totalMinutes: Int
)

data class LearningAnalyticsUiState(
    val range: ReportRange = ReportRange.WEEK,
    val isLoading: Boolean = false,
    val courses: List<Course> = emptyList(),
    val totalStudyMinutes: Int = 0,
    val completionRate: Int = 0,
    val overdueAssignments: Int = 0,
    val pendingAssignments: Int = 0,
    val groupActivityScore: Int = 0,
    val timeDistribution: List<CourseDistribution> = emptyList(),
    val dailyTrend: List<DailyStudyData> = emptyList(),
    val weeklyHeatmap: List<WeeklyHeatmapData> = emptyList(),
    val bestStudyHour: Int? = null,
    val averageDailyMinutes: Int = 0,
    val suggestions: List<String> = emptyList(),
    val latestSessions: List<StudySession> = emptyList(),
    val errorMessage: String? = null
)

class LearningAnalyticsViewModel(
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val studySessionRepository: StudySessionRepository,
    private val groupMessageRepository: GroupMessageRepository
) : ViewModel() {

    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0

    private val _uiState = MutableStateFlow(LearningAnalyticsUiState())
    val uiState: StateFlow<LearningAnalyticsUiState> = _uiState.asStateFlow()

    init {
        refreshAnalytics()
    }

    fun refreshAnalytics(range: ReportRange = _uiState.value.range) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, range = range, errorMessage = null)
                val courses = courseRepository.getCoursesSnapshot(userId)
                val assignments = assignmentRepository.getAssignmentsSnapshot(userId)
                val now = System.currentTimeMillis()
                val start = now - Duration.ofDays(range.days).toMillis()
                val sessions = studySessionRepository.getSessionsWithin(userId, start, now)
                val totalMinutes = sessions.sumOf { it.durationMinutes }
                val completed = assignments.count { it.status == AssignmentStatus.COMPLETED }
                val completionRate = if (assignments.isEmpty()) 0 else ((completed.toDouble() / assignments.size) * 100).roundToInt()
                val overdue = assignments.count { it.status == AssignmentStatus.OVERDUE }
                val pending = assignments.count { it.status == AssignmentStatus.NOT_STARTED || it.status == AssignmentStatus.IN_PROGRESS }
                val groupActivity = groupMessageRepository.countMessagesBetween(start, now)
                val timeDistribution = buildDistribution(courses, sessions)
                val dailyTrend = buildDailyTrend(sessions, start, now, range.days)
                val weeklyHeatmap = buildWeeklyHeatmap(sessions, start, now)
                val bestStudyHour = findBestStudyHour(sessions)
                val averageDailyMinutes = if (range.days > 0) (totalMinutes / range.days.toDouble()).roundToInt() else 0
                val suggestions = buildSuggestions(completionRate, overdue, groupActivity, timeDistribution, averageDailyMinutes, bestStudyHour)
                _uiState.value = LearningAnalyticsUiState(
                    range = range,
                    isLoading = false,
                    courses = courses,
                    totalStudyMinutes = totalMinutes,
                    completionRate = completionRate,
                    overdueAssignments = overdue,
                    pendingAssignments = pending,
                    groupActivityScore = groupActivity,
                    timeDistribution = timeDistribution,
                    dailyTrend = dailyTrend,
                    weeklyHeatmap = weeklyHeatmap,
                    bestStudyHour = bestStudyHour,
                    averageDailyMinutes = averageDailyMinutes,
                    suggestions = suggestions,
                    latestSessions = sessions.take(5),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加载学习数据失败：${e.message}"
                )
            }
        }
    }

    private fun buildDistribution(
        courses: List<Course>,
        sessions: List<StudySession>
    ): List<CourseDistribution> {
        if (sessions.isEmpty()) return emptyList()
        val groupedMinutes = sessions.groupBy { it.courseId }.mapValues { entry ->
            entry.value.sumOf { it.durationMinutes }
        }
        return groupedMinutes.mapNotNull { (courseId, minutes) ->
            val course = courses.find { it.courseId == courseId } ?: return@mapNotNull null
            CourseDistribution(course = course, minutes = minutes)
        }.sortedByDescending { it.minutes }
    }

    private fun buildDailyTrend(
        sessions: List<StudySession>,
        start: Long,
        end: Long,
        days: Long
    ): List<DailyStudyData> {
        val dailyMap = mutableMapOf<String, Int>()
        val formatter = DateTimeFormatter.ofPattern("MM-dd")
        
        for (i in 0 until days) {
            val date = Instant.ofEpochMilli(start + Duration.ofDays(i).toMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dailyMap[formatter.format(date)] = 0
        }
        
        sessions.forEach { session ->
            val date = Instant.ofEpochMilli(session.sessionDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val dateStr = formatter.format(date)
            dailyMap[dateStr] = (dailyMap[dateStr] ?: 0) + session.durationMinutes
        }
        
        return dailyMap.toList().sortedBy { it.first }.map { (date, minutes) ->
            DailyStudyData(date, minutes)
        }
    }
    
    private fun buildWeeklyHeatmap(
        sessions: List<StudySession>,
        start: Long,
        end: Long
    ): List<WeeklyHeatmapData> {
        val dayMap = mutableMapOf<Int, Int>()
        
        sessions.forEach { session ->
            val dayOfWeek = Instant.ofEpochMilli(session.sessionDate)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek.value - 1 // 转换为 0=周一, 6=周日
            dayMap[dayOfWeek] = (dayMap[dayOfWeek] ?: 0) + session.durationMinutes
        }
        
        return (0..6).map { day ->
            WeeklyHeatmapData(day, dayMap[day] ?: 0)
        }
    }
    
    private fun findBestStudyHour(sessions: List<StudySession>): Int? {
        if (sessions.isEmpty()) return null
        val hourMap = mutableMapOf<Int, Int>()
        sessions.forEach { session ->
            val hour = Instant.ofEpochMilli(session.sessionDate)
                .atZone(ZoneId.systemDefault())
                .hour
            hourMap[hour] = (hourMap[hour] ?: 0) + session.durationMinutes
        }
        return hourMap.maxByOrNull { it.value }?.key
    }
    
    private fun buildSuggestions(
        completionRate: Int,
        overdue: Int,
        groupActivity: Int,
        distribution: List<CourseDistribution>,
        averageDailyMinutes: Int,
        bestStudyHour: Int?
    ): List<String> {
        val suggestions = mutableListOf<String>()
        if (completionRate < 70) {
            suggestions += "📊 任务完成率 ${completionRate}%，建议拆分大任务为小目标，每天完成 2-3 个小任务。"
        } else if (completionRate >= 90) {
            suggestions += "🎉 完成率 ${completionRate}%，表现优秀！继续保持高效学习节奏。"
        } else {
            suggestions += "✅ 完成率 ${completionRate}%，保持良好状态，可以适当增加挑战性任务。"
        }
        if (overdue > 0) {
            suggestions += "⚠️ 有 $overdue 个作业已逾期，建议今日优先处理，避免影响后续学习。"
        }
        if (groupActivity < 5) {
            suggestions += "💬 小组讨论较少，主动分享学习心得或提问可以促进共同进步。"
        } else if (groupActivity >= 20) {
            suggestions += "🔥 小组活跃度很高（${groupActivity} 条讨论），继续保持互动学习氛围。"
        } else {
            suggestions += "👥 小组活跃度良好，可以尝试组织一次学习分享会。"
        }
        if (averageDailyMinutes < 60) {
            suggestions += "⏰ 日均学习 ${averageDailyMinutes} 分钟，建议逐步增加到 90-120 分钟。"
        } else if (averageDailyMinutes >= 180) {
            suggestions += "🌟 日均学习 ${averageDailyMinutes} 分钟，学习强度很高，注意劳逸结合。"
        } else {
            suggestions += "📚 日均学习 ${averageDailyMinutes} 分钟，学习节奏合理，继续保持。"
        }
        bestStudyHour?.let { hour ->
            suggestions += "🕐 你的最佳学习时段是 ${hour}:00-${hour + 1}:00，建议在此时间段安排重要学习任务。"
        }
        if (distribution.isNotEmpty()) {
            val topCourse = distribution.first()
            if (topCourse.minutes > 300) {
                suggestions += "📖 在《${topCourse.course.courseName}》投入最多（${topCourse.minutes} 分钟），可以整理学习笔记或制作知识图谱。"
            }
        }
        return suggestions
    }

    fun logStudySession(
        courseId: Int?,
        durationMinutes: Int,
        sessionType: StudySessionType,
        topic: String?,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                val session = StudySession(
                    userId = userId,
                    courseId = courseId,
                    sessionType = sessionType,
                    durationMinutes = durationMinutes,
                    sessionDate = date,
                    focusTopic = topic ?: sessionType.name
                )
                studySessionRepository.insertSession(session)
                refreshAnalytics(_uiState.value.range)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "记录学习时长失败：${e.message}"
                )
            }
        }
    }

    fun updateStudySession(session: StudySession) {
        viewModelScope.launch {
            try {
                studySessionRepository.insertSession(session)
                refreshAnalytics(_uiState.value.range)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "更新记录失败：${e.message}"
                )
            }
        }
    }

    fun deleteStudySession(session: StudySession) {
        viewModelScope.launch {
            try {
                studySessionRepository.deleteSession(session)
                refreshAnalytics(_uiState.value.range)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除记录失败：${e.message}"
                )
            }
        }
    }

    fun formatSession(session: StudySession): String {
        val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        val dateTime = Instant.ofEpochMilli(session.sessionDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val typeNames = mapOf(
            StudySessionType.PREVIEW to "预习",
            StudySessionType.REVIEW to "复习",
            StudySessionType.ASSIGNMENT to "作业",
            StudySessionType.DISCUSSION to "讨论",
            StudySessionType.EXAM_PREP to "备考"
        )
        val typeName = typeNames[session.sessionType] ?: session.sessionType.name
        return "${formatter.format(dateTime)} · ${session.durationMinutes} 分钟 · $typeName"
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class LearningAnalyticsViewModelFactory(
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val studySessionRepository: StudySessionRepository,
    private val groupMessageRepository: GroupMessageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LearningAnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LearningAnalyticsViewModel(
                courseRepository,
                assignmentRepository,
                studySessionRepository,
                groupMessageRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

