package com.example.myapplication.domain.ai

import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.GroupMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

data class AiNoteAttachment(
    val uri: String,
    val displayName: String,
    val type: AttachmentType
)

enum class AttachmentType {
    IMAGE,
    AUDIO,
    TEXT,
    OTHER
}

data class OutlineSection(
    val title: String,
    val bulletPoints: List<String>
)

data class MindMapBranch(
    val title: String,
    val nodes: List<String>
)

data class ChapterLink(
    val courseName: String,
    val chapterLabel: String,
    val reason: String
)

data class AiNoteInsights(
    val summary: String,
    val structuredOutline: List<OutlineSection>,
    val mindMapBranches: List<MindMapBranch>,
    val keyPoints: List<String>,
    val chapterLinks: List<ChapterLink>
)

data class AssignmentHint(
    val relatedConcepts: List<String>,
    val formulas: List<String>,
    val solutionSteps: List<String>,
    val chapterRecommendations: List<ChapterLink>,
    val relatedDiscussions: List<String>
)

data class StudyPlanDay(
    val date: LocalDate,
    val sessions: List<PlannedSession>,
    val priorityAssignments: List<String>,
    val focusScore: Int
)

data class PlannedSession(
    val label: String,
    val detail: String,
    val timeRange: String,
    val type: SessionKind,
    val courseId: Int? = null,
    val assignmentId: Int? = null
)

enum class SessionKind {
    CLASS,
    REVIEW,
    ASSIGNMENT,
    DISCUSSION,
    BUFFER
}

object AiAssistEngine {
    private val sentenceDelimiter = Regex("[。！？!?\\n]")
    private val whitespace = Regex("\\s+")
    private val keywordDictionary = mapOf(
        "傅里叶" to listOf(
            ChapterLink("信号与系统", "第4章 傅里叶变换", "题干涉及频谱/傅里叶对偶"),
            ChapterLink("数字信号处理", "第5章 快速傅里叶变换", "关键词匹配 FFT/频域")
        ),
        "数据结构" to listOf(
            ChapterLink("数据结构", "第3章 树与二叉树", "包含树/遍历/层序关键词"),
            ChapterLink("数据结构", "第5章 图算法", "检测到图/连通性/最短路描述")
        ),
        "线性代数" to listOf(
            ChapterLink("线性代数", "第2章 向量与矩阵运算", "出现向量/矩阵/秩等术语"),
            ChapterLink("线性代数", "第4章 特征值与特征向量", "提及特征值/对角化概念")
        )
    )

    fun generateNoteInsights(
        textContent: String,
        attachments: List<AiNoteAttachment>,
        courses: List<Course>
    ): AiNoteInsights {
        val cleaned = textContent.replace(whitespace, " ").trim()
        val sentences = cleaned.split(sentenceDelimiter).map { it.trim() }.filter { it.length > 4 }
        val outline = buildOutline(sentences)
        val mindMap = buildMindMap(sentences, attachments)
        val keyPoints = sentences.sortedByDescending { it.length }.take(5)
        val summary = buildSummary(sentences, attachments)
        val chapterLinks = mapChapters(cleaned, courses)
        return AiNoteInsights(summary, outline, mindMap, keyPoints, chapterLinks)
    }

    private fun buildOutline(sentences: List<String>): List<OutlineSection> {
        if (sentences.isEmpty()) return emptyList()
        val chunkSize = max(1, sentences.size / 4)
        return sentences.chunked(chunkSize).mapIndexed { index, chunk ->
            val titleCandidate = chunk.firstOrNull()?.take(18)?.trim().orEmpty()
            OutlineSection(
                title = "主题 ${index + 1}：$titleCandidate",
                bulletPoints = chunk.take(4)
            )
        }
    }

    private fun buildMindMap(
        sentences: List<String>,
        attachments: List<AiNoteAttachment>
    ): List<MindMapBranch> {
        val conceptNodes = sentences
            .flatMap { it.split("，", " ", "、") }
            .map { it.trim() }
            .filter { it.length in 2..12 }
            .groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key }

        val attachmentNodes = attachments.map { attachment ->
            when (attachment.type) {
                AttachmentType.IMAGE -> "图像：${attachment.displayName}"
                AttachmentType.AUDIO -> "音频：${attachment.displayName}"
                AttachmentType.TEXT -> "文本：${attachment.displayName}"
                AttachmentType.OTHER -> "附件：${attachment.displayName}"
            }
        }

        val actionNodes = listOf("回顾错题", "整理公式卡片", "制作自测题", "联想实验/生活场景")

        return listOf(
            MindMapBranch("核心概念", conceptNodes),
            MindMapBranch("多模态素材", attachmentNodes),
            MindMapBranch("复习行动", actionNodes)
        ).filter { it.nodes.isNotEmpty() }
    }

    private fun buildSummary(sentences: List<String>, attachments: List<AiNoteAttachment>): String {
        val head = sentences.take(2)
        val attachmentHint = if (attachments.isNotEmpty()) {
            "已识别到 ${attachments.size} 个附件，可结合图片/录音补充细节。"
        } else ""
        return (head + attachmentHint).joinToString(separator = "\n")
    }

    private fun mapChapters(textContent: String, courses: List<Course>): List<ChapterLink> {
        val matches = mutableListOf<ChapterLink>()
        keywordDictionary.forEach { (keyword, links) ->
            if (textContent.contains(keyword, ignoreCase = true)) {
                matches += links
            }
        }
        courses.forEach { course ->
            val hint = course.courseName.take(4)
            if (textContent.contains(hint, ignoreCase = true)) {
                matches += ChapterLink(
                    courseName = course.courseName,
                    chapterLabel = "课堂重点",
                    reason = "笔记中提到课程关键词 $hint"
                )
            }
        }
        return matches.distinctBy { it.courseName + it.chapterLabel }
    }

    fun generateAssignmentHint(
        question: String,
        courses: List<Course>,
        assignments: List<Assignment>,
        relatedMessages: List<GroupMessage>
    ): AssignmentHint {
        val keywords = extractKeywords(question)
        val concepts = keywords.map { "围绕「$it」的背景知识" }
        val formulas = keywords.map { "与 $it 相关的常用公式/推导" }
        val steps = listOf(
            "拆解问题条件与目标，建立量纲或变量映射",
            "列出关键公式/模型，标记未知量",
            "代入已知条件并检查单位/符号",
            "用特例或极限情况验证结果合理性"
        )
        val chapterRecommendations = mapChapters(question, courses)
        val discussions = relatedMessages.take(3).map {
            val time = Instant.ofEpochMilli(it.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            "${it.content.take(60)} · $time"
        }
        return AssignmentHint(
            relatedConcepts = concepts,
            formulas = formulas,
            solutionSteps = steps,
            chapterRecommendations = chapterRecommendations,
            relatedDiscussions = discussions
        )
    }

    private fun extractKeywords(text: String): List<String> {
        return text
            .split(" ", "，", "。", "？", "！", "、", "\n")
            .map { it.trim() }
            .filter { it.length in 2..8 }
            .groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    fun pickKeywords(text: String): List<String> = extractKeywords(text)

    fun generateSmartPlan(
        courses: List<Course>,
        assignments: List<Assignment>,
        dayCount: Int
    ): List<StudyPlanDay> {
        val today = LocalDate.now()
        val sortedAssignments = assignments
            .sortedWith(
                compareBy<Assignment> { it.dueDate }.thenByDescending { it.priority.ordinal }
            )
        var assignmentIndex = 0

        return (0 until dayCount).map { offset ->
            val date = today.plusDays(offset.toLong())
            val weekday = date.dayOfWeek.value
            val dayCourses = courses.filter { it.dayOfWeek == weekday }

            val sessions = mutableListOf<PlannedSession>()
            dayCourses.forEach { course ->
                sessions += PlannedSession(
                    label = course.courseName,
                    detail = "${course.startTime}-${course.endTime} / ${course.location.orEmpty()}",
                    timeRange = "${course.startTime}-${course.endTime}",
                    type = SessionKind.CLASS,
                    courseId = course.courseId
                )
            }

            repeat(2) {
                val assignment = sortedAssignments.getOrNull(assignmentIndex)
                if (assignment != null) {
                    sessions += PlannedSession(
                        label = assignment.title,
                        detail = "截止 ${formatDate(assignment.dueDate)} · 优先级 ${assignment.priority.name}",
                        timeRange = if (it == 0) "19:30-21:00" else "21:00-22:00",
                        type = SessionKind.ASSIGNMENT,
                        assignmentId = assignment.assignmentId,
                        courseId = assignment.courseId
                    )
                    assignmentIndex++
                }
            }

            if (sessions.size < 3) {
                sessions += PlannedSession(
                    label = "缓冲/复盘",
                    detail = "预留机动时间梳理当天重点",
                    timeRange = "22:00-22:30",
                    type = SessionKind.BUFFER
                )
            }

            val priorityAssignments = sessions
                .filter { it.type == SessionKind.ASSIGNMENT }
                .map { it.label }
            val focusScore = min(100, 60 + priorityAssignments.size * 10 + dayCourses.size * 5)

            StudyPlanDay(date, sessions, priorityAssignments, focusScore)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MM-dd"))
    }
}

