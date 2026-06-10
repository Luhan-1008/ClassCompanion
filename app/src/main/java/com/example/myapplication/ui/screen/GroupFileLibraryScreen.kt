package com.example.myapplication.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.myapplication.data.model.FileType
import com.example.myapplication.data.model.GroupFile
import com.example.myapplication.data.repository.GroupFileRepository
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.ui.viewmodel.GroupFileViewModel
import com.example.myapplication.ui.viewmodel.GroupFileViewModelFactory
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFileLibraryScreen(
    navController: NavHostController,
    groupId: Int
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val fileRepository = GroupFileRepository(database.groupFileDao())
    val viewModel: GroupFileViewModel = viewModel(
        factory = GroupFileViewModelFactory(fileRepository, groupId)
    )
    
    val files by viewModel.files.collectAsState()
    var selectedFileType by remember { mutableStateOf<FileType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val userId = CurrentSession.userIdInt ?: 0
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                // TODO: 保存文件并创建GroupFile记录
                val fileName = getFileNameFromUri(context, it)
                val filePath = saveFileFromUri(context, it, fileName)
                
                if (filePath != null) {
                    val file = GroupFile(
                        groupId = groupId,
                        uploaderId = userId,
                        fileName = fileName,
                        filePath = filePath,
                        fileType = getFileTypeFromName(fileName),
                        fileSize = File(filePath).length(),
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.uploadFile(file)
                }
            }
        }
    }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val fileName = "image_${System.currentTimeMillis()}.jpg"
                val filePath = saveFileFromUri(context, it, fileName)
                
                if (filePath != null) {
                    val file = GroupFile(
                        groupId = groupId,
                        uploaderId = userId,
                        fileName = fileName,
                        filePath = filePath,
                        fileType = FileType.IMAGE,
                        fileSize = File(filePath).length(),
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.uploadFile(file)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "文件库",
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
                    // 上传文件按钮
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Upload, contentDescription = "上传文件")
                    }
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "上传图片")
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
            // 搜索栏和筛选
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索文件...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                
                // 文件类型筛选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFileType == null,
                        onClick = { selectedFileType = null },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = selectedFileType == FileType.IMAGE,
                        onClick = { selectedFileType = FileType.IMAGE },
                        label = { Text("图片") }
                    )
                    FilterChip(
                        selected = selectedFileType == FileType.DOCUMENT,
                        onClick = { selectedFileType = FileType.DOCUMENT },
                        label = { Text("文档") }
                    )
                    FilterChip(
                        selected = selectedFileType == FileType.OTHER,
                        onClick = { selectedFileType = FileType.OTHER },
                        label = { Text("其他") }
                    )
                }
            }
            
            // 文件列表
            val filteredFiles = remember(files, selectedFileType, searchQuery) {
                files.filter { file ->
                    (selectedFileType == null || file.fileType == selectedFileType) &&
                    (searchQuery.isBlank() || file.fileName.contains(searchQuery, ignoreCase = true))
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredFiles.isEmpty()) {
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
                                    text = "📁",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Text(
                                    text = "暂无文件",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "点击右上角按钮上传文件",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredFiles) { file ->
                        FileItemCard(
                            file = file,
                            onDownload = {
                                scope.launch {
                                    viewModel.downloadFile(context, file)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteFile(file)
                                }
                            },
                            canDelete = file.uploaderId == userId // 只有上传者可以删除
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemCard(
    file: GroupFile,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val fileIcon = when (file.fileType) {
        FileType.IMAGE -> "🖼️"
        FileType.DOCUMENT -> "📄"
        FileType.VIDEO -> "🎥"
        FileType.AUDIO -> "🎵"
        FileType.TEXT -> "📝"
        FileType.OTHER -> "📎"
    }
    
    // 预先计算图片URI（在Composable外部处理异常）
    val imageUri = remember(file.filePath, file.fileType) {
        if (file.fileType == FileType.IMAGE) {
            getImageUri(context, file.filePath)
        } else {
            null
        }
    }
    
    val imageExists = remember(file.filePath, file.fileType) {
        if (file.fileType == FileType.IMAGE) {
            File(file.filePath).exists()
        } else {
            false
        }
    }
    
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标/预览（可点击查看）
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clickable { onDownload() },
                contentAlignment = Alignment.Center
            ) {
                if (file.fileType == FileType.IMAGE && imageExists && imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = file.fileName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = fileIcon,
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                    }
                }
            }
            
            // 文件信息（可点击查看）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDownload() },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = formatFileSize(file.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatFileTime(file.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // 操作按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("下载") },
                        onClick = {
                            showMenu = false
                            onDownload()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    )
                    if (canDelete) {
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
    }
}

// 工具函数
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatFileTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getFileTypeFromName(fileName: String): FileType {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "jpg", "jpeg", "png", "gif", "webp" -> FileType.IMAGE
        "pdf", "doc", "docx", "xls", "xlsx" -> FileType.DOCUMENT
        "txt", "md", "log" -> FileType.TEXT
        "mp4", "avi", "mov", "mkv" -> FileType.VIDEO
        "mp3", "wav", "aac" -> FileType.AUDIO
        else -> FileType.OTHER
    }
}

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.let {
            val cut = it.lastIndexOf('/')
            if (cut != -1) {
                it.substring(cut + 1)
            } else {
                it
            }
        } ?: "unknown_file"
    }
    return result
}

fun saveFileFromUri(context: android.content.Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val filesDir = context.filesDir
        val file = File(filesDir, "group_files/$fileName")
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

fun getImageUri(context: android.content.Context, filePath: String): Uri? {
    return try {
        val fileObj = File(filePath)
        if (fileObj.exists()) {
            try {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    fileObj
                )
            } catch (e: Exception) {
                android.net.Uri.fromFile(fileObj)
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

