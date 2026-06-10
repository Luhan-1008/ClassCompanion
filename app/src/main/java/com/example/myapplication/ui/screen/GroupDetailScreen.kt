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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.dto.GroupMemberResponse
import com.example.myapplication.ui.viewmodel.StudyGroupViewModel
import com.example.myapplication.ui.viewmodel.StudyGroupViewModelFactory
import com.example.myapplication.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

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
    val userRepository = UserRepository(database.userDao())
    val notificationRepository = NotificationRepository(database.notificationDao())
    val viewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(groupRepository)
    )
    
    val userId = CurrentSession.userIdInt ?: 0
    var group by remember { mutableStateOf<com.example.myapplication.data.model.StudyGroup?>(null) }
    val members by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.JOINED)
    }.collectAsState(initial = emptyList())
    
    // 待审核的申请 - 从本地数据库获取（同一手机不同账号共享数据库）
    val pendingMembers by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.PENDING)
    }.collectAsState(initial = emptyList())
    
    // 获取所有通知（包括已读和未读）- 用于调试
    val allNotifications by remember(userId, groupId) {
        if (userId > 0) {
            notificationRepository.getNotificationsByUser(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    
    // 获取未读通知（特别是加入申请相关的通知）
    val unreadNotifications by remember(userId, groupId) {
        if (userId > 0) {
            notificationRepository.getUnreadNotifications(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    
    // 过滤出与当前小组相关的未读加入申请通知
    val groupApplicationNotifications = remember(unreadNotifications, groupId) {
        val filtered = unreadNotifications.filter { 
            it.type == NotificationType.GROUP_INVITE && 
            it.title == "加入申请" && 
            it.relatedId == groupId
        }
        // 添加调试日志
        if (filtered.isNotEmpty()) {
            android.util.Log.d("GroupDetailScreen", "找到 ${filtered.size} 条未读加入申请通知，当前用户ID=$userId, 小组ID=$groupId")
            filtered.forEach { notif ->
                android.util.Log.d("GroupDetailScreen", "  通知: notificationId=${notif.notificationId}, userId=${notif.userId}, title=${notif.title}, relatedId=${notif.relatedId}, isRead=${notif.isRead}")
            }
        } else {
            android.util.Log.d("GroupDetailScreen", "未找到未读加入申请通知，当前用户ID=$userId, 小组ID=$groupId, 总未读通知数=${unreadNotifications.size}")
            // 打印所有未读通知以便调试
            unreadNotifications.forEach { notif ->
                android.util.Log.d("GroupDetailScreen", "  未读通知: notificationId=${notif.notificationId}, userId=${notif.userId}, type=${notif.type}, title=${notif.title}, relatedId=${notif.relatedId}, isRead=${notif.isRead}")
            }
        }
        filtered
    }
    
    // 调试：打印当前用户和创建者信息，以及所有通知
    LaunchedEffect(userId, group?.creatorId, allNotifications.size) {
        android.util.Log.d("GroupDetailScreen", "=== 通知调试信息 ===")
        android.util.Log.d("GroupDetailScreen", "当前用户ID: $userId")
        android.util.Log.d("GroupDetailScreen", "创建者ID: ${group?.creatorId}")
        android.util.Log.d("GroupDetailScreen", "是否创建者: ${group?.creatorId == userId}")
        android.util.Log.d("GroupDetailScreen", "小组ID: $groupId")
        android.util.Log.d("GroupDetailScreen", "所有通知总数: ${allNotifications.size}")
        android.util.Log.d("GroupDetailScreen", "未读通知总数: ${unreadNotifications.size}")
        
        // 使用同步方法直接查询数据库验证
        withContext(Dispatchers.IO) {
            val directNotifications = notificationRepository.getAllNotificationsByUserSync(userId)
            android.util.Log.d("GroupDetailScreen", "直接查询数据库得到的通知总数: ${directNotifications.size}")
            
            val directUnread = directNotifications.filter { !it.isRead }
            android.util.Log.d("GroupDetailScreen", "直接查询数据库得到的未读通知总数: ${directUnread.size}")
            
            // 打印所有与当前小组相关的通知（包括已读和未读）
            val allGroupNotifications = directNotifications.filter { 
                it.type == NotificationType.GROUP_INVITE && 
                it.title == "加入申请" && 
                it.relatedId == groupId
            }
            android.util.Log.d("GroupDetailScreen", "与当前小组相关的加入申请通知总数（包括已读）: ${allGroupNotifications.size}")
            allGroupNotifications.forEach { notif ->
                android.util.Log.d("GroupDetailScreen", "  通知详情: notificationId=${notif.notificationId}, userId=${notif.userId}, title=${notif.title}, content=${notif.content}, relatedId=${notif.relatedId}, isRead=${notif.isRead}, createdAt=${notif.createdAt}")
            }
            
            // 打印所有通知以便完整调试
            if (directNotifications.isNotEmpty()) {
                android.util.Log.d("GroupDetailScreen", "所有通知列表（直接查询）:")
                directNotifications.forEach { notif ->
                    android.util.Log.d("GroupDetailScreen", "  - notificationId=${notif.notificationId}, userId=${notif.userId}, type=${notif.type}, title=${notif.title}, relatedId=${notif.relatedId}, isRead=${notif.isRead}")
                }
            } else {
                android.util.Log.e("GroupDetailScreen", "✗✗✗ 直接查询数据库：没有找到任何通知！userId=$userId")
            }
        }
        
        // 打印所有与当前小组相关的通知（包括已读和未读）
        val allGroupNotifications = allNotifications.filter { 
            it.type == NotificationType.GROUP_INVITE && 
            it.title == "加入申请" && 
            it.relatedId == groupId
        }
        android.util.Log.d("GroupDetailScreen", "Flow查询：与当前小组相关的加入申请通知总数（包括已读）: ${allGroupNotifications.size}")
        
        android.util.Log.d("GroupDetailScreen", "=== 调试信息结束 ===")
    }
    
    // 过滤掉申请者自己的申请（申请者不能看到自己的申请）
    val pendingApplications = remember(pendingMembers) {
        val filtered = pendingMembers.filter { it.invitedBy == null }
        android.util.Log.d("GroupDetailScreen", "待审核申请数量: ${filtered.size} (总待审核成员: ${pendingMembers.size})")
        filtered.forEach { app ->
            android.util.Log.d("GroupDetailScreen", "  申请: userId=${app.userId}, status=${app.status}")
        }
        filtered
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
    suspend fun isGroupAtCapacity(): Boolean {
        val limit = group?.maxMembers ?: return false
        val joinedCount = memberRepository.getMembersByGroup(groupId, MemberStatus.JOINED).first().size
        return joinedCount >= limit
    }

    val memberProfiles by produceState<Map<Int, User>>(
        initialValue = emptyMap(),
        key1 = members,
        key2 = pendingMembers
    ) {
        val ids = (members + pendingMembers).map { it.userId }.distinct()
        val map = mutableMapOf<Int, User>()
        withContext(Dispatchers.IO) {
            ids.forEach { id ->
                userRepository.getUserById(id)?.let { map[id] = it }
            }
        }
        value = map
    }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteInput by remember { mutableStateOf("") }
    var inviteDialogError by remember { mutableStateOf<String?>(null) }
    var isInviting by remember { mutableStateOf(false) }
    
    LaunchedEffect(groupId) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            group = groupRepository.getGroupById(groupId)
        }
    }
    
    // 强制刷新待审核申请（当进入页面时）
    LaunchedEffect(groupId, userId) {
        android.util.Log.d("GroupDetailScreen", "=== 强制刷新待审核申请 ===")
        android.util.Log.d("GroupDetailScreen", "小组ID: $groupId, 当前用户ID: $userId, 创建者ID: ${group?.creatorId}")
        
        withContext(Dispatchers.IO) {
            // 直接查询数据库获取待审核成员
            val allPendingMembers = memberRepository.getMembersByGroup(groupId, MemberStatus.PENDING).first()
            android.util.Log.d("GroupDetailScreen", "[强制刷新] 直接查询待审核成员: ${allPendingMembers.size} 个")
            allPendingMembers.forEach { member ->
                android.util.Log.d("GroupDetailScreen", "  [强制刷新] 待审核成员: userId=${member.userId}, status=${member.status}, invitedBy=${member.invitedBy}")
            }
            
            // 直接查询数据库获取所有通知
            val allNotifs = notificationRepository.getAllNotificationsByUserSync(userId)
            android.util.Log.d("GroupDetailScreen", "[强制刷新] 直接查询所有通知: ${allNotifs.size} 个 (userId=$userId)")
            
            // 打印所有通知以便调试
            if (allNotifs.isNotEmpty()) {
                android.util.Log.d("GroupDetailScreen", "[强制刷新] 所有通知列表:")
                allNotifs.forEach { notif ->
                    android.util.Log.d("GroupDetailScreen", "  - notificationId=${notif.notificationId}, userId=${notif.userId}, type=${notif.type}, title=${notif.title}, relatedId=${notif.relatedId}, isRead=${notif.isRead}")
                }
            }
            
            // 查找与当前小组相关的通知
            val groupNotifs = allNotifs.filter { 
                it.type == NotificationType.GROUP_INVITE && 
                it.title == "加入申请" && 
                it.relatedId == groupId
            }
            android.util.Log.d("GroupDetailScreen", "[强制刷新] 与当前小组相关的通知: ${groupNotifs.size} 个")
            groupNotifs.forEach { notif ->
                android.util.Log.d("GroupDetailScreen", "  [强制刷新] 通知: notificationId=${notif.notificationId}, userId=${notif.userId}, relatedId=${notif.relatedId}, isRead=${notif.isRead}, content=${notif.content}")
            }
            
            // 如果待审核成员为空但通知存在，说明可能有问题
            if (allPendingMembers.isEmpty() && groupNotifs.isNotEmpty()) {
                android.util.Log.w("GroupDetailScreen", "⚠ 警告: 有通知但没有待审核成员！可能需要检查成员记录")
            }
            
            if (allPendingMembers.isNotEmpty() && groupNotifs.isEmpty()) {
                android.util.Log.w("GroupDetailScreen", "⚠ 警告: 有待审核成员但没有通知！")
            }
        }
    }
    
    val currentMember = remember(members, userId) { members.find { it.userId == userId } }
    val isCreator = remember(group, userId) { group?.creatorId == userId }
    val isAdmin = remember(currentMember) { currentMember?.role == MemberRole.ADMIN }
    val canInvite = remember(isCreator, isAdmin) { isCreator || isAdmin }
    val canManage = remember(isCreator, isAdmin) { isCreator || isAdmin }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var isDeletingGroup by remember { mutableStateOf(false) }
    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var isLeavingGroup by remember { mutableStateOf(false) }
    var showMessagesMenu by remember { mutableStateOf(false) }
    var showMessagesBottomSheet by remember { mutableStateOf(false) }
    
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
                actions = {
                    // 显示待审核申请数量（仅创建者和管理员可见）
                    // 直接使用待审核申请列表，不依赖通知系统
                    val totalCount = pendingApplications.size
                    
                    android.util.Log.d("GroupDetailScreen", "[TopAppBar] 待审核申请数量: $totalCount, 是否创建者/管理员: ${isCreator || isAdmin}")
                    
                    if ((isCreator || isAdmin) && totalCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        text = "$totalCount",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    // 可以添加滚动到待审核申请部分的功能
                                    android.util.Log.d("GroupDetailScreen", "点击了通知图标，待审核申请数: $totalCount")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "待审核申请",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    // 创建者和管理员的菜单（三个点）
                    if (isCreator || isAdmin) {
                        Box {
                            IconButton(
                                onClick = { showMessagesMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多选项",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showMessagesMenu,
                                onDismissRequest = { showMessagesMenu = false }
                            ) {
                                // 消息选项 - 显示申请加入的消息
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        showMessagesMenu = false
                                        showMessagesBottomSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("编辑小组")
                                        }
                                    },
                                    onClick = {
                                        showMessagesMenu = false
                                        navController.navigate("${Screen.EditGroup.route}/$groupId")
                                    }
                                )
                                if (isCreator) {
                                    // 解散小组选项（仅创建者可见）
                                    DropdownMenuItem(
                                        text = { 
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = "解散小组",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMessagesMenu = false
                                            showDeleteGroupDialog = true
                                        }
                                    )
                                }
                            }
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
            // 功能入口卡片
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            }
                            // 成员点击时不做任何操作（已通过enabled=false禁用）
                        },
                        enabled = canInvite
                    )
                }
                
                // 待审核申请（仅创建者和管理员可见，且不包含申请者自己的申请）
                // 只要有待审核申请就显示，不依赖通知系统
                if (canInvite && pendingApplications.isNotEmpty()) {
                    android.util.Log.d("GroupDetailScreen", "[UI] 显示待审核申请卡片: ${pendingApplications.size} 个申请")
                    item {
                        // 当用户点击查看待审核申请时，才标记相关通知为已读
                        // 注意：这里不自动标记，让用户能看到未读提示
                        // 如果需要自动标记，可以在用户点击"同意"或"拒绝"时标记
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "新申请",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "待审核申请 (${pendingApplications.size})",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = "新",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                
                                // 显示待审核的申请（已过滤掉申请者自己的申请）
                                pendingApplications.forEach { pendingMember ->
                                    val applicantUser = memberUserMap[pendingMember.userId]
                                    PendingMemberItem(
                                        member = pendingMember,
                                        userName = applicantUser?.username ?: applicantUser?.realName ?: "用户${pendingMember.userId}",
                                        onApprove = {
                                            val maxMembers = group?.maxMembers ?: Int.MAX_VALUE
                                            if (members.size >= maxMembers) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("小组人数已满，无法同意新的成员")
                                                }
                                                return@PendingMemberItem
                                            }
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
                                        },
                                        onReject = {
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
                                Text(
                                    text = "${members.size}人",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                                MemberStatus.LEFT,
                                                invitedBy = null
                                            )
                                            snackbarHostState.showSnackbar("已移除成员")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // 退出小组按钮（除创建者外的所有成员都可退出）
                if (currentMember != null && !isCreator) {
                    item {
                        Button(
                            onClick = { showLeaveGroupDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "退出小组",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("退出小组")
                        }
                    }
                }
            }
        }
    }
    
    // 退出小组确认对话框
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
                        text = "确定要退出小组「${group?.groupName ?: ""}」吗？",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "退出后将无法接收小组消息和通知，但可以重新申请加入。",
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
                                    // 更新成员状态为已退出
                                    currentMember?.let { member ->
                                        memberRepository.updateMemberStatus(
                                            groupId,
                                            userId,
                                            MemberStatus.LEFT,
                                            invitedBy = null
                                        )
                                    }
                                }
                                snackbarHostState.showSnackbar("已退出小组")
                                // 导航回学习小组列表
                                navController.popBackStack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("退出失败：${e.message}")
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
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
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
                            if (keyword.isBlank()) {
                                inviteDialogError = "请输入用户名或学号"
                                isInviting = false
                                return@launch
                            }
                            
                            val targetUser = withContext(Dispatchers.IO) {
                                // 先尝试精确匹配
                                userRepository.getUserByStudentId(keyword)
                                    ?: userRepository.getUserByUsername(keyword)
                                    ?: run {
                                        // 如果精确匹配失败，尝试模糊搜索
                                        val searchResults = userRepository.searchUsers(keyword)
                                        searchResults.firstOrNull()
                                    }
                            }
                            if (targetUser == null) {
                                inviteDialogError = "未找到该用户，请检查用户名或学号是否正确"
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
                                            status = MemberStatus.PENDING,
                                            invitedBy = userId
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
                                        MemberStatus.PENDING,
                                        invitedBy = userId
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
    
    // 删除小组确认对话框
    group?.let { currentGroup ->
        if (showDeleteGroupDialog) {
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
                            text = "确定要解散小组「${currentGroup.groupName}」吗？",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "此操作将永久删除小组及其所有数据，包括：\n• 所有成员关系\n• 群聊消息\n• 文件库\n• 公告\n• 任务",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "此操作不可恢复！",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
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
                                    // 删除所有成员关系
                                    memberRepository.deleteMembersByGroup(groupId)
                                    
                                    // 删除小组
                                    groupRepository.deleteGroup(currentGroup)
                                }
                                
                                snackbarHostState.showSnackbar("小组已删除")
                                // 导航回学习小组列表
                                navController.popBackStack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("删除失败：${e.message}")
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
                        Text("确认删除")
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
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
        }
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
                onApprove = { pendingMember ->
                    val maxMembers = group?.maxMembers ?: Int.MAX_VALUE
                    if (members.size >= maxMembers) {
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
                onReject = { pendingMember ->
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
            .shadow(if (enabled) 4.dp else 2.dp, shape = RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { if (enabled) onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(if (!enabled) Modifier.alpha(0.6f) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (enabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = if (enabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 禁用状态下显示锁定图标或提示
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "无权限",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
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
    // 创建者可以管理所有成员（除了自己），管理员可以管理普通成员
    val canManage = (userIsCreator && !memberIsCreator) || 
                    (currentUserRole == MemberRole.ADMIN && member.role == MemberRole.MEMBER)
    val canDelete = userIsCreator && !memberIsCreator
    val isSelf = member.userId == currentUserId
    // 创建者可以设置任意成员（除了创建者自己）为管理员或取消管理员
    val canChangeRole = userIsCreator && !memberIsCreator && !isSelf
    
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
        if ((canManage || canChangeRole) && !isSelf) {
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
                
                // 创建者可以设置任意成员（除了创建者自己）为管理员或取消管理员
                if (canChangeRole) {
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
                            if (member.role == MemberRole.MEMBER) {
                                DropdownMenuItem(
                                    text = { Text("设为管理员") },
                                    onClick = {
                                        showMenu = false
                                        onRoleChange(MemberRole.ADMIN)
                                    }
                                )
                            }
                            if (member.role == MemberRole.ADMIN) {
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

@Composable
fun PendingRequestSection(
    pendingMembers: List<GroupMember>,
    memberProfiles: Map<Int, User>,
    onApprove: (GroupMember) -> Unit,
    onReject: (GroupMember) -> Unit
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
                text = "待处理申请",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            pendingMembers.forEach { member ->
                val profile = memberProfiles[member.userId]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.realName ?: profile?.username ?: "用户 ${member.userId}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ID: ${member.userId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onReject(member) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("拒绝")
                        }
                        Button(
                            onClick = { onApprove(member) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("通过")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationMessagesBottomSheet(
    pendingApplications: List<GroupMember>,
    memberUserMap: Map<Int, User>,
    groupId: Int,
    userId: Int,
    onDismiss: () -> Unit,
    onApprove: (GroupMember) -> Unit,
    onReject: (GroupMember) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "消息",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "申请消息",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${pendingApplications.size} 条待处理申请",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 申请列表
        if (pendingApplications.isEmpty()) {
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
                        text = "📭",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "暂无待处理申请",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingApplications) { pendingMember ->
                    val applicantUser = memberUserMap[pendingMember.userId]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "👤",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = applicantUser?.username ?: applicantUser?.realName ?: "用户${pendingMember.userId}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "申请加入小组",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // 操作按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onReject(pendingMember) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("拒绝")
                                }
                                Button(
                                    onClick = { onApprove(pendingMember) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("同意")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

