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
import com.example.myapplication.data.model.GroupAnnouncement
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.repository.GroupAnnouncementRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.viewmodel.GroupAnnouncementViewModel
import com.example.myapplication.ui.viewmodel.GroupAnnouncementViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAnnouncementScreen(
    navController: NavHostController,
    groupId: Int
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val announcementRepository = GroupAnnouncementRepository(database.groupAnnouncementDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    val viewModel: GroupAnnouncementViewModel = viewModel(
        factory = GroupAnnouncementViewModelFactory(announcementRepository, groupId)
    )
    
    val announcements by viewModel.announcements.collectAsState()
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
                        text = "公告板",
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
                                // TODO: 打开创建公告对话框
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "发布公告")
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (announcements.isEmpty()) {
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
                                text = "📢",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "暂无公告",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(announcements) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        canEdit = canCreate,
                        onEdit = {
                            // TODO: 编辑公告
                        },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteAnnouncement(announcement)
                            }
                        },
                        onPin = {
                            scope.launch {
                                viewModel.togglePin(announcement)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnnouncementCard(
    announcement: GroupAnnouncement,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(announcement.createdAt))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (announcement.isPinned) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (announcement.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "置顶",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                            DropdownMenuItem(
                                text = { Text(if (announcement.isPinned) "取消置顶" else "置顶") },
                                onClick = {
                                    showMenu = false
                                    onPin()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PushPin, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
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
            
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

