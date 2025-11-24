package com.example.myapplication.ui.screen

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.StudySessionType
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.data.repository.StudySessionRepository
import com.example.myapplication.ui.viewmodel.LearningAnalyticsViewModel
import com.example.myapplication.ui.viewmodel.LearningAnalyticsViewModelFactory
import com.example.myapplication.ui.viewmodel.ReportRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningAnalyticsScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: LearningAnalyticsViewModel = viewModel(
        factory = LearningAnalyticsViewModelFactory(
            CourseRepository(database.courseDao()),
            AssignmentRepository(database.assignmentDao()),
            StudySessionRepository(database.studySessionDao()),
            GroupMessageRepository(database.groupMessageDao())
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }
    var selectedRange by remember { mutableStateOf(ReportRange.WEEK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习分析") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showLogDialog = true }) {
                        Text("记录学习")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = selectedRange == ReportRange.WEEK,
                        onClick = {
                            selectedRange = ReportRange.WEEK
                            viewModel.refreshAnalytics(ReportRange.WEEK)
                        },
                        label = { Text("近7日") }
                    )
                    FilterChip(
                        selected = selectedRange == ReportRange.MONTH,
                        onClick = {
                            selectedRange = ReportRange.MONTH
                            viewModel.refreshAnalytics(ReportRange.MONTH)
                        },
                        label = { Text("近30日") }
                    )
                }
            }

            if (uiState.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            uiState.errorMessage?.let { error ->
                item {
                    AssistChip(onClick = { viewModel.clearError() }, label = { Text(error) })
                }
            }

            item {
                MetricsCard(uiState)
            }

            item {
                TimeDistributionChart(uiState.timeDistribution)
            }

            item {
                SuggestionCard(uiState.suggestions)
            }

            if (uiState.latestSessions.isNotEmpty()) {
                item { Text("最近记录", style = MaterialTheme.typography.titleMedium) }
                items(uiState.latestSessions) { session ->
                    ListItem(
                        headlineContent = { Text(viewModel.formatSession(session)) },
                        supportingContent = { Text(session.focusTopic ?: "") }
                    )
                }
            }
        }
    }

    if (showLogDialog) {
        StudyLogDialog(
            courses = uiState.courses,
            onDismiss = { showLogDialog = false },
            onConfirm = { courseId, duration, type, topic ->
                viewModel.logStudySession(courseId, duration, type, topic)
                showLogDialog = false
            }
        )
    }
}

@Composable
private fun MetricsCard(uiState: com.example.myapplication.ui.viewmodel.LearningAnalyticsUiState) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("学习雷达", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow("累计学习时长", "${uiState.totalStudyMinutes} 分钟")
            MetricRow("任务完成率", "${uiState.completionRate}%")
            MetricRow("待完成任务", "${uiState.pendingAssignments} 项")
            MetricRow("逾期任务", "${uiState.overdueAssignments} 项")
            MetricRow("小组活跃度", "${uiState.groupActivityScore} 条讨论")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun TimeDistributionChart(distribution: List<com.example.myapplication.ui.viewmodel.CourseDistribution>) {
    if (distribution.isEmpty()) {
        Text("暂未记录学习时长，先创建一条学习日志吧。")
        return
    }
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("课程投入分布", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            val maxMinutes = distribution.maxOf { it.minutes }
            val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            val labelPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 32f
            }
            val valuePaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 32f
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val barWidth = size.width / (distribution.size * 2)
                distribution.forEachIndexed { index, data ->
                    val ratio = data.minutes / maxMinutes.toFloat()
                    val barHeight = size.height * ratio
                    val x = barWidth + index * barWidth * 2
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        data.course.courseName.take(6),
                        x,
                        size.height + 28f,
                        labelPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${data.minutes}m",
                        x,
                        size.height - barHeight - 10f,
                        valuePaint
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestions: List<String>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("个性化建议", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (suggestions.isEmpty()) {
                Text("暂无建议，先记录学习记录后即可生成。")
            } else {
                suggestions.forEachIndexed { index, suggestion ->
                    Text("${index + 1}. $suggestion", lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyLogDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (courseId: Int?, duration: Int, type: StudySessionType, topic: String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var durationText by remember { mutableStateOf("60") }
    var selectedType by remember { mutableStateOf(StudySessionType.REVIEW) }
    var topic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val duration = durationText.toIntOrNull() ?: 0
                onConfirm(selectedCourse?.courseId, duration, selectedType, topic)
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("记录学习时长") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCourse?.courseName ?: "未选择",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("关联课程") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        courses.forEach { course ->
                            DropdownMenuItem(text = { Text(course.courseName) }, onClick = {
                                selectedCourse = course
                                expanded = false
                            })
                        }
                        DropdownMenuItem(text = { Text("无具体课程") }, onClick = {
                            selectedCourse = null
                            expanded = false
                        })
                    }
                }
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("学习分钟数") },
                    modifier = Modifier.fillMaxWidth()
                )
                SingleChoiceSegmentedButtonRow {
                    val types = StudySessionType.values()
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("补充说明") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
