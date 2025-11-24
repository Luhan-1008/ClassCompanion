package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.model.StudyGroup
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.viewmodel.StudyGroupViewModel
import com.example.myapplication.ui.viewmodel.StudyGroupViewModelFactory
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = StudyGroupRepository(database.studyGroupDao())
    val courseRepository = CourseRepository(database.courseDao())
    val assignmentRepository = AssignmentRepository(database.assignmentDao())
    val viewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(repository)
    )
    val userId = CurrentSession.userIdInt ?: 0
    val courses by remember(userId) {
        courseRepository.getCoursesByUser(userId)
    }.collectAsState(initial = emptyList())
    val tasks by remember(userId) {
        assignmentRepository.getAssignmentsByUser(userId)
    }.collectAsState(initial = emptyList())
    
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf(20) }
    var isPublic by remember { mutableStateOf(true) }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showCourseDropdown by remember { mutableStateOf(false) }
    var showTaskDropdown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "创建学习小组",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
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
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("小组名称 *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("小组描述") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text("主题") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        // 关联课程
                        Text(
                            text = "关联课程（可选）",
                            style = MaterialTheme.typography.labelLarge
                        )
                        ExposedDropdownMenuBox(
                            expanded = showCourseDropdown,
                            onExpandedChange = { showCourseDropdown = !showCourseDropdown }
                        ) {
                            val selectedCourse = courses.find { it.courseId == selectedCourseId }
                            OutlinedTextField(
                                value = selectedCourse?.courseName ?: "未选择课程",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("选择课程") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCourseDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showCourseDropdown,
                                onDismissRequest = { showCourseDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("不关联课程") },
                                    onClick = {
                                        selectedCourseId = null
                                        showCourseDropdown = false
                                    }
                                )
                                courses.forEach { course ->
                                    DropdownMenuItem(
                                        text = { Text(course.courseName) },
                                        onClick = {
                                            selectedCourseId = course.courseId
                                            showCourseDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 关联任务
                        Text(
                            text = "关联任务（可选）",
                            style = MaterialTheme.typography.labelLarge
                        )
                        ExposedDropdownMenuBox(
                            expanded = showTaskDropdown,
                            onExpandedChange = { showTaskDropdown = !showTaskDropdown }
                        ) {
                            val selectedTask = tasks.find { it.assignmentId == selectedTaskId }
                            OutlinedTextField(
                                value = selectedTask?.title ?: "未选择任务",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("选择任务") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTaskDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showTaskDropdown,
                                onDismissRequest = { showTaskDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("不关联任务") },
                                    onClick = {
                                        selectedTaskId = null
                                        showTaskDropdown = false
                                    }
                                )
                                tasks
                                    .filter { it.status != AssignmentStatus.COMPLETED }
                                    .forEach { task ->
                                        DropdownMenuItem(
                                            text = { Text(task.title) },
                                            onClick = {
                                                selectedTaskId = task.assignmentId
                                                showTaskDropdown = false
                                            }
                                        )
                                    }
                            }
                        }
                    }
                }
                
                // 设置卡片
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
                            text = "小组设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = maxMembers.toString(),
                            onValueChange = { maxMembers = it.toIntOrNull() ?: 20 },
                            label = { Text("最大成员数") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "公开小组",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            scope.launch {
                                val group = StudyGroup(
                                    creatorId = userId,
                                    groupName = groupName,
                                    description = description.ifBlank { null },
                                    courseId = selectedCourseId,
                                    taskId = selectedTaskId,
                                    topic = topic.ifBlank { null },
                                    maxMembers = maxMembers,
                                    isPublic = isPublic
                                )
                                val groupIdLong = repository.insertGroup(group)
                                val groupId = groupIdLong.toInt()
                                
                                // 如果关联了任务，创建对应的GroupTask
                                val taskId = selectedTaskId
                                if (taskId != null) {
                                    val assignment = assignmentRepository.getAssignmentById(taskId)
                                    if (assignment != null) {
                                        val groupTaskRepository = com.example.myapplication.data.repository.GroupTaskRepository(database.groupTaskDao())
                                        val groupTask = com.example.myapplication.data.model.GroupTask(
                                            groupId = groupId,
                                            creatorId = userId,
                                            assigneeId = null,
                                            title = assignment.title,
                                            description = assignment.description,
                                            status = when (assignment.status) {
                                                com.example.myapplication.data.model.AssignmentStatus.NOT_STARTED -> com.example.myapplication.data.model.TaskStatus.TODO
                                                com.example.myapplication.data.model.AssignmentStatus.IN_PROGRESS -> com.example.myapplication.data.model.TaskStatus.IN_PROGRESS
                                                com.example.myapplication.data.model.AssignmentStatus.COMPLETED -> com.example.myapplication.data.model.TaskStatus.COMPLETED
                                                com.example.myapplication.data.model.AssignmentStatus.OVERDUE -> com.example.myapplication.data.model.TaskStatus.TODO
                                            },
                                            priority = when (assignment.priority) {
                                                com.example.myapplication.data.model.Priority.LOW -> com.example.myapplication.data.model.TaskPriority.LOW
                                                com.example.myapplication.data.model.Priority.MEDIUM -> com.example.myapplication.data.model.TaskPriority.MEDIUM
                                                com.example.myapplication.data.model.Priority.HIGH -> com.example.myapplication.data.model.TaskPriority.HIGH
                                            },
                                            dueDate = assignment.dueDate,
                                            createdAt = System.currentTimeMillis(),
                                            completedAt = if (assignment.status == com.example.myapplication.data.model.AssignmentStatus.COMPLETED) System.currentTimeMillis() else null
                                        )
                                        groupTaskRepository.insertTask(groupTask)
                                    }
                                }
                                
                                navController.popBackStack()
                            }
                        }
                    },
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
                        text = "创建小组",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

