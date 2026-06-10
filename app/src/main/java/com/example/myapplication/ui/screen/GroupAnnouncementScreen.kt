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
import com.example.myapplication.data.repository.GroupAnnouncementRepository
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
    val viewModel: GroupAnnouncementViewModel = viewModel(
        factory = GroupAnnouncementViewModelFactory(announcementRepository, groupId)
    )
    
    val announcements by viewModel.announcements.collectAsState()
    val userId = CurrentSession.userIdInt ?: 0
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 允许所有已登录用户发布公告
    val canCreate = userId != 0
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<GroupAnnouncement?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var pinToTop by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                showCreateDialog = true
                                titleError = false
                                contentError = false
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
                        canEdit = canCreate && announcement.authorId == userId,
                        onEdit = {
                            showEditDialog = announcement
                            newTitle = announcement.title
                            newContent = announcement.content
                            pinToTop = announcement.isPinned
                            titleError = false
                            contentError = false
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

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
            },
            title = { Text("发布公告") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = {
                            newTitle = it
                            if (titleError && it.isNotBlank()) titleError = false
                        },
                        label = { Text("标题") },
                        singleLine = true,
                        isError = titleError
                    )
                    if (titleError) {
                        Text(
                            text = "标题不能为空",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = {
                            newContent = it
                            if (contentError && it.isNotBlank()) contentError = false
                        },
                        label = { Text("内容") },
                        modifier = Modifier.height(150.dp),
                        maxLines = 6,
                        isError = contentError
                    )
                    if (contentError) {
                        Text(
                            text = "内容不能为空",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("置顶显示", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = pinToTop,
                            onCheckedChange = { pinToTop = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedTitle = newTitle.trim()
                        val trimmedContent = newContent.trim()
                        titleError = trimmedTitle.isEmpty()
                        contentError = trimmedContent.isEmpty()
                        if (titleError || contentError) return@Button
                        scope.launch {
                            viewModel.createAnnouncement(
                                GroupAnnouncement(
                                    groupId = groupId,
                                    authorId = userId,
                                    title = trimmedTitle,
                                    content = trimmedContent,
                                    isPinned = pinToTop
                                )
                            )
                            snackbarHostState.showSnackbar("公告已发布")
                            newTitle = ""
                            newContent = ""
                            pinToTop = false
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("发布")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑公告对话框
    showEditDialog?.let { announcement ->
        AlertDialog(
            onDismissRequest = {
                showEditDialog = null
                newTitle = ""
                newContent = ""
                pinToTop = false
            },
            title = { Text("编辑公告") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = {
                            newTitle = it
                            if (titleError && it.isNotBlank()) titleError = false
                        },
                        label = { Text("标题") },
                        singleLine = true,
                        isError = titleError
                    )
                    if (titleError) {
                        Text(
                            text = "标题不能为空",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = {
                            newContent = it
                            if (contentError && it.isNotBlank()) contentError = false
                        },
                        label = { Text("内容") },
                        modifier = Modifier.height(150.dp),
                        maxLines = 6,
                        isError = contentError
                    )
                    if (contentError) {
                        Text(
                            text = "内容不能为空",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("置顶显示", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = pinToTop,
                            onCheckedChange = { pinToTop = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedTitle = newTitle.trim()
                        val trimmedContent = newContent.trim()
                        titleError = trimmedTitle.isEmpty()
                        contentError = trimmedContent.isEmpty()
                        if (titleError || contentError) return@Button
                        scope.launch {
                            viewModel.updateAnnouncement(
                                announcement.copy(
                                    title = trimmedTitle,
                                    content = trimmedContent,
                                    isPinned = pinToTop
                                )
                            )
                            snackbarHostState.showSnackbar("公告已更新")
                            showEditDialog = null
                            newTitle = ""
                            newContent = ""
                            pinToTop = false
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = null
                    newTitle = ""
                    newContent = ""
                    pinToTop = false
                }) {
                    Text("取消")
                }
            }
        )
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

