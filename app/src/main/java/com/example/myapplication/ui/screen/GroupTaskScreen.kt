package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.*
import androidx.compose.material.icons.filled.CalendarToday
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.GroupTaskRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.GroupTaskViewModel
import com.example.myapplication.ui.viewmodel.GroupTaskViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTaskScreen(
    navController: NavHostController,
    groupId: Int
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val taskRepository = GroupTaskRepository(database.groupTaskDao())
    val assignmentRepository = AssignmentRepository(database.assignmentDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val viewModel: GroupTaskViewModel = viewModel(
        factory = GroupTaskViewModelFactory(taskRepository, assignmentRepository, groupId)
    )
    
    val tasks by viewModel.tasks.collectAsState()
    val groupAssignments by viewModel.groupAssignments.collectAsState()
    var selectedStatus by remember { mutableStateOf<TaskStatus?>(null) }
    val userId = CurrentSession.userIdInt ?: 0
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // 检查用户权限
    var userRole by remember { mutableStateOf<MemberRole?>(null) }
    LaunchedEffect(groupId, userId) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val member = memberRepository.getMember(groupId, userId)
            userRole = member?.role
        }
    }
    
    val canCreate = userRole == MemberRole.CREATOR || userRole == MemberRole.ADMIN
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "小组任务",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (canCreate) {
                        IconButton(
                            onClick = {
                                showCreateDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "创建任务")
                        }
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
                    selected = selectedStatus == TaskStatus.TODO,
                    onClick = { selectedStatus = TaskStatus.TODO },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    label = { Text("未完成") }
                )
                FilterChip(
                    selected = selectedStatus == TaskStatus.IN_PROGRESS,
                    onClick = { selectedStatus = TaskStatus.IN_PROGRESS },
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
                    selected = selectedStatus == TaskStatus.COMPLETED,
                    onClick = { selectedStatus = TaskStatus.COMPLETED },
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
            
            // 任务列表
            val filteredTasks = remember(tasks, selectedStatus) {
                if (selectedStatus == null) {
                    tasks
                } else {
                    tasks.filter { it.status == selectedStatus }
                }
            }
            val filteredAssignments = remember(groupAssignments, selectedStatus) {
                groupAssignments.filter { assignment ->
                    selectedStatus?.let { assignmentStatusToTaskStatus(assignment.status) == it } ?: true
                }
            }
            val combinedItems = remember(filteredTasks, filteredAssignments) {
                val items = mutableListOf<GroupTaskItem>()
                filteredTasks.forEach { items += GroupTaskItem.Native(it) }
                filteredAssignments.forEach { items += GroupTaskItem.Linked(it) }
                items
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (combinedItems.isEmpty()) {
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
                            }
                        }
                    }
                } else {
                    items(
                        items = combinedItems,
                        key = { it.key }
                    ) { item ->
                        when (item) {
                            is GroupTaskItem.Native -> {
                                val task = item.task
                                TaskCard(
                                    task = task,
                                    canEdit = canCreate || task.assigneeId == userId,
                                    onStatusChange = { newStatus ->
                                        scope.launch {
                                            viewModel.updateTaskStatus(
                                                task.taskId,
                                                newStatus,
                                                if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
                                            )
                                        }
                                    },
                                    onDelete = {
                                        if (canCreate) {
                                            scope.launch {
                                                viewModel.deleteTask(task)
                                            }
                                        }
                                    }
                                )
                            }
                            is GroupTaskItem.Linked -> {
                                LinkedAssignmentCard(
                                    assignment = item.assignment,
                                    onOpen = {
                                        navController.navigate("${Screen.EditAssignment.route}/${item.assignment.assignmentId}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 创建任务对话框
    if (showCreateDialog) {
        CreateTaskDialog(
            groupId = groupId,
            creatorId = userId,
            onDismiss = { showCreateDialog = false },
            onConfirm = { task ->
                scope.launch {
                    viewModel.createTask(task)
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun LinkedAssignmentCard(
    assignment: Assignment,
    onOpen: () -> Unit
) {
    val statusColor = when (assignment.status) {
        AssignmentStatus.COMPLETED -> Color(0xFF4CAF50)
        AssignmentStatus.OVERDUE -> MaterialTheme.colorScheme.error
        AssignmentStatus.IN_PROGRESS -> Color(0xFFFF9800)
        AssignmentStatus.NOT_STARTED -> MaterialTheme.colorScheme.primary
    }
    val statusText = when (assignment.status) {
        AssignmentStatus.COMPLETED -> "已完成"
        AssignmentStatus.OVERDUE -> "已逾期"
        AssignmentStatus.IN_PROGRESS -> "进行中"
        AssignmentStatus.NOT_STARTED -> "未开始"
    }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!assignment.description.isNullOrEmpty()) {
                        Text(
                            text = assignment.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                StatusBadge(text = statusText, color = statusColor)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(assignment.dueDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpen) {
                    Text("查看任务")
                }
            }
        }
    }
}

private sealed class GroupTaskItem(val key: String) {
    class Native(val task: GroupTask) : GroupTaskItem("task_${task.taskId}")
    class Linked(val assignment: Assignment) : GroupTaskItem("assignment_${assignment.assignmentId}")
}

private fun assignmentStatusToTaskStatus(status: AssignmentStatus): TaskStatus {
    return when (status) {
        AssignmentStatus.NOT_STARTED -> TaskStatus.TODO
        AssignmentStatus.IN_PROGRESS -> TaskStatus.IN_PROGRESS
        AssignmentStatus.COMPLETED -> TaskStatus.COMPLETED
        AssignmentStatus.OVERDUE -> TaskStatus.IN_PROGRESS
    }
}

@Composable
fun TaskCard(
    task: GroupTask,
    canEdit: Boolean,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val statusDisplay = remember(task.status) {
        when (task.status) {
            TaskStatus.TODO -> "未完成"
            TaskStatus.IN_PROGRESS -> "进行中"
            TaskStatus.COMPLETED -> "已完成"
            TaskStatus.CANCELLED -> "已取消"
        }
    }
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.IN_PROGRESS -> Color(0xFFFF9800)
        TaskStatus.TODO -> MaterialTheme.colorScheme.primary
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    val priorityText = when (task.priority) {
        TaskPriority.HIGH -> "高"
        TaskPriority.MEDIUM -> "中"
        TaskPriority.LOW -> "低"
    }
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
        TaskPriority.MEDIUM -> Color(0xFFFF9800)
        TaskPriority.LOW -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!task.description.isNullOrBlank()) {
                        Text(
                            text = task.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                StatusBadge(text = statusDisplay, color = statusColor)
                if (canEdit) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.dueDate?.let { dateFormat.format(Date(it)) } ?: "无截止",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(text = "优先级 $priorityText", color = priorityColor.copy(alpha = 0.8f))
            }
        }
    }
    if (canEdit) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 状态切换选项
            when (task.status) {
                TaskStatus.TODO -> {
                    // 待办 -> 可以切换到进行中或已完成
                    DropdownMenuItem(
                        text = { Text("标记为进行中") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.IN_PROGRESS)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("标记为已完成") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.COMPLETED)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    )
                }
                TaskStatus.IN_PROGRESS -> {
                    // 进行中 -> 可以切换回待办或标记为已完成
                    DropdownMenuItem(
                        text = { Text("标记为待办") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.TODO)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("标记为已完成") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.COMPLETED)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    )
                }
                TaskStatus.COMPLETED -> {
                    // 已完成 -> 可以切换回进行中或待办
                    DropdownMenuItem(
                        text = { Text("标记为进行中") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.IN_PROGRESS)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("标记为待办") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.TODO)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    )
                }
                TaskStatus.CANCELLED -> {
                    // 已取消 -> 可以恢复为待办
                    DropdownMenuItem(
                        text = { Text("恢复为待办") },
                        onClick = {
                            showMenu = false
                            onStatusChange(TaskStatus.TODO)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    )
                }
            }
            
            Divider()
            
            // 删除选项
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.15f),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(
    groupId: Int,
    creatorId: Int,
    onDismiss: () -> Unit,
    onConfirm: (GroupTask) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf(Date()) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dueDateDisplay = remember(dueDate) { dateFormatter.format(dueDate) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f),
        title = {
            Text(
                text = "创建任务",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 基本信息卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
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
                            text = "基本信息",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("任务标题 *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "优先级",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TaskPriority.values().forEach { priorityOption ->
                                        val isSelected = priority == priorityOption
                                        val displayName = when (priorityOption) {
                                            TaskPriority.LOW -> "低"
                                            TaskPriority.MEDIUM -> "中"
                                            TaskPriority.HIGH -> "高"
                                        }
                                        CompactChip(
                                            text = displayName,
                                            isSelected = isSelected,
                                            selectedColor = MaterialTheme.colorScheme.secondaryContainer,
                                            unselectedColor = MaterialTheme.colorScheme.surfaceVariant,
                                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            horizontalPadding = 8.dp
                                        ) {
                                            priority = priorityOption
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("任务描述") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "截止时间",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                com.example.myapplication.ui.components.showDateTimePicker(context, dueDate) { selected ->
                                                    dueDate = selected
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = dueDateDisplay,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "点击选择截止日期和时间",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            com.example.myapplication.ui.components.showDateTimePicker(context, dueDate) { selected ->
                                                dueDate = selected
                                            }
                                        }
                                    ) {
                                        Text("调整")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val task = GroupTask(
                            groupId = groupId,
                            creatorId = creatorId,
                            assigneeId = null,
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            status = TaskStatus.TODO,
                            priority = priority,
                            dueDate = dueDate.time,
                            createdAt = System.currentTimeMillis(),
                            completedAt = null
                        )
                        onConfirm(task)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "保存任务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CompactChip(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    horizontalPadding: Dp,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) selectedColor else unselectedColor
    val contentColor = if (isSelected) selectedTextColor else unselectedTextColor
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() },
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        contentColor = contentColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            maxLines = 1
        )
    }
}

