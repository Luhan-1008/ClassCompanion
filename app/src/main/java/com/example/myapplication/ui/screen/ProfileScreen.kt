package com.example.myapplication.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.RemoteUserRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.example.myapplication.ui.viewmodel.UserViewModelFactory
import com.example.myapplication.work.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = UserRepository(database.userDao())
    val remoteRepository = remember { null as RemoteUserRepository? }
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(repository, remoteRepository)
    )

    val currentUser by viewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()
    val userId = CurrentSession.userIdInt ?: 0
    val courseDao = remember { database.courseDao() }
    val assignmentDao = remember { database.assignmentDao() }
    val groupMemberDao = remember { database.groupMemberDao() }
    val courses by remember(userId) { courseDao.getCoursesByUser(userId) }.collectAsState(initial = emptyList())
    val assignments by remember(userId) { assignmentDao.getAssignmentsByUser(userId) }.collectAsState(initial = emptyList())
    val joinedGroups by remember(userId) { groupMemberDao.getGroupsByMember(userId) }.collectAsState(initial = emptyList())

    // 加载当前用户信息
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    // 加载头像
    LaunchedEffect(currentUser?.avatarUrl) {
        scope.launch {
            avatarBitmap = loadAvatarBitmap(context, currentUser?.avatarUrl)
        }
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                    inputStream?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            avatarBitmap = bitmap.asImageBitmap()
                            val savedPath = withContext(Dispatchers.IO) {
                                saveAvatarToInternalStorage(context, bitmap, userId)
                            }
                            val updatedUser = savedPath?.let { path -> currentUser?.copy(avatarUrl = path) }
                            if (updatedUser != null) {
                                withContext(Dispatchers.IO) {
                                    repository.updateUser(updatedUser)
                                }
                                viewModel.loadCurrentUser()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
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
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeaderSection(
                user = currentUser,
                avatarBitmap = avatarBitmap,
                onAvatarClick = { imagePickerLauncher.launch("image/*") },
                stats = listOf(
                    "课程" to courses.size,
                    "作业" to assignments.size,
                    "小组" to joinedGroups.size
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "功能",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                        NavigationMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "AI笔记助手",
                            description = "智能生成知识提纲"
                        ) {
                            navController.navigate("ai_note")
                        }

                        NavigationMenuItem(
                            icon = Icons.Default.Home,
                            title = "作业辅导提示",
                            description = "获取解题思路和提示"
                        ) {
                            navController.navigate("assignment_help")
                        }

                        NavigationMenuItem(
                            icon = Icons.Default.List,
                            title = "个性化学习分析",
                            description = "查看学习报告和建议"
                        ) {
                            navController.navigate("learning_analytics")
                        }
                }
            }

            // 退出登录按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                    Button(
                        onClick = {
                            showLogoutDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "退出登录",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "退出登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 退出登录确认对话框
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "确认退出",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "确定要退出登录吗？退出后需要重新登录才能使用应用。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                                    ReminderScheduler.cancel(context.applicationContext)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        Text("退出")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("取消")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    user: com.example.myapplication.data.model.User?,
    avatarBitmap: ImageBitmap?,
    onAvatarClick: () -> Unit,
    stats: List<Pair<String, Int>>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() }
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "头像",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "头像",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user?.realName?.takeIf { it.isNotBlank() } ?: user?.username ?: "未登录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                user?.studentId?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "学号 $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                user?.email?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private fun loadAvatarBitmap(context: Context, uriString: String?): ImageBitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
        val bitmap: Bitmap? = when {
            uriString.startsWith("file://") -> {
                val file = File(Uri.parse(uriString).path ?: return null)
                BitmapFactory.decodeFile(file.absolutePath)
            }
            uriString.startsWith("/") -> BitmapFactory.decodeFile(uriString)
            else -> {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        }
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

private fun saveAvatarToInternalStorage(context: Context, bitmap: Bitmap, userId: Int): String? {
    return try {
        val dir = File(context.filesDir, "avatars").apply {
            if (!exists()) mkdirs()
        }
        val file = File(dir, "avatar_$userId.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun NavigationMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
