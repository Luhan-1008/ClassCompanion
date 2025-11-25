package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.domain.ai.AiAssistEngine
import com.example.myapplication.domain.ai.AiNoteAttachment
import com.example.myapplication.domain.ai.AiNoteInsights
import com.example.myapplication.domain.ai.AssignmentHint
import com.example.myapplication.domain.ai.StudyPlanDay
import com.example.myapplication.service.AiModelService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiNoteUiState(
    val isProcessing: Boolean = false,
    val insights: AiNoteInsights? = null,
    val errorMessage: String? = null
)

data class AssignmentHelpUiState(
    val isProcessing: Boolean = false,
    val hint: AssignmentHint? = null,
    val errorMessage: String? = null
)

data class SmartPlannerUiState(
    val isProcessing: Boolean = false,
    val plan: List<StudyPlanDay> = emptyList(),
    val errorMessage: String? = null
)

class AiAssistViewModel(
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val groupMessageRepository: GroupMessageRepository
) : ViewModel() {

    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0

    private val _noteUiState = MutableStateFlow(AiNoteUiState())
    val noteUiState: StateFlow<AiNoteUiState> = _noteUiState.asStateFlow()

    private val _assignmentUiState = MutableStateFlow(AssignmentHelpUiState())
    val assignmentUiState: StateFlow<AssignmentHelpUiState> = _assignmentUiState.asStateFlow()

    private val _plannerUiState = MutableStateFlow(SmartPlannerUiState())
    val plannerUiState: StateFlow<SmartPlannerUiState> = _plannerUiState.asStateFlow()

    fun analyzeNotes(noteText: String, attachments: List<AiNoteAttachment>, context: android.content.Context) {
        if (noteText.isBlank() && attachments.isEmpty()) {
            _noteUiState.value = AiNoteUiState(
                isProcessing = false,
                errorMessage = "请输入内容或至少选择一个附件"
            )
            return
        }
        viewModelScope.launch {
            try {
                _noteUiState.value = _noteUiState.value.copy(isProcessing = true, errorMessage = null)
                
                val aiService = com.example.myapplication.service.AiModelService(context)
                
                // 处理图片：转换为base64
                var imageBase64: String? = null
                val imageAttachment = attachments.firstOrNull { it.type == com.example.myapplication.domain.ai.AttachmentType.IMAGE }
                if (imageAttachment != null) {
                    val uri = android.net.Uri.parse(imageAttachment.uri)
                    imageBase64 = aiService.imageToBase64(uri)
                }
                
                // 处理音频：转文字
                var audioTranscript: String? = null
                var audioError: String? = null
                val audioAttachment = attachments.firstOrNull { it.type == com.example.myapplication.domain.ai.AttachmentType.AUDIO }
                if (audioAttachment != null) {
                    val uri = android.net.Uri.parse(audioAttachment.uri)
                    val transcriptResult = aiService.transcribeAudio(uri)
                    transcriptResult.onSuccess { transcript ->
                        audioTranscript = transcript
                    }.onFailure { error ->
                        audioError = error.message
                        // 如果只有音频没有其他内容，直接返回错误
                        if (noteText.isBlank() && imageBase64 == null) {
                            _noteUiState.value = AiNoteUiState(
                                isProcessing = false,
                                errorMessage = "音频转文字失败：${error.message}"
                            )
                            return@launch
                        }
                    }
                }
                
                // 调用大模型生成知识提纲
                val outlineResult = aiService.generateKnowledgeOutline(
                    textContent = noteText,
                    imageBase64 = imageBase64,
                    audioTranscript = audioTranscript
                )
                
                val outline = outlineResult.getOrThrow()
                
                // 转换为AiNoteInsights格式
                val courses = courseRepository.getCoursesSnapshot(userId)
                val insights = com.example.myapplication.domain.ai.AiNoteInsights(
                    summary = outline.summary,
                    structuredOutline = outline.structuredOutline.map { 
                        com.example.myapplication.domain.ai.OutlineSection(it.title, it.bulletPoints)
                    },
                    mindMapBranches = outline.mindMapBranches.map {
                        com.example.myapplication.domain.ai.MindMapBranch(it.title, it.nodes)
                    },
                    keyPoints = outline.keyPoints,
                    chapterLinks = outline.chapterLinks.map {
                        com.example.myapplication.domain.ai.ChapterLink(it.courseName, it.chapterLabel, it.reason)
                    }
                )
                
                _noteUiState.value = AiNoteUiState(isProcessing = false, insights = insights)
            } catch (e: Exception) {
                _noteUiState.value = AiNoteUiState(
                    isProcessing = false,
                    errorMessage = "分析失败：${e.message}"
                )
            }
        }
    }

    fun requestAssignmentHelp(question: String, context: Context) {
        if (question.isBlank()) {
            _assignmentUiState.value = AssignmentHelpUiState(
                isProcessing = false,
                errorMessage = "请输入问题描述"
            )
            return
        }
        viewModelScope.launch {
            try {
                _assignmentUiState.value = _assignmentUiState.value.copy(isProcessing = true, errorMessage = null)
                val courses = courseRepository.getCoursesSnapshot(userId)
                val assignments = assignmentRepository.getAssignmentsSnapshot(userId)
                val keywords = AiAssistEngine.pickKeywords(question)
                val relatedMessages = keywords
                    .takeIf { it.isNotEmpty() }
                    ?.flatMap { keyword ->
                        groupMessageRepository.searchMessages(keyword, limit = 5)
                    } ?: emptyList()
                val contextInfo = buildString {
                    if (courses.isNotEmpty()) {
                        append("相关课程：\n")
                        courses.take(5).forEach { course ->
                            append("- ${course.courseName}")
                            course.teacherName?.takeIf { it.isNotBlank() }?.let { append("（教师：$it）") }
                            append("\n")
                        }
                        append("\n")
                    }
                    if (assignments.isNotEmpty()) {
                        append("相关作业：\n")
                        assignments.take(5).forEach { assignment ->
                            append("- ${assignment.title}")
                            assignment.description?.takeIf { it.isNotBlank() }?.let { append("：${it.take(60)}") }
                            append("\n")
                        }
                        append("\n")
                    }
                    if (relatedMessages.isNotEmpty()) {
                        append("小组讨论要点：\n")
                        relatedMessages.take(5).forEach { message ->
                            append("- $message\n")
                        }
                    }
                }.ifBlank { null }
                
                val aiService = AiModelService(context)
                val hintResult = aiService.generateAssignmentHint(question, contextInfo)
                
                hintResult.onSuccess { hintResponse ->
                    val hint = AssignmentHint(
                        relatedConcepts = hintResponse.relatedConcepts,
                        formulas = hintResponse.formulas,
                        solutionSteps = hintResponse.solutionSteps,
                        chapterRecommendations = hintResponse.chapterLinks.map {
                            com.example.myapplication.domain.ai.ChapterLink(it.courseName, it.chapterLabel, it.reason)
                        },
                        relatedDiscussions = hintResponse.relatedDiscussions
                    )
                    _assignmentUiState.value = AssignmentHelpUiState(isProcessing = false, hint = hint)
                }.onFailure { error ->
                    _assignmentUiState.value = AssignmentHelpUiState(
                        isProcessing = false,
                        errorMessage = error.message ?: "提示生成失败，请稍后重试"
                    )
                }
            } catch (e: Exception) {
                _assignmentUiState.value = AssignmentHelpUiState(
                    isProcessing = false,
                    errorMessage = "提示生成失败：${e.message}"
                )
            }
        }
    }

    fun generateSmartPlanner(dayCount: Int = 5) {
        viewModelScope.launch {
            try {
                _plannerUiState.value = _plannerUiState.value.copy(isProcessing = true, errorMessage = null)
                val courses = courseRepository.getCoursesSnapshot(userId)
                val assignments = assignmentRepository.getAssignmentsSnapshot(userId)
                    .filter { it.status != AssignmentStatus.COMPLETED }
                val plan = AiAssistEngine.generateSmartPlan(courses, assignments, dayCount)
                _plannerUiState.value = SmartPlannerUiState(isProcessing = false, plan = plan)
            } catch (e: Exception) {
                _plannerUiState.value = SmartPlannerUiState(
                    isProcessing = false,
                    errorMessage = "日程生成失败：${e.message}"
                )
            }
        }
    }

    fun clearNoteError() {
        _noteUiState.value = _noteUiState.value.copy(errorMessage = null)
    }

    fun clearAssignmentError() {
        _assignmentUiState.value = _assignmentUiState.value.copy(errorMessage = null)
    }

    fun clearPlannerError() {
        _plannerUiState.value = _plannerUiState.value.copy(errorMessage = null)
    }
}

class AiAssistViewModelFactory(
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val groupMessageRepository: GroupMessageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiAssistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiAssistViewModel(courseRepository, assignmentRepository, groupMessageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

