package com.example.myapplication.ui.viewmodel

import android.content.Context
import android.util.Log
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
                
                // 处理图片：转换为base64（仅取第一张）
                var imageBase64: String? = null
                val imageAttachment = attachments.firstOrNull { it.type == com.example.myapplication.domain.ai.AttachmentType.IMAGE }
                if (imageAttachment != null) {
                    val uri = android.net.Uri.parse(imageAttachment.uri)
                    imageBase64 = aiService.imageToBase64(uri)
                }
                
                // 处理音频：支持多段录音依次转文字，并合并结果
                var audioTranscript: String? = null
                var audioError: String? = null
                val audioAttachments = attachments.filter { it.type == com.example.myapplication.domain.ai.AttachmentType.AUDIO }
                if (audioAttachments.isNotEmpty()) {
                    val combined = StringBuilder()
                    var successCount = 0
                    audioAttachments.forEachIndexed { index, audioAttachment ->
                        val uri = android.net.Uri.parse(audioAttachment.uri)
                        val transcriptResult = aiService.transcribeAudio(uri)
                        transcriptResult.onSuccess { transcript ->
                            val trimmed = transcript.trim()
                            if (trimmed.isNotEmpty()) {
                                if (combined.isNotEmpty()) {
                                    combined.append("\n\n---- 第${index + 1}段录音 ----\n")
                                }
                                combined.append(trimmed)
                                successCount++
                            }
                        }.onFailure { error ->
                            audioError = error.message
                        }
                    }
                    if (successCount > 0) {
                        audioTranscript = combined.toString()
                    } else if (noteText.isBlank() && imageBase64 == null) {
                        // 只有音频但全部转写失败，直接提示错误
                        _noteUiState.value = AiNoteUiState(
                            isProcessing = false,
                            errorMessage = "音频转文字失败：${audioError ?: "请检查音频文件格式或时长"}"
                        )
                        return@launch
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

    fun requestAssignmentHelp(
        question: String,
        context: Context,
        imageUri: android.net.Uri? = null
    ) {
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
                val aiService = AiModelService(context)

                // 如果有题目截图，先尝试做一次 OCR，把识别到的文字拼到上下文中
                var imageContext: String? = null
                if (imageUri != null) {
                    try {
                        val base64 = aiService.imageToBase64(imageUri)
                        if (base64 != null) {
                            val extracted = aiService.extractTextFromImage(base64)
                            if (!extracted.isNullOrBlank()) {
                                imageContext = buildString {
                                    append("题目截图识别内容：\n")
                                    append(extracted.trim())
                                    append("\n\n")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("AiAssistViewModel", "解析题目截图失败: ${e.message}", e)
                    }
                }

                val courses = try {
                    courseRepository.getCoursesSnapshot(userId)
                } catch (e: Exception) {
                    Log.e("AiAssistViewModel", "获取课程失败: ${e.message}", e)
                    emptyList()
                }
                val assignments = try {
                    assignmentRepository.getAssignmentsSnapshot(userId)
                } catch (e: Exception) {
                    Log.e("AiAssistViewModel", "获取作业失败: ${e.message}", e)
                    emptyList()
                }
                val keywords = AiAssistEngine.pickKeywords(question)
                val relatedMessages = try {
                    keywords
                        .takeIf { it.isNotEmpty() }
                        ?.flatMap { keyword ->
                            try {
                                groupMessageRepository.searchMessages(keyword, limit = 5)
                            } catch (e: Exception) {
                                Log.w("AiAssistViewModel", "搜索消息失败: ${e.message}", e)
                                emptyList()
                            }
                        } ?: emptyList()
                } catch (e: Exception) {
                    Log.w("AiAssistViewModel", "处理相关消息失败: ${e.message}", e)
                    emptyList()
                }
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
                            try {
                                val content = message.content?.take(60) ?: "（无内容）"
                                append("- $content\n")
                            } catch (e: Exception) {
                                Log.w("AiAssistViewModel", "处理消息内容失败: ${e.message}", e)
                            }
                        }
                    }

                    imageContext?.let {
                        append(it)
                    }
                }.ifBlank { null }

                val hintResult = aiService.generateAssignmentHint(question, contextInfo)
                
                hintResult.onSuccess { hintResponse ->
                    try {
                        val hint = AssignmentHint(
                            relatedConcepts = hintResponse.relatedConcepts,
                            formulas = hintResponse.formulas,
                            solutionSteps = hintResponse.solutionSteps,
                            chapterRecommendations = try {
                                hintResponse.chapterLinks.map {
                                    com.example.myapplication.domain.ai.ChapterLink(
                                        it.courseName,
                                        it.chapterLabel,
                                        it.reason
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("AiAssistViewModel", "转换章节链接失败: ${e.message}", e)
                                emptyList()
                            },
                            relatedDiscussions = hintResponse.relatedDiscussions
                        )
                        _assignmentUiState.value = AssignmentHelpUiState(isProcessing = false, hint = hint)
                    } catch (e: Exception) {
                        Log.e("AiAssistViewModel", "处理提示响应失败: ${e.message}", e)
                        _assignmentUiState.value = AssignmentHelpUiState(
                            isProcessing = false,
                            errorMessage = "处理AI响应失败：${e.message}"
                        )
                    }
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

