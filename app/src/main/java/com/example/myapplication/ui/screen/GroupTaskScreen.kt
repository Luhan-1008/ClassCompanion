package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.GroupTaskRepository
import com.example.myapplication.session.CurrentSession
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
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val viewModel: GroupTaskViewModel = viewModel(
        factory = GroupTaskViewModelFactory(taskRepository, groupId)
    )
    
    val tasks by viewModel.tasks.collectAsState()
    var selectedStatus by remember { mutableStateOf<TaskStatus?>(null) }
    val userId = CurrentSession.userIdInt ?: 0
    val scope = rememberCoroutineScope()
    
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
                                // TODO: 打开创建任务对话框
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "创建任务")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 状态筛选
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
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
                        selected = selectedStatus == TaskStatus.TODO,
                        onClick = { selectedStatus = TaskStatus.TODO },
                        label = { Text("待办") }
                    )
                    FilterChip(
                        selected = selectedStatus == TaskStatus.IN_PROGRESS,
                        onClick = { selectedStatus = TaskStatus.IN_PROGRESS },
                        label = { Text("进行中") }
                    )
                    FilterChip(
                        selected = selectedStatus == TaskStatus.COMPLETED,
                        onClick = { selectedStatus = TaskStatus.COMPLETED },
                        label = { Text("已完成") }
                    )
                }
            }
            
            // 任务列表
            val filteredTasks = remember(tasks, selectedStatus) {
                if (selectedStatus == null) {
                    tasks
                } else {
                    tasks.filter { it.status == selectedStatus }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredTasks.isEmpty()) {
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
                    items(filteredTasks) { task ->
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
                }
            }
        }
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
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.IN_PROGRESS -> Color(0xFFFF9800)
        TaskStatus.TODO -> MaterialTheme.colorScheme.primary
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.error
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                when (task.priority) {
                                    TaskPriority.HIGH -> "高"
                                    TaskPriority.MEDIUM -> "中"
                                    TaskPriority.LOW -> "低"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor
                            ) 
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = priorityColor.copy(alpha = 0.2f)
                        )
                    )
                }
                
                if (canEdit) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (task.status != TaskStatus.COMPLETED) {
                                DropdownMenuItem(
                                    text = { Text("标记完成") },
                                    onClick = {
                                        showMenu = false
                                        onStatusChange(TaskStatus.COMPLETED)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                )
                            }
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
            }
            
            if (!task.description.isNullOrEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            when (task.status) {
                                TaskStatus.TODO -> "待办"
                                TaskStatus.IN_PROGRESS -> "进行中"
                                TaskStatus.COMPLETED -> "已完成"
                                TaskStatus.CANCELLED -> "已取消"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        ) 
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.2f)
                    )
                )
                
                task.dueDate?.let {
                    Text(
                        text = "截止: ${dateFormat.format(Date(it))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

