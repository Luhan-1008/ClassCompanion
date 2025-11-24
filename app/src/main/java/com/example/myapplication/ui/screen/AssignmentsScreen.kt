package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.model.Priority
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.AssignmentViewModel
import com.example.myapplication.ui.viewmodel.AssignmentViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    navController: NavHostController,
    initialCourseId: Int? = null
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = AssignmentRepository(database.assignmentDao())
    val courseRepository = CourseRepository(database.courseDao())
    val viewModel: AssignmentViewModel = viewModel(
        factory = AssignmentViewModelFactory(repository)
    )
    
    val assignments by viewModel.assignments.collectAsState()
    val userId = CurrentSession.userIdInt ?: 0
    val courses by remember(userId) {
        courseRepository.getCoursesByUser(userId)
    }.collectAsState(initial = emptyList())
    val courseMap = remember(courses) { courses.associateBy { it.courseId } }
    var selectedStatus by remember { mutableStateOf<AssignmentStatus?>(null) }
    var searchText by remember { mutableStateOf("") }
    var courseFilter by remember(initialCourseId) { mutableStateOf(initialCourseId) }
    
    val priorityWeight = remember {
        mapOf(
            Priority.HIGH to 3,
            Priority.MEDIUM to 2,
            Priority.LOW to 1
        )
    }
    
    val filteredAssignments = remember(assignments, searchText, courseFilter, courseMap) {
        val query = searchText.trim().lowercase()
        assignments
            .filter { assignment ->
                val matchesStatus = selectedStatus?.let { assignment.status == it } ?: true
                val matchesCourseFilter = courseFilter?.let { assignment.courseId == it } ?: true
                val matchesSearch = if (query.isEmpty()) {
                    true
                } else {
                    assignment.title.lowercase().contains(query) ||
                            (courseMap[assignment.courseId]?.courseName?.lowercase()?.contains(query) == true)
                }
                matchesStatus && matchesCourseFilter && matchesSearch
            }
            .sortedWith(
                compareBy<com.example.myapplication.data.model.Assignment> { it.dueDate }
                    .thenByDescending { priorityWeight[it.priority] ?: 0 }
            )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "任务看板",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddAssignment.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加任务")
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加任务")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 状态筛选器 - 美化版
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = selectedStatus == AssignmentStatus.NOT_STARTED,
                        onClick = { selectedStatus = AssignmentStatus.NOT_STARTED },
                        label = { Text("未开始") }
                    )
                    FilterChip(
                        selected = selectedStatus == AssignmentStatus.IN_PROGRESS,
                        onClick = { selectedStatus = AssignmentStatus.IN_PROGRESS },
                        label = { Text("进行中") }
                    )
                    FilterChip(
                        selected = selectedStatus == AssignmentStatus.COMPLETED,
                        onClick = { selectedStatus = AssignmentStatus.COMPLETED },
                        label = { Text("已完成") }
                    )
                }
            }
            
            // 搜索与筛选
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    },
                    placeholder = { Text("搜索任务或课程") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (courseFilter != null) {
                    AssistChip(
                        onClick = { courseFilter = null },
                        label = {
                            Text(
                                text = "课程筛选：${courseMap[courseFilter]?.courseName ?: "已选择"} (点击清除)"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除"
                            )
                        }
                    )
                }
            }
            
            // 任务列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredAssignments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "✅",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Text(
                                    text = "暂无任务",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "点击右下角按钮添加任务",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredAssignments) { assignment ->
                        val courseName = courseMap[assignment.courseId]?.courseName
                        AssignmentCard(
                            assignment = assignment,
                            courseName = courseName,
                            onEdit = {
                                navController.navigate("${Screen.EditAssignment.route}/${assignment.assignmentId}")
                            },
                            onDelete = {
                                viewModel.deleteAssignment(assignment)
                            },
                            onStatusChange = { status ->
                                viewModel.updateAssignmentStatus(assignment.assignmentId, status)
                            },
                            onProgressChange = { assignmentId, progressValue ->
                                viewModel.updateAssignmentProgress(assignmentId, progressValue)
                                val autoStatus = when {
                                    progressValue >= 100 -> AssignmentStatus.COMPLETED
                                    progressValue > 0 -> AssignmentStatus.IN_PROGRESS
                                    else -> AssignmentStatus.NOT_STARTED
                                }
                                viewModel.updateAssignmentStatus(assignmentId, autoStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentCard(
    assignment: com.example.myapplication.data.model.Assignment,
    courseName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (AssignmentStatus) -> Unit,
    onProgressChange: (Int, Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dueDateStr = dateFormat.format(Date(assignment.dueDate))
    
    val statusColor = when (assignment.status) {
        AssignmentStatus.COMPLETED -> Color(0xFF4CAF50)
        AssignmentStatus.OVERDUE -> MaterialTheme.colorScheme.error
        AssignmentStatus.IN_PROGRESS -> Color(0xFFFF9800)
        AssignmentStatus.NOT_STARTED -> MaterialTheme.colorScheme.primary
    }
    
    val statusEmoji = when (assignment.status) {
        AssignmentStatus.COMPLETED -> "✅"
        AssignmentStatus.OVERDUE -> "⚠️"
        AssignmentStatus.IN_PROGRESS -> "🔄"
        AssignmentStatus.NOT_STARTED -> "📝"
    }
    
    var sliderValue by remember { mutableFloatStateOf(assignment.progress.toFloat()) }
    LaunchedEffect(assignment.progress) {
        sliderValue = assignment.progress.toFloat()
    }
    val sliderProgress = (sliderValue / 100f).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = statusColor.copy(alpha = 0.1f)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左侧彩色指示条
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(statusColor)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = statusEmoji,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = assignment.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 截止时间
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⏰ $dueDateStr",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                    
                    // 关联课程显示
                    if (courseName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "📚 $courseName",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (!assignment.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = assignment.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 优先级显示
                    val priorityColor = when (assignment.priority) {
                        com.example.myapplication.data.model.Priority.HIGH -> MaterialTheme.colorScheme.error
                        com.example.myapplication.data.model.Priority.MEDIUM -> Color(0xFFFF9800)
                        com.example.myapplication.data.model.Priority.LOW -> Color(0xFF4CAF50)
                    }
                    val priorityText = when (assignment.priority) {
                        com.example.myapplication.data.model.Priority.HIGH -> "高优先级"
                        com.example.myapplication.data.model.Priority.MEDIUM -> "中优先级"
                        com.example.myapplication.data.model.Priority.LOW -> "低优先级"
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    when (assignment.type) {
                                        com.example.myapplication.data.model.AssignmentType.HOMEWORK -> "作业"
                                        com.example.myapplication.data.model.AssignmentType.EXPERIMENT -> "实验"
                                        com.example.myapplication.data.model.AssignmentType.OTHER -> "其他"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    priorityText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = priorityColor
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = priorityColor.copy(alpha = 0.2f)
                            )
                        )
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    when (assignment.status) {
                                        AssignmentStatus.COMPLETED -> "已完成"
                                        AssignmentStatus.OVERDUE -> "已逾期"
                                        AssignmentStatus.IN_PROGRESS -> "进行中"
                                        AssignmentStatus.NOT_STARTED -> "未开始"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = statusColor.copy(alpha = 0.2f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 进度调节
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "进度",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${sliderValue.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = 0f..100f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth(),
                            onValueChangeFinished = {
                                onProgressChange(assignment.assignmentId, sliderValue.roundToInt())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = statusColor,
                                activeTrackColor = statusColor
                            )
                        )
                        LinearProgressIndicator(
                            progress = sliderProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("标记为完成") },
                        onClick = {
                            showMenu = false
                            onStatusChange(AssignmentStatus.COMPLETED)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

