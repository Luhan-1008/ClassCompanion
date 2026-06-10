package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.flow.combine
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.viewmodel.StudyGroupViewModel
import com.example.myapplication.ui.viewmodel.StudyGroupViewModelFactory
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.dto.GroupMemberCreateRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDiscoveryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val groupRepository = StudyGroupRepository(database.studyGroupDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val notificationRepository = NotificationRepository(database.notificationDao())
    val userRepository = UserRepository(database.userDao())
    val viewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(groupRepository)
    )
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val userId = CurrentSession.userIdInt ?: 0
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 搜索公开小组
    val groups by remember(searchQuery, selectedCourseId) {
        if (searchQuery.isNotBlank() || selectedCourseId != null) {
            groupRepository.searchPublicGroups(selectedCourseId, searchQuery)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<com.example.myapplication.data.model.StudyGroup>())
        }
    }.collectAsState(initial = emptyList())
    
    // 获取用户已加入的小组ID列表
    val joinedGroupIds by remember(userId) {
        memberRepository.getGroupsByMember(userId, MemberStatus.JOINED)
            .map { members -> members.map { it.groupId }.toSet() }
    }.collectAsState(initial = emptySet())
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "发现小组",
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索小组名称或主题...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            // 小组列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (groups.isEmpty() && searchQuery.isBlank()) {
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
                                    text = "🔍",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Text(
                                    text = "搜索学习小组",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "输入关键词搜索公开小组",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else if (groups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未找到相关小组",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // 可以搜索到所有公开小组（包括自己已经加入/创建的小组），
                    // 但已加入的小组在本页面不能跳转查看详情。
                    items(groups) { group ->
                        val isJoined = joinedGroupIds.contains(group.groupId)
                        DiscoveredGroupCard(
                            group = group,
                            isJoined = isJoined,
                            onJoinClick = {
                                scope.launch {
                                    // 检查是否已经申请过
                                    val existingMember = memberRepository.getMember(group.groupId, userId)
                                    if (existingMember != null) {
                                        when (existingMember.status) {
                                            MemberStatus.JOINED -> {
                                                snackbarHostState.showSnackbar("您已经加入该小组")
                                            }
                                            MemberStatus.PENDING -> {
                                                snackbarHostState.showSnackbar("您已经申请加入，请等待审核")
                                            }
                                            else -> {
                                                // 重新申请
                                                memberRepository.updateMemberStatus(
                                                    group.groupId,
                                                    userId,
                                                    MemberStatus.PENDING,
                                                    invitedBy = null
                                                )
                                                
                                                // 重新申请时也要发送通知
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        val freshGroup = groupRepository.getGroupById(group.groupId)
                                                        if (freshGroup != null) {
                                                            val applicant = userRepository.getUserById(userId)
                                                            val applicantName = applicant?.username ?: "用户${userId}"
                                                            val groupName = freshGroup.groupName
                                                            val creatorId = freshGroup.creatorId
                                                            
                                                            Log.d("GroupDiscoveryScreen", "[重新申请] 开始发送加入申请通知")
                                                            Log.d("GroupDiscoveryScreen", "[重新申请] 申请人: $applicantName (ID=$userId)")
                                                            Log.d("GroupDiscoveryScreen", "[重新申请] 创建者ID: $creatorId")
                                                            
                                                            if (creatorId > 0) {
                                                                val notification = Notification(
                                                                    userId = creatorId,
                                                                    type = NotificationType.GROUP_INVITE,
                                                                    title = "加入申请",
                                                                    content = "${applicantName}申请加入小组「${groupName}」",
                                                                    relatedId = freshGroup.groupId,
                                                                    isRead = false
                                                                )
                                                                
                                                                try {
                                                                    val notificationId = notificationRepository.insertNotification(notification)
                                                                    Log.d("GroupDiscoveryScreen", "[重新申请] ✓ 已给创建者发送通知: 创建者ID=$creatorId, 通知ID=$notificationId")
                                                                    
                                                                    // 验证通知
                                                                    val savedNotification = notificationRepository.getNotificationById(notificationId.toInt())
                                                                    if (savedNotification != null) {
                                                                        Log.d("GroupDiscoveryScreen", "[重新申请] ✓ 通知验证成功: notificationId=$notificationId")
                                                                    } else {
                                                                        Log.e("GroupDiscoveryScreen", "[重新申请] ✗ 通知保存失败")
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e("GroupDiscoveryScreen", "[重新申请] ✗ 保存通知时出错", e)
                                                                }
                                                            }
                                                            
                                                            // 给所有管理员发送通知
                                                            val allMembers = memberRepository.getMembersByGroup(freshGroup.groupId, MemberStatus.JOINED).first()
                                                            val admins = allMembers.filter { 
                                                                it.role == MemberRole.ADMIN && it.userId != userId && it.userId != creatorId
                                                            }
                                                            
                                                            Log.d("GroupDiscoveryScreen", "[重新申请] 找到 ${admins.size} 个管理员，将发送通知")
                                                            admins.forEach { admin ->
                                                                val adminNotification = Notification(
                                                                    userId = admin.userId,
                                                                    type = NotificationType.GROUP_INVITE,
                                                                    title = "加入申请",
                                                                    content = "${applicantName}申请加入小组「${groupName}」",
                                                                    relatedId = freshGroup.groupId,
                                                                    isRead = false
                                                                )
                                                                
                                                                try {
                                                                    val notificationId = notificationRepository.insertNotification(adminNotification)
                                                                    Log.d("GroupDiscoveryScreen", "[重新申请] ✓ 已给管理员发送通知: 管理员ID=${admin.userId}, 通知ID=$notificationId")
                                                                } catch (e: Exception) {
                                                                    Log.e("GroupDiscoveryScreen", "[重新申请] ✗ 给管理员发送通知失败: adminId=${admin.userId}", e)
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("GroupDiscoveryScreen", "[重新申请] 发送通知失败", e)
                                                    }
                                                }
                                                
                                                snackbarHostState.showSnackbar("已发送加入申请")
                                            }
                                        }
                                    } else {
                                        // 申请加入小组
                                        val member = com.example.myapplication.data.model.GroupMember(
                                            groupId = group.groupId,
                                            userId = userId,
                                            role = com.example.myapplication.data.model.MemberRole.MEMBER,
                                            status = MemberStatus.PENDING
                                        )
                                        
                                        // 同一手机不同账号：使用本地数据库发送通知
                                        withContext(Dispatchers.IO) {
                                            Log.d("GroupDiscoveryScreen", "=== 开始申请加入小组 ===")
                                            Log.d("GroupDiscoveryScreen", "小组ID: ${group.groupId}, 用户ID: $userId")
                                            
                                            // 1. 插入本地成员记录
                                            val memberId = memberRepository.insertMember(member)
                                            Log.d("GroupDiscoveryScreen", "✓ 已保存成员到本地数据库: memberId=$memberId, groupId=${member.groupId}, userId=${member.userId}, status=${member.status}")
                                            
                                            // 验证成员是否真的保存了
                                            kotlinx.coroutines.delay(100)
                                            val savedMember = memberRepository.getMember(group.groupId, userId)
                                            if (savedMember != null) {
                                                Log.d("GroupDiscoveryScreen", "✓ 成员验证成功: memberId=${savedMember.memberId}, status=${savedMember.status}")
                                            } else {
                                                Log.e("GroupDiscoveryScreen", "✗ 成员验证失败: 查询不到成员记录！")
                                            }
                                            
                                            // 查询该小组的所有待审核成员
                                            val allPending = memberRepository.getMembersByGroup(group.groupId, MemberStatus.PENDING).first()
                                            Log.d("GroupDiscoveryScreen", "该小组的待审核成员总数: ${allPending.size}")
                                            allPending.forEach { m ->
                                                Log.d("GroupDiscoveryScreen", "  待审核成员: userId=${m.userId}, status=${m.status}")
                                            }
                                            
                                            // 2. 获取小组和用户信息
                                            val freshGroup = groupRepository.getGroupById(group.groupId)
                                            if (freshGroup == null) {
                                                Log.e("GroupDiscoveryScreen", "无法获取小组信息")
                                                return@withContext
                                            }
                                            
                                            val applicant = userRepository.getUserById(userId)
                                            val applicantName = applicant?.username ?: "用户${userId}"
                                            val groupName = freshGroup.groupName
                                            val creatorId = freshGroup.creatorId
                                            
                                            Log.d("GroupDiscoveryScreen", "创建者ID: $creatorId, 申请人: $applicantName")
                                            
                                            // 3. 发送通知到本地数据库（同一手机，不同账号可以共享数据库）
                                            // 给创建者发送通知
                                            if (creatorId > 0 && creatorId != userId) {
                                                val notification = Notification(
                                                    userId = creatorId,
                                                    type = NotificationType.GROUP_INVITE,
                                                    title = "加入申请",
                                                    content = "${applicantName}申请加入小组「${groupName}」",
                                                    relatedId = freshGroup.groupId,
                                                    isRead = false
                                                )
                                                
                                                try {
                                                    val notificationId = notificationRepository.insertNotification(notification)
                                                    Log.d("GroupDiscoveryScreen", "✓✓✓ 通知已发送给创建者！通知ID=$notificationId, 创建者ID=$creatorId")
                                                    
                                                    // 验证通知
                                                    val saved = notificationRepository.getNotificationById(notificationId.toInt())
                                                    if (saved != null) {
                                                        Log.d("GroupDiscoveryScreen", "✓ 通知验证成功: userId=${saved.userId}, isRead=${saved.isRead}")
                                                    } else {
                                                        Log.e("GroupDiscoveryScreen", "✗ 通知验证失败")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("GroupDiscoveryScreen", "✗ 发送通知失败", e)
                                                }
                                            } else {
                                                Log.w("GroupDiscoveryScreen", "创建者ID无效或与申请人相同: creatorId=$creatorId, userId=$userId")
                                            }
                                            
                                            // 给所有管理员发送通知
                                            val allMembers = memberRepository.getMembersByGroup(freshGroup.groupId, MemberStatus.JOINED).first()
                                            val admins = allMembers.filter { 
                                                it.role == MemberRole.ADMIN && it.userId != userId && it.userId != creatorId
                                            }
                                            
                                            Log.d("GroupDiscoveryScreen", "找到 ${admins.size} 个管理员，将发送通知")
                                            admins.forEach { admin ->
                                                val adminNotification = Notification(
                                                    userId = admin.userId,
                                                    type = NotificationType.GROUP_INVITE,
                                                    title = "加入申请",
                                                    content = "${applicantName}申请加入小组「${groupName}」",
                                                    relatedId = freshGroup.groupId,
                                                    isRead = false
                                                )
                                                
                                                try {
                                                    val notificationId = notificationRepository.insertNotification(adminNotification)
                                                    Log.d("GroupDiscoveryScreen", "✓✓✓ 通知已发送给管理员！通知ID=$notificationId, 管理员ID=${admin.userId}")
                                                } catch (e: Exception) {
                                                    Log.e("GroupDiscoveryScreen", "✗ 给管理员发送通知失败: adminId=${admin.userId}", e)
                                                }
                                            }
                                            
                                            Log.d("GroupDiscoveryScreen", "=== 申请流程结束 ===")
                                        }
                                        
                                        snackbarHostState.showSnackbar("已发送加入申请")
                                    }
                                }
                            },
                            onViewClick = {
                                // 发现小组页面不再提供“查看详情”入口（即使已加入小组）
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredGroupCard(
    group: com.example.myapplication.data.model.StudyGroup,
    isJoined: Boolean,
    onJoinClick: () -> Unit,
    onViewClick: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = group.groupName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!group.isPublic) {
                            AssistChip(
                                onClick = {},
                                label = { Text("私密", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    if (!group.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    if (!group.topic.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    group.topic,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
                
                when {
                    !isJoined -> {
                        Button(
                            onClick = onJoinClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("申请加入")
                        }
                    }
                    else -> {
                        // 已加入小组：只显示“已加入”状态，不提供查看入口
                        Text(
                            text = "已加入",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 成员数信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "最大成员数: ${group.maxMembers}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

