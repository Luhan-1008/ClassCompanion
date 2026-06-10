package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.session.CurrentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    navController: NavHostController,
    groupId: Int
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val userRepository = UserRepository(database.userDao())
    val groupRepository = StudyGroupRepository(database.studyGroupDao())
    val userId = CurrentSession.userIdInt ?: 0
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var group by remember { mutableStateOf<com.example.myapplication.data.model.StudyGroup?>(null) }
    
    // 加载小组信息
    LaunchedEffect(groupId) {
        withContext(Dispatchers.IO) {
            group = groupRepository.getGroupById(groupId)
        }
    }
    
    val members by remember(groupId) {
        memberRepository.getMembersByGroup(groupId, MemberStatus.JOINED)
    }.collectAsState(initial = emptyList())
    
    // 获取所有成员的用户信息
    val memberUserMap by produceState<Map<Int, User>>(
        initialValue = emptyMap(),
        key1 = members
    ) {
        val map = mutableMapOf<Int, User>()
        withContext(Dispatchers.IO) {
            members.forEach { member ->
                userRepository.getUserById(member.userId)?.let { user ->
                    map[member.userId] = user
                }
            }
        }
        value = map
    }
    
    // 判断当前用户是否是创建者
    val isCreator = remember(group, userId) { group?.creatorId == userId }
    val currentMember = remember(members, userId) { members.find { it.userId == userId } }
    
    // 按角色排序成员
    val sortedMembers = remember(members) {
        members.sortedBy { member ->
            when (member.role) {
                MemberRole.CREATOR -> 0
                MemberRole.ADMIN -> 1
                MemberRole.MEMBER -> 2
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "小组成员",
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // 成员数量统计
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "共 ${members.size} 人",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            items(sortedMembers, key = { it.memberId }) { member ->
                val user = memberUserMap[member.userId]
                val userName = user?.realName ?: user?.username ?: "用户${member.userId}"
                val isSelf = member.userId == userId
                val memberIsCreator = member.role == MemberRole.CREATOR || member.userId == group?.creatorId
                
                // 创建者可以管理所有成员（除了自己），但不能管理其他创建者
                val canManage = isCreator && !isSelf && !memberIsCreator
                
                MemberListItem(
                    userName = userName,
                    role = member.role,
                    isSelf = isSelf,
                    canManage = canManage,
                    member = member,
                    onRoleChange = { newRole ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                memberRepository.updateMember(
                                    member.copy(role = newRole)
                                )
                            }
                            snackbarHostState.showSnackbar(
                                if (newRole == MemberRole.ADMIN) "已设置为管理员" else "已取消管理员"
                            )
                        }
                    },
                    onRemove = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                memberRepository.updateMemberStatus(
                                    member.groupId,
                                    member.userId,
                                    MemberStatus.LEFT,
                                    invitedBy = null
                                )
                            }
                            snackbarHostState.showSnackbar("已移除成员")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MemberListItem(
    userName: String,
    role: MemberRole,
    isSelf: Boolean,
    canManage: Boolean = false,
    member: GroupMember? = null,
    onRoleChange: ((MemberRole) -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = when (role) {
                                MemberRole.CREATOR -> listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                                MemberRole.ADMIN -> listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                                MemberRole.MEMBER -> listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (role) {
                        MemberRole.CREATOR, MemberRole.ADMIN -> Color.White
                        MemberRole.MEMBER -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
            
            // 用户名和角色
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelf) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "我",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (role) {
                        MemberRole.CREATOR -> MaterialTheme.colorScheme.primaryContainer
                        MemberRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer
                        MemberRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = when (role) {
                            MemberRole.CREATOR -> "创建者"
                            MemberRole.ADMIN -> "管理员"
                            MemberRole.MEMBER -> "成员"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (role) {
                            MemberRole.CREATOR -> MaterialTheme.colorScheme.onPrimaryContainer
                            MemberRole.ADMIN -> MaterialTheme.colorScheme.onSecondaryContainer
                            MemberRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // 操作按钮区域（仅创建者可见）
            if (canManage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 删除成员按钮
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
                    
                    // 设置管理员菜单
                    if (onRoleChange != null && member != null) {
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
    }
    
    // 删除确认对话框
    if (showDeleteConfirm && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    "确认删除",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "确定要移除该成员吗？",
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

