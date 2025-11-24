package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.StudyGroupViewModel
import com.example.myapplication.ui.viewmodel.StudyGroupViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyGroupsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = StudyGroupRepository(database.studyGroupDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val courseRepository = CourseRepository(database.courseDao())
    val viewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(repository)
    )
    
    val groups by viewModel.groups.collectAsState()
    val userId = CurrentSession.userIdInt ?: 0
    val scope = rememberCoroutineScope()
    val pendingInvites by memberRepository.getGroupsByMember(userId, MemberStatus.PENDING)
        .collectAsState(initial = emptyList())
    val pendingGroupDetails by produceState<Map<Int, com.example.myapplication.data.model.StudyGroup>>(
        initialValue = emptyMap(),
        key1 = pendingInvites
    ) {
        val map = mutableMapOf<Int, com.example.myapplication.data.model.StudyGroup>()
        withContext(Dispatchers.IO) {
            pendingInvites.forEach { member ->
                repository.getGroupById(member.groupId)?.let { group ->
                    map[member.groupId] = group
                }
            }
        }
        value = map
    }
    
    val courses by remember(userId) {
        courseRepository.getCoursesByUser(userId)
    }.collectAsState(initial = emptyList())
    val courseMap = remember(courses) { courses.associateBy { it.courseId } }
    var showActionMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "学习小组",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showActionMenu = !showActionMenu }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "小组操作"
                            )
                        }
                        DropdownMenu(
                            expanded = showActionMenu,
                            onDismissRequest = { showActionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("创建小组") },
                                onClick = {
                                    showActionMenu = false
                                    navController.navigate(Screen.CreateGroup.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("发现小组") },
                                onClick = {
                                    showActionMenu = false
                                    navController.navigate("group_discovery")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("加入小组") },
                                onClick = {
                                    showActionMenu = false
                                    navController.navigate("join_group_by_invite")
                                }
                            )
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pendingInvites.isNotEmpty()) {
                item {
                    Text(
                        text = "待处理的邀请",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(pendingInvites, key = { it.memberId }) { pending ->
                    val group = pendingGroupDetails[pending.groupId]
                    PendingInviteCard(
                        group = group,
                        onAccept = {
                            scope.launch {
                                memberRepository.updateMemberStatus(
                                    pending.groupId,
                                    userId,
                                    MemberStatus.JOINED
                                )
                            }
                        },
                        onDecline = {
                            scope.launch {
                                memberRepository.updateMemberStatus(
                                    pending.groupId,
                                    userId,
                                    MemberStatus.LEFT
                                )
                            }
                        },
                        onViewGroup = {
                            group?.let {
                                navController.navigate("${Screen.GroupDetail.route}/${it.groupId}")
                            }
                        }
                    )
                }
            }
            
            if (groups.isEmpty()) {
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
                                text = "👥",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "暂无学习小组",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击右上角加号完成创建或加入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(groups) { group ->
                    val courseName = group.courseId?.let { courseMap[it]?.courseName }
                    StudyGroupCard(
                        group = group,
                        courseName = courseName,
                        onClick = {
                            navController.navigate("${Screen.GroupDetail.route}/${group.groupId}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudyGroupCard(
    group: com.example.myapplication.data.model.StudyGroup,
    courseName: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${group.groupName} (${if (group.isPublic) "公开" else "私密"})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "关联课程：${courseName ?: "未关联"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PendingInviteCard(
    group: com.example.myapplication.data.model.StudyGroup?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onViewGroup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📬",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group?.groupName ?: "学习小组邀请",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = group?.description ?: "你收到了新的学习小组邀请，是否加入？",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("稍后吧")
                    }
                    Button(
                        onClick = {
                            onAccept()
                            onViewGroup()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("立即加入", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


