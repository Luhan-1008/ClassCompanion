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
    MONTH(30)
}

data class CourseDistribution(
    val course: Course,
    val minutes: Int
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
                val suggestions = buildSuggestions(completionRate, overdue, groupActivity, timeDistribution)
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

    private fun buildSuggestions(
        completionRate: Int,
        overdue: Int,
        groupActivity: Int,
        distribution: List<CourseDistribution>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        if (completionRate < 70) {
            suggestions += "本周任务完成率偏低，可尝试拆分任务并增加早晚 30 分钟快冲刺。"
        } else {
            suggestions += "保持 ${completionRate}% 的完成率，继续复盘高价值题目。"
        }
        if (overdue > 0) {
            suggestions += "有 $overdue 个作业已逾期，优先在今日安排补交窗口。"
        }
        if (groupActivity < 5) {
            suggestions += "小组讨论较少，建议主动发起一次问题投票或资料分享。"
        } else {
            suggestions += "小组活跃度良好，尝试沉淀讨论纪要形成 FAQ。"
        }
        if (distribution.isNotEmpty()) {
            val topCourse = distribution.first()
            suggestions += "本周期在《${topCourse.course.courseName}》投入 ${topCourse.minutes} 分钟，可输出本周总结或错题集。"
        }
        return suggestions
    }

    fun logStudySession(
        courseId: Int?,
        durationMinutes: Int,
        sessionType: StudySessionType,
        topic: String?
    ) {
        viewModelScope.launch {
            try {
                val session = StudySession(
                    userId = userId,
                    courseId = courseId,
                    sessionType = sessionType,
                    durationMinutes = durationMinutes,
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

    fun formatSession(session: StudySession): String {
        val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        val dateTime = Instant.ofEpochMilli(session.sessionDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return "${formatter.format(dateTime)} · ${session.durationMinutes} 分钟 · ${session.sessionType.name}"
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

