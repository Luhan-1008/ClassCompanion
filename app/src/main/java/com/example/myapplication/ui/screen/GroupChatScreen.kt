package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.GroupMessage
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.GroupChatViewModel
import com.example.myapplication.ui.viewmodel.GroupChatViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    navController: NavHostController,
    groupId: Int
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val messageRepository = GroupMessageRepository(database.groupMessageDao())
    val userRepository = UserRepository(database.userDao())
    val groupRepository = StudyGroupRepository(database.studyGroupDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val notificationRepository = NotificationRepository(database.notificationDao())
    val viewModel: GroupChatViewModel = viewModel(
        factory = GroupChatViewModelFactory(messageRepository, groupId)
    )
    
    val messages by viewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val userId = CurrentSession.userIdInt ?: 0
    
    // 获取群信息
    var group by remember { mutableStateOf<com.example.myapplication.data.model.StudyGroup?>(null) }
    LaunchedEffect(groupId) {
        withContext(Dispatchers.IO) {
            group = groupRepository.getGroupById(groupId)
        }
    }
    
    // 获取当前用户的成员信息，判断是否为创建者或管理员
    var currentMember by remember { mutableStateOf<GroupMember?>(null) }
    LaunchedEffect(groupId, userId) {
        if (userId > 0) {
            withContext(Dispatchers.IO) {
                currentMember = memberRepository.getMember(groupId, userId)
            }
        }
    }
    
    val isCreator = remember(group, userId) { group?.creatorId == userId }
    val isAdmin = remember(currentMember) { currentMember?.role == MemberRole.ADMIN }
    val canManage = isCreator || isAdmin
    
    // 待审核的申请
    val pendingMembers by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.PENDING)
    }.collectAsState(initial = emptyList())
    val joinedMembers by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.JOINED)
    }.collectAsState(initial = emptyList())
    
    // 过滤掉申请者自己的申请
    val pendingApplications = remember(pendingMembers) {
        pendingMembers.filter { it.invitedBy == null }
    }
    
    // 获取所有成员的用户信息（用于显示用户名）
    val memberUserMap by produceState<Map<Int, User>>(
        initialValue = emptyMap(),
        key1 = pendingApplications
    ) {
        val map = mutableMapOf<Int, User>()
        withContext(Dispatchers.IO) {
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
    
    // 获取与当前小组相关的未读加入申请通知
    val unreadNotifications by remember(userId, groupId) {
        if (userId > 0) {
            notificationRepository.getUnreadNotifications(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    
    val groupApplicationNotifications = remember(unreadNotifications, groupId) {
        unreadNotifications.filter { 
            it.type == NotificationType.GROUP_INVITE && 
            it.title == "加入申请" && 
            it.relatedId == groupId
        }
    }
    
    var showMenu by remember { mutableStateOf(false) }
    var showMessagesBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var isLeavingGroup by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var isDeletingGroup by remember { mutableStateOf(false) }
    val canLeaveGroup = currentMember != null && !isCreator
    
    // 获取用户信息映射
    val userMap by produceState<Map<Int, User>>(
        initialValue = emptyMap(),
        key1 = messages
    ) {
        val map = mutableMapOf<Int, User>()
        withContext(Dispatchers.IO) {
            messages.map { it.userId }.distinct().forEach { uid ->
                userRepository.getUserById(uid)?.let { user ->
                    map[uid] = user
                }
            }
        }
        value = map
    }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val fileName = "image_${System.currentTimeMillis()}.jpg"
                    val filePath = saveImageFromUri(context, it, fileName)
                    
                    if (filePath != null) {
                        val message = GroupMessage(
                            groupId = groupId,
                            userId = userId,
                            content = filePath, // 存储文件路径
                            messageType = MessageType.IMAGE,
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.sendMessage(message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = group?.groupName ?: "群聊",
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
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多选项"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (canManage) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("编辑信息")
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate("${Screen.EditGroup.route}/$groupId")
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("查看成员")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("group_members/$groupId")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("邀请成员")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("group_invite/$groupId")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("文件库")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("group_files/$groupId")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("公告板")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("group_announcements/$groupId")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.List,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("小组任务")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("group_tasks/$groupId")
                                }
                            )
                            // 消息选项 - 仅创建者和管理员可见
                            if (canManage) {
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Message,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("消息")
                                            // 如果有待审核申请，显示徽章
                                            if (pendingApplications.isNotEmpty()) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                ) {
                                                    Text(
                                                        text = "${pendingApplications.size}",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        showMessagesBottomSheet = true
                                    }
                                )
                            }
                            if (canLeaveGroup) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ExitToApp,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text("退出小组", color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        showLeaveGroupDialog = true
                                    }
                                )
                            }
                            if (isCreator) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text("解散小组", color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteGroupDialog = true
                                    }
                                )
                            }
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
            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val sender = userMap[message.userId]
                    MessageBubble(
                        message = message,
                        isOwnMessage = message.userId == userId,
                        senderName = sender?.username ?: sender?.realName ?: "用户${message.userId}"
                    )
                }
            }
            
            // 输入栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 图片选择按钮
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "选择图片",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 输入框
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    // 发送按钮
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                scope.launch {
                                    val message = GroupMessage(
                                        groupId = groupId,
                                        userId = userId,
                                        content = messageText,
                                        messageType = MessageType.TEXT,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    viewModel.sendMessage(message)
                                    messageText = ""
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "发送",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
    
    // 退出小组确认
    if (showLeaveGroupDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLeavingGroup) {
                    showLeaveGroupDialog = false
                }
            },
            title = {
                Text(
                    text = "退出小组",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "确定要退出「${group?.groupName.orEmpty()}」吗？",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "退出后将无法收到该小组的聊天与通知，需重新申请才能加入。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLeavingGroup = true
                            try {
                                withContext(Dispatchers.IO) {
                                    memberRepository.updateMemberStatus(
                                        groupId,
                                        userId,
                                        MemberStatus.LEFT,
                                        invitedBy = null
                                    )
                                }
                                showLeaveGroupDialog = false
                                snackbarHostState.showSnackbar("已退出小组")
                                navController.popBackStack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("退出失败：${e.message}")
                            } finally {
                                isLeavingGroup = false
                            }
                        }
                    },
                    enabled = !isLeavingGroup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    if (isLeavingGroup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("确认退出")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isLeavingGroup) {
                            showLeaveGroupDialog = false
                        }
                    },
                    enabled = !isLeavingGroup
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 解散小组确认（仅创建者可见）
    if (showDeleteGroupDialog && isCreator) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingGroup) {
                    showDeleteGroupDialog = false
                }
            },
            title = {
                Text(
                    text = "解散小组",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "确定要解散「${group?.groupName.orEmpty()}」吗？",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "此操作将删除所有成员关系、聊天记录、文件、公告与任务，且不可恢复。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isDeletingGroup = true
                            try {
                                withContext(Dispatchers.IO) {
                                    memberRepository.deleteMembersByGroup(groupId)
                                    group?.let { groupRepository.deleteGroup(it) }
                                }
                                showDeleteGroupDialog = false
                                snackbarHostState.showSnackbar("小组已解散")
                                navController.popBackStack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("解散失败：${e.message}")
                                isDeletingGroup = false
                            }
                        }
                    },
                    enabled = !isDeletingGroup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    if (isDeletingGroup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("确认解散")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeletingGroup) {
                            showDeleteGroupDialog = false
                        }
                    },
                    enabled = !isDeletingGroup
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 申请消息底部表单
    if (showMessagesBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMessagesBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ApplicationMessagesBottomSheet(
                pendingApplications = pendingApplications,
                memberUserMap = memberUserMap,
                groupId = groupId,
                userId = userId,
                onDismiss = { showMessagesBottomSheet = false },
                onApprove = { pendingMember: GroupMember ->
                    val maxMembers = group?.maxMembers ?: Int.MAX_VALUE
                    if (joinedMembers.size >= maxMembers) {
                        scope.launch {
                            snackbarHostState.showSnackbar("小组人数已满，无法同意新的成员")
                        }
                    } else {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                // 更新本地数据库
                                memberRepository.updateMemberStatus(
                                    groupId,
                                    pendingMember.userId,
                                    MemberStatus.JOINED,
                                    invitedBy = null
                                )
                                
                                // 标记相关通知为已读
                                groupApplicationNotifications
                                    .filter { it.relatedId == groupId && it.userId == userId }
                                    .forEach { notificationRepository.markAsRead(it.notificationId) }
                            }
                            snackbarHostState.showSnackbar("已同意加入")
                        }
                    }
                },
                onReject = { pendingMember: GroupMember ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            // 更新本地数据库
                            memberRepository.updateMemberStatus(
                                groupId,
                                pendingMember.userId,
                                MemberStatus.LEFT,
                                invitedBy = null
                            )
                            
                            // 标记相关通知为已读
                            groupApplicationNotifications
                                .filter { it.relatedId == groupId && it.userId == userId }
                                .forEach { notificationRepository.markAsRead(it.notificationId) }
                        }
                        snackbarHostState.showSnackbar("已拒绝申请")
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: GroupMessage,
    isOwnMessage: Boolean,
    senderName: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // 头像
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // 显示发送者名称（仅非自己的消息）
            if (!isOwnMessage && senderName.isNotEmpty()) {
                Text(
                    text = senderName,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            // 消息内容
            when (message.messageType) {
                MessageType.TEXT -> {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                            bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                        ),
                        color = if (isOwnMessage) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.shadow(2.dp, shape = RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOwnMessage) 
                                Color.White 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MessageType.IMAGE -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.shadow(2.dp, shape = RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = message.content, // content存储图片URL或路径
                            contentDescription = "图片消息",
                            modifier = Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                MessageType.FILE -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.shadow(2.dp, shape = RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📎", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // 时间戳
            Text(
                text = formatMessageTime(message.createdAt),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        if (isOwnMessage) {
            Spacer(modifier = Modifier.width(8.dp))
            // 头像
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "我",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "刚刚"
        diff < 3600000 -> "${diff / 60000}分钟前"
        diff < 86400000 -> "${diff / 3600000}小时前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun saveImageFromUri(context: android.content.Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val filesDir = context.filesDir
        val file = File(filesDir, "group_chat_images/$fileName")
        file.parentFile?.mkdirs()
        
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
