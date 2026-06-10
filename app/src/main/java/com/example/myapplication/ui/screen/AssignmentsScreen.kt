package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.myapplication.data.repository.StudyGroupRepository
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
    val groupRepository = StudyGroupRepository(database.studyGroupDao())
    val viewModel: AssignmentViewModel = viewModel(
        factory = AssignmentViewModelFactory(repository)
    )
    
    val assignments by viewModel.assignments.collectAsState()
    val userId = CurrentSession.userIdInt ?: 0
    val courses by remember(userId) {
        courseRepository.getCoursesByUser(userId)
    }.collectAsState(initial = emptyList())
    val groups by remember(userId) {
        groupRepository.getGroupsByUser(userId)
    }.collectAsState(initial = emptyList())
    val courseMap = remember(courses) { courses.associateBy { it.courseId } }
    val groupMap = remember(groups) { groups.associateBy { it.groupId } }
    var selectedStatus by remember { mutableStateOf<AssignmentStatus?>(null) }
    var searchText by remember { mutableStateOf("") }
    var courseFilter by remember(initialCourseId) { mutableStateOf(initialCourseId) }
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var assignmentToDelete by remember { mutableStateOf<com.example.myapplication.data.model.Assignment?>(null) }
    
    val priorityWeight = remember {
        mapOf(
            Priority.HIGH to 3,
            Priority.MEDIUM to 2,
            Priority.LOW to 1
        )
    }
    
    val filteredAssignments = remember(assignments, searchText, courseFilter, courseMap, selectedStatus) {
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
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AddAssignment.route) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加任务"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 状态筛选器 - 蓝白配色
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    label = { Text("全部") }
                )
                FilterChip(
                    selected = selectedStatus == AssignmentStatus.NOT_STARTED,
                    onClick = { selectedStatus = AssignmentStatus.NOT_STARTED },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    label = { Text("未开始") }
                )
                FilterChip(
                    selected = selectedStatus == AssignmentStatus.IN_PROGRESS,
                    onClick = { selectedStatus = AssignmentStatus.IN_PROGRESS },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    label = { Text("进行中") }
                )
                FilterChip(
                    selected = selectedStatus == AssignmentStatus.COMPLETED,
                    onClick = { selectedStatus = AssignmentStatus.COMPLETED },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    label = { Text("已完成") }
                )
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
                                    text = "点击右上角加号添加任务",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredAssignments) { assignment ->
                        val courseName = courseMap[assignment.courseId]?.courseName
                        val groupName = groupMap[assignment.groupId]?.groupName
                        AssignmentCard(
                            assignment = assignment,
                            courseName = courseName,
                            groupName = groupName,
                            onEdit = {
                                navController.navigate("${Screen.EditAssignment.route}/${assignment.assignmentId}")
                            },
                            onDelete = {
                                assignmentToDelete = assignment
                                showDeleteConfirmDialog = true
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

    if (showDeleteConfirmDialog && assignmentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                assignmentToDelete = null
            },
            title = {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要删除任务「${assignmentToDelete?.title}」吗？此操作无法撤销。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        assignmentToDelete?.let { viewModel.deleteAssignment(it) }
                        showDeleteConfirmDialog = false
                        assignmentToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        assignmentToDelete = null
                    }
                ) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: com.example.myapplication.data.model.Assignment,
    courseName: String?,
    groupName: String?,
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
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable { onEdit() },
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
                    val displayTitle = if (courseName != null) {
                        "${assignment.title}（$courseName）"
                    } else {
                        assignment.title
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = statusEmoji,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = displayTitle,
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
                    
                    if (groupName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "👥 $groupName",
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

