package com.example.myapplication.ui.screen

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.StudySessionType
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.data.repository.StudySessionRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.RemoteUserRepository
import com.example.myapplication.session.TokenManager
import com.example.myapplication.ui.viewmodel.LearningAnalyticsViewModel
import com.example.myapplication.ui.viewmodel.LearningAnalyticsViewModelFactory
import com.example.myapplication.ui.viewmodel.ReportRange
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.example.myapplication.ui.viewmodel.UserViewModelFactory
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.session.CurrentSession
import kotlinx.coroutines.launch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import java.util.Calendar
import java.util.Date

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
    val userRepository = remember { UserRepository(database.userDao()) }
    val remoteRepository = remember { null as RemoteUserRepository? }
    val tokenManager = remember { TokenManager(context) }
    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(userRepository, remoteRepository, tokenManager)
    )
    val currentUser by userViewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<com.example.myapplication.data.model.StudySession?>(null) }
    var selectedRange by remember { mutableStateOf(ReportRange.WEEK) }
    val scope = rememberCoroutineScope()
    
    // 加载当前用户信息
    LaunchedEffect(Unit) {
        userViewModel.loadCurrentUser()
    }

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
                    TextButton(onClick = { 
                        editingSession = null
                        showLogDialog = true 
                    }) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedRange == ReportRange.WEEK,
                        onClick = {
                            selectedRange = ReportRange.WEEK
                            viewModel.refreshAnalytics(ReportRange.WEEK)
                        },
                        label = { Text("近7日") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRange == ReportRange.MONTH,
                        onClick = {
                            selectedRange = ReportRange.MONTH
                            viewModel.refreshAnalytics(ReportRange.MONTH)
                        },
                        label = { Text("近30日") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRange == ReportRange.QUARTER,
                        onClick = {
                            selectedRange = ReportRange.QUARTER
                            viewModel.refreshAnalytics(ReportRange.QUARTER)
                        },
                        label = { Text("近90日") },
                        modifier = Modifier.weight(1f)
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
                SuggestionCard(
                    suggestions = uiState.suggestions,
                    dailyTrend = uiState.dailyTrend,
                    timeDistribution = uiState.timeDistribution
                )
            }

            if (uiState.latestSessions.isNotEmpty()) {
                item {
                    Text(
                        "📝 最近记录",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(uiState.latestSessions.take(5)) { session ->
                    val courseName = uiState.courses.find { it.courseId == session.courseId }?.courseName
                    RecentSessionCard(
                        session = session,
                        courseName = courseName,
                        onEdit = {
                            editingSession = session
                            showLogDialog = true
                        },
                        onDelete = {
                            viewModel.deleteStudySession(session)
                        }
                    )
                }
            }
        }
    }

    if (showLogDialog) {
        StudyLogDialog(
            courses = uiState.courses,
            initialSession = editingSession,
            onDismiss = { 
                showLogDialog = false 
                editingSession = null
            },
            onConfirm = { courseId, duration, type, topic, date ->
                if (editingSession != null) {
                    viewModel.updateStudySession(
                        editingSession!!.copy(
                            courseId = courseId,
                            durationMinutes = duration,
                            sessionType = type,
                            focusTopic = topic,
                            sessionDate = date
                        )
                    )
                } else {
                    viewModel.logStudySession(courseId, duration, type, topic, date)
                }
                showLogDialog = false
                editingSession = null
            }
        )
    }
}

@Composable
private fun MetricsCard(uiState: com.example.myapplication.ui.viewmodel.LearningAnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📊 学习概览",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(
                    label = "总时长",
                    value = "${uiState.totalStudyMinutes}",
                    unit = "分钟",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                MetricBox(
                    label = "完成率",
                    value = "${uiState.completionRate}",
                    unit = "%",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(
                    label = "待完成",
                    value = "${uiState.pendingAssignments}",
                    unit = "项",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
                MetricBox(
                    label = "逾期",
                    value = "${uiState.overdueAssignments}",
                    unit = "项",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📚 课程投入分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
    if (distribution.isEmpty()) {
                Text(
                    "暂未记录学习时长，先创建一条学习日志吧。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
            val maxMinutes = distribution.maxOf { it.minutes }
                val primaryColor = MaterialTheme.colorScheme.primary
                val barColor = primaryColor.copy(alpha = 0.8f)
            val labelPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
            }
            val valuePaint = Paint().apply {
                    color = primaryColor.hashCode()
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    val padding = 50.dp.toPx()
                    val chartWidth = size.width - padding * 2
                    val chartHeight = size.height - padding * 2
                    val barWidth = (chartWidth / distribution.size) * 0.6f
                    val barSpacing = (chartWidth / distribution.size) * 0.4f
                    
                    distribution.forEachIndexed { index, data ->
                        val ratio = data.minutes / maxMinutes.toFloat()
                        val barHeight = chartHeight * ratio
                        val x = padding + index * (barWidth + barSpacing) + barSpacing / 2
                        val y = padding + chartHeight - barHeight
                        
                        // 绘制柱状图
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                        
                        // 绘制课程名称
                        drawContext.canvas.nativeCanvas.drawText(
                            data.course.courseName.take(4),
                            x + barWidth / 2,
                            size.height - 8.dp.toPx(),
                            labelPaint
                        )
                        
                        // 绘制数值
                        if (barHeight > 30.dp.toPx()) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${data.minutes}m",
                                x + barWidth / 2,
                                y - 8.dp.toPx(),
                                valuePaint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTrendChart(trend: List<com.example.myapplication.ui.viewmodel.DailyStudyData>) {
    if (trend.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📈 学习趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            val maxMinutes = trend.maxOfOrNull { it.minutes } ?: 1
            val minMinutes = trend.minOfOrNull { it.minutes } ?: 0
            val range = (maxMinutes - minMinutes).coerceAtLeast(1)
            val primaryColor = MaterialTheme.colorScheme.primary
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val padding = 40.dp.toPx()
                val chartWidth = size.width - padding * 2
                val chartHeight = size.height - padding * 2
                val pointSpacing = chartWidth / (trend.size - 1).coerceAtLeast(1)
                
                // 绘制网格线
                for (i in 0..4) {
                    val y = padding + (chartHeight / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(padding, y),
                        end = Offset(size.width - padding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // 绘制折线
                val points = trend.mapIndexed { index, data ->
                    val x = padding + pointSpacing * index
                    val normalizedValue = (data.minutes - minMinutes).toFloat() / range
                    val y = padding + chartHeight * (1 - normalizedValue)
                    Offset(x, y)
                }
                
                // 绘制连接线
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3.dp.toPx()
                    )
                }
                
                // 绘制数据点
                points.forEach { point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = point
                    )
                }
                
                // 绘制标签
                trend.forEachIndexed { index, data ->
                    val x = padding + pointSpacing * index
                    val normalizedValue = (data.minutes - minMinutes).toFloat() / range
                    val y = padding + chartHeight * (1 - normalizedValue)
                    
                    // 日期标签
                    val datePaint = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        data.date,
                        x,
                        size.height - 8.dp.toPx(),
                        datePaint
                    )
                    
                    // 数值标签
                    if (data.minutes > 0) {
                        val valuePaint = Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = 28f
                            textAlign = Paint.Align.CENTER
                        }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${data.minutes}m",
                        x,
                            y - 12.dp.toPx(),
                        valuePaint
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyHeatmapCard(heatmap: List<com.example.myapplication.ui.viewmodel.WeeklyHeatmapData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "🔥 周学习热力图",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            val maxMinutes = heatmap.maxOfOrNull { it.totalMinutes } ?: 1
            val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                heatmap.forEachIndexed { index, data ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val intensity = if (maxMinutes > 0) (data.totalMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f) else 0f
                        val color = when {
                            intensity > 0.7f -> MaterialTheme.colorScheme.primary
                            intensity > 0.4f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            intensity > 0.1f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(
                                    color = color,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${data.totalMinutes}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (intensity > 0.5f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = dayNames[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestions: List<String>,
    dailyTrend: List<com.example.myapplication.ui.viewmodel.DailyStudyData>,
    timeDistribution: List<com.example.myapplication.ui.viewmodel.CourseDistribution>
) {
    Text(
        text = "💡 学习分析与建议",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    // 学习趋势图
    if (dailyTrend.isNotEmpty()) {
        DailyTrendChart(dailyTrend)
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // 课程投入分布
    TimeDistributionChart(timeDistribution)
    
    // 个性化建议
    if (suggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💡 个性化建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                suggestions.forEachIndexed { index, suggestion ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            suggestion,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSessionCard(
    session: com.example.myapplication.data.model.StudySession,
    courseName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeNames = mapOf(
        com.example.myapplication.data.model.StudySessionType.PREVIEW to "预习",
        com.example.myapplication.data.model.StudySessionType.REVIEW to "复习",
        com.example.myapplication.data.model.StudySessionType.ASSIGNMENT to "作业",
        com.example.myapplication.data.model.StudySessionType.DISCUSSION to "讨论",
        com.example.myapplication.data.model.StudySessionType.EXAM_PREP to "备考"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
                val dateTime = java.time.Instant.ofEpochMilli(session.sessionDate)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
                val timeStr = formatter.format(dateTime)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (courseName != null) {
                        Text(
                            text = " · $courseName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = "${typeNames[session.sessionType] ?: ""} · ${session.durationMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (!session.focusTopic.isNullOrBlank()) {
                    Text(
                        text = session.focusTopic ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyLogDialog(
    courses: List<Course>,
    initialSession: com.example.myapplication.data.model.StudySession? = null,
    onDismiss: () -> Unit,
    onConfirm: (courseId: Int?, duration: Int, type: StudySessionType, topic: String?, date: Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var durationText by remember { mutableStateOf("60") }
    var selectedType by remember { mutableStateOf(StudySessionType.REVIEW) }
    var topic by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Initialize state if editing
    LaunchedEffect(initialSession) {
        if (initialSession != null) {
            selectedCourse = courses.find { it.courseId == initialSession.courseId }
            durationText = initialSession.durationMinutes.toString()
            selectedType = initialSession.sessionType
            topic = initialSession.focusTopic ?: ""
            selectedDate = initialSession.sessionDate
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            selectedDate = calendar.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )
    
    val typeNames = mapOf(
        StudySessionType.PREVIEW to "预习",
        StudySessionType.REVIEW to "复习",
        StudySessionType.ASSIGNMENT to "作业",
        StudySessionType.DISCUSSION to "讨论",
        StudySessionType.EXAM_PREP to "备考"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "📚",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Text(
                        if (initialSession != null) "编辑学习记录" else "记录学习时长",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 日期和时间选择
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "日期和时间",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            Text(dateFormatter.format(Date(selectedDate)))
                        }
                        OutlinedButton(
                            onClick = { timePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            Text(timeFormatter.format(Date(selectedDate)))
                        }
                    }
                }

                // 关联课程
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "关联课程",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                    OutlinedTextField(
                            value = selectedCourse?.courseName ?: "无具体课程",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("无具体课程") },
                                onClick = {
                                    selectedCourse = null
                                    expanded = false
                                }
                            )
                        courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course.courseName) },
                                    onClick = {
                                selectedCourse = course
                                expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // 学习时长
                val duration = durationText.toIntOrNull() ?: 60
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "学习时长",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$duration",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "分钟",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    // 滑块
                    val durationValue = durationText.toFloatOrNull() ?: 60f
                    val durationRange = 15f..480f // 15分钟到8小时
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Slider(
                            value = durationValue.coerceIn(durationRange),
                            onValueChange = { durationText = it.roundToInt().toString() },
                            valueRange = durationRange,
                            steps = 30, // 每15分钟一个步长
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "15分钟",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "8小时",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 时长换算显示
                    val hours = duration / 60
                    val mins = duration % 60
                    if (hours > 0 || mins > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hours > 0) {
                                    Text(
                                        "$hours 小时",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (mins > 0) {
                                        Text(
                                            " $mins 分钟",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        "$mins 分钟",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 学习类型
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "学习类型",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudySessionType.values().forEach { type ->
                            FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                                label = { Text(typeNames[type] ?: type.name) },
                                modifier = Modifier.weight(1f)
                        )
                        }
                    }
                }
                
                // 补充说明
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("补充说明（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    placeholder = { Text("例如：复习了第三章内容、完成了作业1-5题...") }
                )
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 0
                            if (duration > 0) {
                                onConfirm(selectedCourse?.courseId, duration, selectedType, topic.ifBlank { null }, selectedDate)
                            }
                        },
                        enabled = durationText.toIntOrNull()?.let { it > 0 } == true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("保存记录", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
