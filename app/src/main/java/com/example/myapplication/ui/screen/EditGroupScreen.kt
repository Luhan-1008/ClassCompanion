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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.model.StudyGroup
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.session.CurrentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    navController: NavHostController,
    groupId: Int?
) {
    if (groupId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("小组ID无效")
        }
        return
    }

    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val groupRepository = StudyGroupRepository(database.studyGroupDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val courseRepository = CourseRepository(database.courseDao())
    val assignmentRepository = AssignmentRepository(database.assignmentDao())
    val userRepository = UserRepository(database.userDao())
    val userId = CurrentSession.userIdInt ?: 0
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val courses by remember(userId) {
        if (userId > 0) {
            courseRepository.getCoursesByUser(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<com.example.myapplication.data.model.Course>())
        }
    }.collectAsState(initial = emptyList())

    val tasks by remember(userId) {
        if (userId > 0) {
            assignmentRepository.getAssignmentsByUser(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<com.example.myapplication.data.model.Assignment>())
        }
    }.collectAsState(initial = emptyList())

    var group by remember { mutableStateOf<StudyGroup?>(null) }
    var joinedCount by remember { mutableStateOf(0) }
    var hasPermission by remember { mutableStateOf(true) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var maxMembersText by remember { mutableStateOf("20") }
    var hasEditedMaxMembers by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showCourseDropdown by remember { mutableStateOf(false) }
    var showTaskDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        withContext(Dispatchers.IO) {
            val currentGroup = groupRepository.getGroupById(groupId)
            val memberCount = memberRepository.getMembersByGroup(groupId, MemberStatus.JOINED).first().size
            val currentMember = memberRepository.getMember(groupId, userId)
            val permitted = currentGroup?.creatorId == userId || currentMember?.role == MemberRole.ADMIN
            withContext(Dispatchers.Main) {
                group = currentGroup
                joinedCount = memberCount
                hasPermission = permitted
                isInitialLoading = false
                if (currentGroup != null) {
                    groupName = currentGroup.groupName
                    description = currentGroup.description ?: ""
                    topic = currentGroup.topic ?: ""
                    maxMembersText = currentGroup.maxMembers.toString()
                    isPublic = currentGroup.isPublic
                    selectedCourseId = currentGroup.courseId
                    selectedTaskId = currentGroup.taskId
                } else {
                    errorMessage = "小组不存在或已被删除"
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "编辑小组信息",
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
        when {
            isInitialLoading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            !hasPermission -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("只有创建者或管理员可以编辑小组信息")
                }
            }
            else -> {
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
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "当前成员：$joinedCount 人",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            }
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
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "关联设置",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("关联课程（可选）", style = MaterialTheme.typography.labelLarge)
                                ExposedDropdownMenuBox(
                                    expanded = showCourseDropdown,
                                    onExpandedChange = { showCourseDropdown = !showCourseDropdown }
                                ) {
                                    val selectedCourse = courses.find { it.courseId == selectedCourseId }
                                    OutlinedTextField(
                                        value = selectedCourse?.courseName ?: "未选择课程",
                                        onValueChange = {},
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

                                Text("关联任务（可选）", style = MaterialTheme.typography.labelLarge)
                                ExposedDropdownMenuBox(
                                    expanded = showTaskDropdown,
                                    onExpandedChange = { showTaskDropdown = !showTaskDropdown }
                                ) {
                                    val selectedTask = tasks.find { it.assignmentId == selectedTaskId }
                                    OutlinedTextField(
                                        value = selectedTask?.title ?: "未选择任务",
                                        onValueChange = {},
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

                                OutlinedTextField(
                                    value = maxMembersText,
                                    onValueChange = { input ->
                                        // 只要用户手动改动过，就认为进入编辑状态
                                        hasEditedMaxMembers = true
                                        maxMembersText = input.filter { it.isDigit() }.take(3)
                                    },
                                    label = { Text("最大成员数 *") },
                                    supportingText = {
                                        Text("当前成员：$joinedCount")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // 第一次获取焦点时自动清空原来的数值，方便直接输入新数字
                                        .onFocusChanged { state ->
                                            if (state.isFocused && !hasEditedMaxMembers) {
                                                hasEditedMaxMembers = true
                                                maxMembersText = ""
                                            }
                                        },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("公开小组", style = MaterialTheme.typography.bodyLarge)
                                    Switch(
                                        checked = isPublic,
                                        onCheckedChange = { isPublic = it }
                                    )
                                }
                            }
                        }

                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val trimmedName = groupName.trim()
                                if (trimmedName.isEmpty()) {
                                    errorMessage = "请输入小组名称"
                                    return@Button
                                }
                                val maxMembers = maxMembersText.toIntOrNull()
                                if (maxMembers == null || maxMembers <= 0) {
                                    errorMessage = "请输入有效的最大成员数"
                                    return@Button
                                }
                                if (maxMembers < joinedCount) {
                                    errorMessage = "最大成员数不能少于当前成员数 ($joinedCount)"
                                    return@Button
                                }
                                val currentGroup = group
                                if (currentGroup == null) {
                                    errorMessage = "小组信息未加载，请稍后重试"
                                    return@Button
                                }

                                errorMessage = null
                                scope.launch {
                                    isSaving = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            groupRepository.updateGroup(
                                                currentGroup.copy(
                                                    groupName = trimmedName,
                                                    description = description.trim().ifBlank { null },
                                                    topic = topic.trim().ifBlank { null },
                                                    maxMembers = maxMembers,
                                                    isPublic = isPublic,
                                                    courseId = selectedCourseId,
                                                    taskId = selectedTaskId
                                                )
                                            )
                                        }
                                        snackbarHostState.showSnackbar("小组信息已更新")
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        errorMessage = "保存失败：${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving && group != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "保存修改",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

