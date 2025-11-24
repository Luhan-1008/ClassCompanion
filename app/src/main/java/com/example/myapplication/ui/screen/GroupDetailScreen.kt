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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.viewmodel.StudyGroupViewModel
import com.example.myapplication.ui.viewmodel.StudyGroupViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(navController: NavHostController, groupId: Int?) {
    if (groupId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
    val notificationRepository = NotificationRepository(database.notificationDao())
    val viewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(groupRepository)
    )
    
    val userId = CurrentSession.userIdInt ?: 0
    var group by remember { mutableStateOf<com.example.myapplication.data.model.StudyGroup?>(null) }
    var relatedCourse by remember { mutableStateOf<Course?>(null) }
    var relatedTask by remember { mutableStateOf<Assignment?>(null) }
    val members by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.JOINED)
    }.collectAsState(initial = emptyList())
    
    // 待审核的申请
    val pendingMembers by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.PENDING)
    }.collectAsState(initial = emptyList())
    
    // 过滤掉申请者自己的申请（申请者不能看到自己的申请）
    val pendingApplications = remember(pendingMembers, userId) {
        pendingMembers.filter { it.userId != userId }
    }
    
    // 获取所有成员的用户信息（用于显示用户名）
    val memberUserMap by produceState<Map<Int, com.example.myapplication.data.model.User>>(
        initialValue = emptyMap(),
        key1 = members, pendingApplications
    ) {
        val map = mutableMapOf<Int, com.example.myapplication.data.model.User>()
        withContext(Dispatchers.IO) {
            // 获取已加入成员的用户信息
            members.forEach { member ->
                if (!map.containsKey(member.userId)) {
                    userRepository.getUserById(member.userId)?.let { user ->
                        map[member.userId] = user
                    }
                }
            }
            // 获取待审核申请的用户信息
            pendingApplications.forEach { member ->
                if (!map.containsKey(member.userId)) {
                    userRepository.getUserById(member.userId)?.let { user ->
                        map[member.userId] = user
                    }
                }
            }
        }
        value = map
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteInput by remember { mutableStateOf("") }
    var inviteDialogError by remember { mutableStateOf<String?>(null) }
    var isInviting by remember { mutableStateOf(false) }
    
    LaunchedEffect(groupId) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            group = groupRepository.getGroupById(groupId)
        }
    }
    
    val currentMember = remember(members, userId) { members.find { it.userId == userId } }
    val isCreator = remember(group, userId) { group?.creatorId == userId }
    val isAdmin = remember(currentMember) { currentMember?.role == MemberRole.ADMIN }
    val canInvite = remember(isCreator, isAdmin) { isCreator || isAdmin }
    
    LaunchedEffect(group?.courseId) {
        relatedCourse = group?.courseId?.let { courseId ->
            withContext(Dispatchers.IO) { courseRepository.getCourseById(courseId) }
        }
    }
    LaunchedEffect(group?.taskId) {
        relatedTask = group?.taskId?.let { taskId ->
            withContext(Dispatchers.IO) { assignmentRepository.getAssignmentById(taskId) }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = group?.groupName ?: "小组详情",
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
                )
            )
        },
        floatingActionButton = {
            if (canInvite) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("group_invite/$groupId")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "邀请成员",
                        tint = Color.White
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 功能入口卡片
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                group?.let {
                    item {
                        GroupAssociationCard(
                            course = relatedCourse,
                            task = relatedTask
                        )
                    }
                }
                
                item {
                    // 群聊入口
                    FeatureCard(
                        title = "群聊",
                        description = "与组员交流讨论",
                        icon = Icons.Default.Chat,
                        onClick = {
                            navController.navigate("group_chat/$groupId")
                        }
                    )
                }
                
                item {
                    // 文件库入口
                    FeatureCard(
                        title = "文件库",
                        description = "共享学习资料和文件",
                        icon = Icons.Default.Folder,
                        onClick = {
                            navController.navigate("group_files/$groupId")
                        }
                    )
                }
                
                item {
                    // 公告板入口
                    FeatureCard(
                        title = "公告板",
                        description = "查看小组公告和通知",
                        icon = Icons.Default.Notifications,
                        onClick = {
                            navController.navigate("group_announcements/$groupId")
                        }
                    )
                }
                
                item {
                    // 小组任务入口
                    FeatureCard(
                        title = "小组任务",
                        description = "创建和跟踪小组任务",
                        icon = Icons.Default.Assignment,
                        onClick = {
                            navController.navigate("group_tasks/$groupId")
                        }
                    )
                }
                
                // 邀请成员功能 - 所有成员都可以看到，但只有创建者/管理员能创建邀请
                item {
                    val currentMember = members.find { it.userId == userId }
                    val isCreator = group?.creatorId == userId
                    val isAdmin = currentMember?.role == MemberRole.ADMIN
                    val canInvite = isCreator || isAdmin
                    
                    FeatureCard(
                        title = "邀请成员",
                        description = if (canInvite) "生成邀请码和二维码" else "仅创建者和管理员可邀请",
                        icon = Icons.Default.PersonAdd,
                        onClick = {
                            if (canInvite) {
                                navController.navigate("group_invite/$groupId")
                            } else {
                                // 可以显示一个提示
                            }
                        },
                        enabled = canInvite
                    )
                }
                
                // 待审核申请（仅创建者和管理员可见，且不包含申请者自己的申请）
                if (canInvite && pendingApplications.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                                    Text(
                                        text = "待审核申请 (${pendingApplications.size})",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // 显示待审核的申请（已过滤掉申请者自己的申请）
                                pendingApplications.forEach { pendingMember ->
                                    val applicantUser = memberUserMap[pendingMember.userId]
                                    PendingMemberItem(
                                        member = pendingMember,
                                        userName = applicantUser?.username ?: applicantUser?.realName ?: "用户${pendingMember.userId}",
                                        onApprove = {
                                            scope.launch {
                                                memberRepository.updateMemberStatus(
                                                    groupId,
                                                    pendingMember.userId,
                                                    MemberStatus.JOINED
                                                )
                                                snackbarHostState.showSnackbar("已同意加入")
                                            }
                                        },
                                        onReject = {
                                            scope.launch {
                                                memberRepository.updateMemberStatus(
                                                    groupId,
                                                    pendingMember.userId,
                                                    MemberStatus.LEFT
                                                )
                                                snackbarHostState.showSnackbar("已拒绝申请")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    // 成员列表
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
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "小组成员",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${members.size}人",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (canInvite) {
                                        TextButton(onClick = { showInviteDialog = true }) {
                                            Text("直接邀请")
                                        }
                                    }
                                }
                            }
                            
                            // 成员列表（按角色排序）
                            val sortedMembers = members.sortedBy { member ->
                                when (member.role) {
                                    MemberRole.CREATOR -> 0
                                    MemberRole.ADMIN -> 1
                                    MemberRole.MEMBER -> 2
                                }
                            }
                            sortedMembers.forEach { member ->
                                val memberUser = memberUserMap[member.userId]
                                MemberItem(
                                    member = member,
                                    userName = memberUser?.username ?: memberUser?.realName ?: "用户${member.userId}",
                                    currentUserId = userId,
                                    currentUserRole = currentMember?.role,
                                    isCreator = isCreator,
                                    groupCreatorId = group?.creatorId,
                                    onRoleChange = { newRole ->
                                        scope.launch {
                                            memberRepository.updateMember(
                                                member.copy(role = newRole)
                                            )
                                            snackbarHostState.showSnackbar("已更新成员权限")
                                        }
                                    },
                                    onRemove = {
                                        scope.launch {
                                            memberRepository.updateMemberStatus(
                                                member.groupId,
                                                member.userId,
                                                MemberStatus.LEFT
                                            )
                                            snackbarHostState.showSnackbar("已移除成员")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isInviting) {
                    showInviteDialog = false
                    inviteDialogError = null
                }
            },
            title = {
                Text("直接邀请成员")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it },
                        label = { Text("用户名或学号") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "输入需要邀请的同学的用户名或学号，对方会收到通知。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    inviteDialogError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = inviteInput.isNotBlank() && !isInviting,
                    onClick = {
                        scope.launch {
                            isInviting = true
                            inviteDialogError = null
                            val keyword = inviteInput.trim()
                            val targetUser = withContext(Dispatchers.IO) {
                                userRepository.getUserByStudentId(keyword)
                                    ?: userRepository.getUserByUsername(keyword)
                            }
                            if (targetUser == null) {
                                inviteDialogError = "未找到该用户"
                                isInviting = false
                                return@launch
                            }
                            if (targetUser.userId == userId) {
                                inviteDialogError = "不能邀请自己"
                                isInviting = false
                                return@launch
                            }
                            val existing = withContext(Dispatchers.IO) {
                                memberRepository.getMember(groupId, targetUser.userId)
                            }
                            when {
                                existing == null -> {
                                    memberRepository.insertMember(
                                        GroupMember(
                                            groupId = groupId,
                                            userId = targetUser.userId,
                                            role = MemberRole.MEMBER,
                                            status = MemberStatus.PENDING
                                        )
                                    )
                                    // 创建邀请通知
                                    val currentUser = withContext(Dispatchers.IO) {
                                        userRepository.getUserById(userId)
                                    }
                                    val groupName = group?.groupName ?: "学习小组"
                                    notificationRepository.insertNotification(
                                        Notification(
                                            userId = targetUser.userId,
                                            type = NotificationType.GROUP_INVITE,
                                            title = "小组邀请",
                                            content = "${currentUser?.username ?: "有人"}邀请您加入小组「${groupName}」",
                                            relatedId = groupId,
                                            isRead = false
                                        )
                                    )
                                }
                                existing.status == MemberStatus.JOINED -> {
                                    inviteDialogError = "该成员已在小组中"
                                    isInviting = false
                                    return@launch
                                }
                                existing.status == MemberStatus.PENDING -> {
                                    inviteDialogError = "已发送邀请，请等待对方处理"
                                    isInviting = false
                                    return@launch
                                }
                                else -> {
                                    memberRepository.updateMemberStatus(
                                        groupId,
                                        targetUser.userId,
                                        MemberStatus.PENDING
                                    )
                                    // 创建邀请通知
                                    val currentUser = withContext(Dispatchers.IO) {
                                        userRepository.getUserById(userId)
                                    }
                                    val groupName = group?.groupName ?: "学习小组"
                                    notificationRepository.insertNotification(
                                        Notification(
                                            userId = targetUser.userId,
                                            type = NotificationType.GROUP_INVITE,
                                            title = "小组邀请",
                                            content = "${currentUser?.username ?: "有人"}邀请您加入小组「${groupName}」",
                                            relatedId = groupId,
                                            isRead = false
                                        )
                                    )
                                }
                            }
                            showInviteDialog = false
                            inviteInput = ""
                            snackbarHostState.showSnackbar("邀请已发送给 ${targetUser.username}")
                            isInviting = false
                        }
                    }
                ) {
                    if (isInviting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("发送邀请")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isInviting) {
                            showInviteDialog = false
                            inviteDialogError = null
                        }
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { if (enabled) onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GroupAssociationCard(
    course: Course?,
    task: Assignment?
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "关联信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "关联课程：${course?.courseName ?: "未设置"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "关联任务：${task?.title ?: "未设置"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PendingMemberItem(
    member: com.example.myapplication.data.model.GroupMember,
    userName: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "👤",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "申请加入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text("同意", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.height(36.dp)
            ) {
                Text("拒绝", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun MemberItem(
    member: com.example.myapplication.data.model.GroupMember,
    userName: String,
    currentUserId: Int,
    currentUserRole: MemberRole?,
    isCreator: Boolean,
    groupCreatorId: Int?,
    onRoleChange: (MemberRole) -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // 判断当前用户是否是创建者（通过group.creatorId或member.role）
    val userIsCreator = isCreator || (groupCreatorId == currentUserId) || (currentUserRole == MemberRole.CREATOR)
    // 判断该成员是否是创建者
    val memberIsCreator = (member.role == MemberRole.CREATOR) || (member.userId == groupCreatorId)
    // 创建者可以删除所有成员（除了自己），管理员可以删除普通成员
    val canManage = (userIsCreator && !memberIsCreator) || 
                    (currentUserRole == MemberRole.ADMIN && member.role == MemberRole.MEMBER)
    val canDelete = userIsCreator && !memberIsCreator
    val isSelf = member.userId == currentUserId
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = when (member.role) {
                MemberRole.CREATOR -> MaterialTheme.colorScheme.primary
                MemberRole.ADMIN -> MaterialTheme.colorScheme.secondary
                MemberRole.MEMBER -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (isSelf) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "我",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            when (member.role) {
                                MemberRole.CREATOR -> "创建者"
                                MemberRole.ADMIN -> "管理员"
                                MemberRole.MEMBER -> "成员"
                            },
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (member.role) {
                            MemberRole.CREATOR -> MaterialTheme.colorScheme.primaryContainer
                            MemberRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer
                            MemberRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
        }
        
        // 操作按钮区域
        if (canManage && !isSelf) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 如果有删除权限，直接显示删除按钮
                if (canDelete) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除成员",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // 如果有其他管理操作（如设置管理员），显示更多按钮
                if ((member.role == MemberRole.MEMBER && userIsCreator) ||
                    (member.role == MemberRole.ADMIN && userIsCreator)) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (member.role == MemberRole.MEMBER && userIsCreator) {
                                DropdownMenuItem(
                                    text = { Text("设为管理员") },
                                    onClick = {
                                        showMenu = false
                                        onRoleChange(MemberRole.ADMIN)
                                    }
                                )
                            }
                            if (member.role == MemberRole.ADMIN && userIsCreator) {
                                DropdownMenuItem(
                                    text = { Text("取消管理员") },
                                    onClick = {
                                        showMenu = false
                                        onRoleChange(MemberRole.MEMBER)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要移除该成员吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

