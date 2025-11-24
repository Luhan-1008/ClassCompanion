package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import android.net.Uri
import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import com.example.myapplication.utils.CourseImportParser
import com.example.myapplication.session.CurrentSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.StudyGroupRepository
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.CourseViewModel
import com.example.myapplication.ui.viewmodel.CourseViewModelFactory
import java.util.*

enum class ScheduleViewType {
    DAY,    // 日视图
    WEEK,   // 周视图
    SEMESTER // 学期视图
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = CourseRepository(database.courseDao())
    val assignmentRepository = AssignmentRepository(database.assignmentDao())
    val viewModel: CourseViewModel = viewModel(
        factory = CourseViewModelFactory(repository)
    )
    
    val courses by viewModel.courses.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val insertSuccess by viewModel.insertSuccess.collectAsState()
    var selectedDay by remember { mutableStateOf(getCurrentDayOfWeek()) }
    var viewType by remember { mutableStateOf(ScheduleViewType.DAY) }
    var selectedCourse by remember { mutableStateOf<com.example.myapplication.data.model.Course?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val userId = CurrentSession.userIdInt ?: 0
    
    // 导出功能
    fun exportCourses(share: Boolean) {
        scope.launch {
            try {
                val csvContent = withContext(Dispatchers.IO) {
                    CourseImportParser.exportCoursesToCsv(courses)
                }
                
                val fileName = "Course_Schedule_${System.currentTimeMillis()}.csv"
                
                if (share) {
                    // 分享文件
                    val file = File(context.cacheDir, fileName)
                    withContext(Dispatchers.IO) {
                        file.writeText(csvContent)
                    }
                    
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    context.startActivity(Intent.createChooser(intent, "导出课程表"))
                } else {
                    // 保存到本地
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { stream ->
                                stream.write(csvContent.toByteArray())
                            }
                            importResult = "已保存到下载目录"
                            showImportDialog = true
                        } ?: run {
                            importResult = "保存失败"
                            showImportDialog = true
                        }
                    } else {
                        // 旧版本直接保存到 SD 卡根目录
                        val sdCardDir = Environment.getExternalStorageDirectory()
                        val file = File(sdCardDir, fileName)
                        withContext(Dispatchers.IO) {
                            file.writeText(csvContent)
                        }
                        importResult = "已保存到SD卡: ${file.absolutePath}"
                        showImportDialog = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                importResult = "导出失败: ${e.message}"
                showImportDialog = true
            }
        }
    }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            exportCourses(share = false)
        } else {
            importResult = "需要存储权限才能保存到SD卡"
            showImportDialog = true
        }
    }

    val assignments by remember(userId) {
        assignmentRepository.getAssignmentsByUser(userId)
    }.collectAsState(initial = emptyList())
    val pendingTasksByCourse = remember(assignments) {
        assignments
            .filter { it.status != AssignmentStatus.COMPLETED }
            .groupBy { it.courseId }
    }
    
    val studyGroupRepository = remember { StudyGroupRepository(database.studyGroupDao()) }
    
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    // 文件选择器 - 只接受Excel和CSV文件
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val fileName = getFileNameFromUri(context, it)
                    val inputStream = context.contentResolver.openInputStream(it)
                    
                    if (inputStream != null) {
                        val parsedCourses = withContext(Dispatchers.IO) {
                            CourseImportParser.parseCourses(inputStream, fileName, userId)
                        }
                        inputStream.close()
                        
                        if (parsedCourses.isNotEmpty()) {
                            viewModel.importCourses(parsedCourses)
                            importResult = "成功解析 ${parsedCourses.size} 门课程，正在导入..."
                        } else {
                            importResult = "文件中没有找到有效的课程数据"
                        }
                        showImportDialog = true
                    } else {
                        importResult = "无法读取文件"
                        showImportDialog = true
                    }
                } catch (e: Exception) {
                    importResult = "导入失败: ${e.message}"
                    showImportDialog = true
                    e.printStackTrace()
                }
            }
        }
    }
    
    // 监听导入结果
    LaunchedEffect(insertSuccess, errorMessage) {
        if (insertSuccess) {
            importResult = "课程导入成功！"
            viewModel.resetInsertSuccess()
        } else if (errorMessage != null) {
            importResult = errorMessage
        }
    }
    
    // 获取文件名
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "课程表",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when(viewType) {
                                ScheduleViewType.DAY -> weekDays[selectedDay - 1]
                                ScheduleViewType.WEEK -> "周视图"
                                ScheduleViewType.SEMESTER -> "学期视图"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // 导入按钮
                    IconButton(
                        onClick = {
                            // 支持Excel和CSV文件
                            filePickerLauncher.launch("application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/csv,text/comma-separated-values")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "导入课程"
                        )
                    }
                    // 导出按钮
                    IconButton(
                        onClick = {
                            showExportDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "导出课程"
                        )
                    }
                    // 视图切换
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = viewType == ScheduleViewType.DAY,
                            onClick = { viewType = ScheduleViewType.DAY },
                            label = { Text("日", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = viewType == ScheduleViewType.WEEK,
                            onClick = { viewType = ScheduleViewType.WEEK },
                            label = { Text("周", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddCourse.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加课程")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (viewType == ScheduleViewType.DAY) {
                // 星期选择器 - 仅在日视图显示
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weekDays.forEachIndexed { index, day ->
                            val isSelected = selectedDay == index + 1
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDay = index + 1
                                    viewModel.getCoursesByDay(selectedDay)
                                },
                                label = {
                                    Text(
                                        day,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderWidth = 2.dp,
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 根据视图类型显示不同内容
            when (viewType) {
                ScheduleViewType.DAY -> {
                    // 日视图：课程列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val dayCourses = courses.filter { it.dayOfWeek == selectedDay }
                            .sortedBy { it.startTime }
                        
                        if (dayCourses.isEmpty()) {
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
                                            text = "📅",
                                            style = MaterialTheme.typography.displayLarge
                                        )
                                        Text(
                                            text = "今天没有课程",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "点击右下角按钮添加课程",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(dayCourses) { course ->
                                val pendingCount = pendingTasksByCourse[course.courseId]?.size ?: 0
                                val groupsForCourse by remember(course.courseId) {
                                    studyGroupRepository.getGroupsByCourse(course.courseId)
                                }.collectAsState(initial = emptyList())
                                CourseCard(
                                    course = course,
                                    pendingTaskCount = pendingCount,
                                    groups = groupsForCourse,
                                    onClick = { selectedCourse = course },
                                    onViewTasks = if (pendingCount > 0) {
                                        {
                                            navController.navigate("${Screen.Assignments.route}?courseId=${course.courseId}")
                                        }
                                    } else null,
                                    onViewGroups = if (groupsForCourse.isNotEmpty()) {
                                        {
                                            navController.navigate("${Screen.GroupDetail.route}/${groupsForCourse.first().groupId}")
                                        }
                                    } else null,
                                    onEdit = {
                                        navController.navigate("${Screen.EditCourse.route}/${course.courseId}")
                                    },
                                    onDelete = {
                                        viewModel.deleteCourse(course)
                                    }
                                )
                            }
                        }
                    }
                }
                ScheduleViewType.WEEK -> {
                    // 周视图
                    Spacer(modifier = Modifier.height(8.dp))
                    WeekViewScreen(
                        courses = courses,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        onCourseClick = { course ->
                            selectedCourse = course
                        }
                    )
                }
                ScheduleViewType.SEMESTER -> {
                    // 学期视图（待实现）
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("学期视图功能开发中...")
                    }
                }
            }
        }
        
        // 课程详情对话框
        selectedCourse?.let { course ->
            CourseDetailDialog(
                course = course,
                onDismiss = { selectedCourse = null },
                onEdit = {
                    navController.navigate("${Screen.EditCourse.route}/${course.courseId}")
                },
                onNavigate = { location ->
                    openMapNavigation(context, location)
                }
            )
        }
        
        // 导出选项对话框
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Text(
                        text = "导出课程表",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showExportDialog = false
                                exportCourses(share = true)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("分享文件")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showExportDialog = false
                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                        exportCourses(share = false)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                } else {
                                    exportCourses(share = false)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存到本地")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("取消")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // 导入结果对话框
        if (showImportDialog && importResult != null) {
            AlertDialog(
                onDismissRequest = { 
                    showImportDialog = false
                    importResult = null
                },
                title = {
                    Text(
                        text = "导入结果",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = importResult ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showImportDialog = false
                            importResult = null
                        }
                    ) {
                        Text("确定")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
        
        // 模板对话框
        if (showTemplateDialog) {
            val templateContent = CourseImportParser.generateCsvTemplate()
            AlertDialog(
                onDismissRequest = { showTemplateDialog = false },
                title = {
                    Text(
                        text = "CSV模板",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "请按照以下格式准备CSV文件：",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = templateContent,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "说明：\n" +
                                    "• 第一行为表头，必须保留\n" +
                                    "• 星期： 1-7 （1=周一， 7=周日） 或中文 （周一-周日）\n" +
                                    "• 时间格式： HH:mm （如 08:00）\n" +
                                    "• 开始周和结束周： 数字 （如 1-16）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showTemplateDialog = false }
                    ) {
                        Text("确定")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseCard(
    course: com.example.myapplication.data.model.Course,
    pendingTaskCount: Int = 0,
    groups: List<com.example.myapplication.data.model.StudyGroup> = emptyList(),
    onViewTasks: (() -> Unit)? = null,
    onViewGroups: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val courseColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(course.color))
    val dayLabel = getDayName(course.dayOfWeek)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = course.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dayLabel · ${course.startTime} - ${course.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = courseColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoTag(
                    icon = "⏱",
                    text = "${course.startTime} - ${course.endTime}",
                    color = courseColor
                )
                InfoTag(
                    icon = "📆",
                    text = "第${course.startWeek}-${course.endWeek}周",
                    color = courseColor
                )
                if (!course.location.isNullOrEmpty()) {
                    InfoTag(
                        icon = "📍",
                        text = course.location,
                        color = courseColor
                    )
                }
                if (!course.teacherName.isNullOrEmpty()) {
                    InfoTag(
                        icon = "👤",
                        text = course.teacherName,
                        color = courseColor
                    )
                }
            }
            
            if (pendingTaskCount > 0 || groups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pendingTaskCount > 0) {
                        OutlinedButton(
                            onClick = { onViewTasks?.invoke() },
                            enabled = onViewTasks != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, courseColor.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = courseColor,
                                disabledContentColor = courseColor.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "查看任务",
                                tint = courseColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("查看未完成任务 ($pendingTaskCount)", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (groups.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onViewGroups?.invoke() },
                            enabled = onViewGroups != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, courseColor.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = courseColor,
                                disabledContentColor = courseColor.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("👥", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("查看学习小组 (${groups.size})", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTag(
    icon: String,
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.bodySmall)
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun getCurrentDayOfWeek(): Int {
    val calendar = Calendar.getInstance()
    var day = calendar.get(Calendar.DAY_OF_WEEK)
    // Calendar中周日是1，周一至周六是2-7，需要转换为1-7（周一是1）
    day = if (day == Calendar.SUNDAY) 7 else day - 1
    return day
}

fun openMapNavigation(context: android.content.Context, location: String) {
    try {
        // 尝试使用高德地图
        val amapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("androidamap://navi?sourceApplication=课程伴侣&poiname=$location&lat=0&lon=0&dev=0")
            setPackage("com.autonavi.minimap")
        }
        if (amapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(amapIntent)
            return
        }
    } catch (e: Exception) {
        // 忽略
    }
    
    try {
        // 尝试使用百度地图
        val baiduIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("baidumap://map/direction?destination=$location&mode=walking")
            setPackage("com.baidu.BaiduMap")
        }
        if (baiduIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(baiduIntent)
            return
        }
    } catch (e: Exception) {
        // 忽略
    }
    
    // 使用通用地图搜索
    val searchIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        data = android.net.Uri.parse("geo:0,0?q=$location")
    }
    if (searchIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(searchIntent)
    }
}

