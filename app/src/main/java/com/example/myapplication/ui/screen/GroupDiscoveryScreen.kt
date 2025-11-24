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
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.viewmodel.StudyGroupViewModel
import com.example.myapplication.ui.viewmodel.StudyGroupViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDiscoveryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val groupRepository = StudyGroupRepository(database.studyGroupDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val viewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(groupRepository)
    )
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val userId = CurrentSession.userIdInt ?: 0
    
    // 搜索公开小组
    val groups by remember(searchQuery, selectedCourseId) {
        if (searchQuery.isNotBlank() || selectedCourseId != null) {
            groupRepository.searchPublicGroups(selectedCourseId, searchQuery)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<com.example.myapplication.data.model.StudyGroup>())
        }
    }.collectAsState(initial = emptyList())
    
    Scaffold(
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
                    items(groups) { group ->
                        DiscoveredGroupCard(
                            group = group,
                            isJoined = false, // TODO: 检查是否已加入
                            onJoinClick = {
                                scope.launch {
                                    // 申请加入小组
                                    val member = com.example.myapplication.data.model.GroupMember(
                                        groupId = group.groupId,
                                        userId = userId,
                                        role = com.example.myapplication.data.model.MemberRole.MEMBER,
                                        status = MemberStatus.PENDING
                                    )
                                    memberRepository.insertMember(member)
                                    
                                    // 显示提示
                                    // TODO: 使用Snackbar显示提示
                                }
                            },
                            onViewClick = {
                                navController.navigate("${com.example.myapplication.ui.navigation.Screen.GroupDetail.route}/${group.groupId}")
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
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable { onViewClick() },
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
                
                if (!isJoined) {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("申请加入")
                    }
                } else {
                    TextButton(onClick = onViewClick) {
                        Text("查看")
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

