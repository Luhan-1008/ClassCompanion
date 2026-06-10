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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import android.net.Uri
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
import com.example.myapplication.utils.ScheduleSettingsManager
import com.example.myapplication.utils.ColorSchemeUtils
import com.example.myapplication.utils.ScheduleDateUtils
import com.example.myapplication.utils.WeekDisplayUtils
import com.example.myapplication.data.model.ScheduleSettings
import com.example.myapplication.service.PreciseReminderService
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale

enum class ScheduleViewType {
    DAY,    // 日视图
    WEEK,   // 周视图
    ALL,    // 所有课程
    SEMESTER // 学期视图
}

private enum class CourseExportFormat(val label: String) {
    CSV("CSV"),
    EXCEL("Excel")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = CourseRepository(database.courseDao())
    val userRepository = com.example.myapplication.data.repository.UserRepository(database.userDao())
    val assignmentRepository = AssignmentRepository(database.assignmentDao())
    val viewModel: CourseViewModel = viewModel(
        factory = CourseViewModelFactory(repository, userRepository)
    )
    
    val courses by viewModel.courses.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val insertSuccess by viewModel.insertSuccess.collectAsState()
    val lastImportedIds by viewModel.lastImportedCourseIds.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userId = CurrentSession.userIdInt ?: 0
    
    // 设置管理器
    val settingsManager = remember { ScheduleSettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.getSettings()) }
    
    // 根据设置计算可见的日期索引（1-7，1为周一）
    val visibleDays = remember(settings.showSaturday, settings.showSunday) {
        val days = mutableListOf<Int>()
        (1..5).forEach { days.add(it) } // 周一到周五
        if (settings.showSaturday) days.add(6) // 周六
        if (settings.showSunday) days.add(7) // 周日
        days
    }
    
    // 初始化选中的日期，如果当前日期不可见，则选择第一个可见的日期
    var selectedDay by remember { 
        val currentDay = getCurrentDayOfWeek()
        mutableStateOf(currentDay)
    }
    
    // 当可见日期改变时，如果当前选中的日期不可见，则调整到第一个可见的日期
    LaunchedEffect(visibleDays) {
        if (selectedDay !in visibleDays) {
            selectedDay = visibleDays.firstOrNull() ?: 1
        }
    }
    
    var currentWeek by remember(settings) { 
        mutableStateOf(ScheduleDateUtils.getCurrentWeek(settings)) 
    }
    var viewType by remember { mutableStateOf(ScheduleViewType.DAY) }
    var selectedCourse by remember { mutableStateOf<com.example.myapplication.data.model.Course?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showTopBarMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<com.example.myapplication.data.model.Course?>(null) }
    
    val assignments by remember(userId) {
        assignmentRepository.getAssignmentsByUser(userId)
    }.collectAsState(initial = emptyList())
    val pendingTasksByCourse = remember(assignments) {
        assignments
            .filter { it.status != AssignmentStatus.COMPLETED }
            .groupBy { it.courseId }
    }
    
    val studyGroupRepository = remember { StudyGroupRepository(database.studyGroupDao()) }
    
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    
    // 计算本周日期
    val weekDates = remember(currentWeek, settings) {
        val calendar = ScheduleDateUtils.getWeekStartDate(currentWeek, settings)
        (0..6).map { 
            val date = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            java.text.SimpleDateFormat("MM.dd", java.util.Locale.getDefault()).format(date)
        }
    }
    
    // 文件选择器 - 只接受Excel和CSV文件
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val fileName = getFileNameFromUri(context, it)
                    println("Debug: Selected URI: $it, Filename: $fileName")
                    val inputStream = context.contentResolver.openInputStream(it)
                    
                    if (inputStream != null) {
                        val parsedCourses = withContext(Dispatchers.IO) {
                            CourseImportParser.parseCourses(inputStream, fileName, userId, settings.colorScheme)
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
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    if (courses.isEmpty()) {
                        importResult = "当前没有课程可导出"
                        showImportDialog = true
                        return@launch
                    }
                    val bytes = withContext(Dispatchers.IO) {
                        CourseImportParser.exportCoursesToWeeklyExcel(courses)
                    }
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(bytes)
                    } ?: run {
                        importResult = "无法写入文件"
                        showImportDialog = true
                        return@launch
                    }
                    importResult = "导出成功，已保存课程表"
                } catch (e: Exception) {
                    importResult = "导出失败: ${e.message}"
                }
                showImportDialog = true
            }
        }
    }
    
    val exportFileName = remember {
        val sdf = SimpleDateFormat("课程表_yyyyMMdd_HHmm'.xlsx'", Locale.getDefault())
        sdf.format(Date())
    }
    
    // 监听导入结果
    LaunchedEffect(insertSuccess, errorMessage) {
        if (insertSuccess) {
            importResult = "课程导入成功！"
            viewModel.resetInsertSuccess()
            
            // 显示成功 Snackbar
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "成功导入课程",
                    duration = SnackbarDuration.Short
                )
            }
        } else if (errorMessage != null) {
            // 如果是撤销操作的消息，显示在 Snackbar
            if (errorMessage == "已撤销导入") {
                scope.launch {
                    snackbarHostState.showSnackbar("已撤销刚才的导入")
                }
                viewModel.clearError()
            } else if (errorMessage?.startsWith("撤销失败") == true) {
                 scope.launch {
                    snackbarHostState.showSnackbar(errorMessage ?: "撤销失败")
                }
                viewModel.clearError()
            } else if (errorMessage == "已清空所有课程") {
                scope.launch {
                    snackbarHostState.showSnackbar("已清空所有课程")
                }
                viewModel.clearError()
            } else {
                importResult = errorMessage
            }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = settings.scheduleName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 视图切换
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 日/周切换按钮
                        FilterChip(
                            selected = viewType == ScheduleViewType.DAY || viewType == ScheduleViewType.WEEK,
                            onClick = { 
                                val newViewType = if (viewType == ScheduleViewType.DAY) {
                                    ScheduleViewType.WEEK
                                } else {
                                    ScheduleViewType.DAY
                                }
                                viewType = newViewType
                                if (newViewType == ScheduleViewType.WEEK) {
                                    viewModel.loadAllCourses()
                                }
                            },
                            label = { 
                                Text(
                                    if (viewType == ScheduleViewType.DAY) "周" else "日",
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            }
                        )
                        FilterChip(
                            selected = viewType == ScheduleViewType.ALL,
                            onClick = { 
                                viewType = ScheduleViewType.ALL
                                viewModel.loadAllCourses()
                            },
                            label = { Text("全部", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    // 更多菜单
                    Box {
                        IconButton(onClick = { showTopBarMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多选项"
                            )
                        }
                        DropdownMenu(
                            expanded = showTopBarMenu,
                            onDismissRequest = { showTopBarMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("导入课程") 
                                    }
                                },
                                onClick = {
                                    showTopBarMenu = false
                                    filePickerLauncher.launch("*/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("导出课程表") 
                                    }
                                },
                                onClick = {
                                    showTopBarMenu = false
                                    exportLauncher.launch(exportFileName)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("课表设置") 
                                    }
                                },
                                onClick = {
                                    showTopBarMenu = false
                                    showSettingsDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Delete, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("清空所有课程", color = MaterialTheme.colorScheme.error) 
                                    }
                                },
                                onClick = {
                                    showTopBarMenu = false
                                    showClearConfirmDialog = true
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddCourse.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
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
                // 统一表头（与周视图一致）
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // 左侧：周次选择
                    Box(
                        modifier = Modifier.width(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        var showWeekSelector by remember { mutableStateOf(false) }
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "第${currentWeek}周",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showWeekSelector = true }
                                )
                            }
                            DropdownMenu(
                                expanded = showWeekSelector,
                                onDismissRequest = { showWeekSelector = false }
                            ) {
                                (1..settings.totalWeeks).forEach { week ->
                                    DropdownMenuItem(
                                        text = { Text("第${week}周") },
                                        onClick = {
                                            currentWeek = week
                                            showWeekSelector = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // 右侧：星期选择（根据设置过滤周六日）
                    weekDays.forEachIndexed { index, day ->
                        val dayOfWeek = index + 1 // 1-7，1为周一
                        // 只显示可见的日期
                        if (dayOfWeek !in visibleDays) return@forEachIndexed
                        
                        val isSelected = selectedDay == dayOfWeek
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDay = dayOfWeek }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = weekDates.getOrElse(index) { "" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
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
                        val dayCourses = courses.filter { 
                            if (it.dayOfWeek != selectedDay) return@filter false
                            
                            // 检查课程是否在当前周
                            if (!it.weeks.isNullOrBlank()) {
                                // 如果weeks字段不为空，使用精确的周数列表
                                it.weeks.split(",").mapNotNull { week -> week.trim().toIntOrNull() }.contains(currentWeek)
                            } else {
                                // 否则使用startWeek和endWeek的范围
                                currentWeek >= it.startWeek && currentWeek <= it.endWeek
                            }
                        }.sortedBy { it.startTime }
                        
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
                                    settings = settings,
                                    courseIndex = (course.courseId ?: 0) % 7,
                                    pendingTaskCount = pendingCount,
                                    groups = groupsForCourse,
                                    hideDayTime = true,
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
                    WeekViewScreen(
                        courses = courses,
                        settings = settings,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        onCourseClick = { course ->
                            selectedCourse = course
                        }
                    )
                }
                ScheduleViewType.ALL -> {
                    // 所有课程列表
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 按课程名分组，然后按周数排序
                        val allCourses = courses.sortedWith(
                            compareBy(
                                { it.courseName },
                                { it.startWeek },
                                { it.endWeek },
                                { it.dayOfWeek },
                                { it.startTime }
                            )
                        )
                        
                        if (allCourses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无课程数据")
                                }
                            }
                        } else {
                            items(allCourses) { course ->
                                val pendingCount = pendingTasksByCourse[course.courseId]?.size ?: 0
                                val groupsForCourse by remember(course.courseId) {
                                    studyGroupRepository.getGroupsByCourse(course.courseId)
                                }.collectAsState(initial = emptyList())
                                CourseCard(
                                    course = course,
                                    settings = settings,
                                    courseIndex = (course.courseId ?: 0) % 7,
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
                onDelete = {
                    showDeleteConfirmDialog = course
                }
            )
        }
        
        // 删除确认对话框
        showDeleteConfirmDialog?.let { course ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = {
                    Text(
                        text = "确认删除",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "确定要删除课程「${course.courseName}」吗？此操作无法撤销。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // 取消课程提醒
                            PreciseReminderService.cancelCourseReminder(context, course.courseId)
                            // 删除课程
                            viewModel.deleteCourse(course)
                            showDeleteConfirmDialog = null
                            selectedCourse = null
                            scope.launch {
                                snackbarHostState.showSnackbar("已删除课程")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
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
        
        // 清空确认对话框
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("确认清空") },
                text = { Text("确定要删除所有课程吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllCourses()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 课表设置对话框
        if (showSettingsDialog) {
            ScheduleSettingsDialog(
                settings = settings,
                onDismiss = { showSettingsDialog = false },
                onSave = { newSettings ->
                    settings = newSettings
                    settingsManager.saveSettings(newSettings)
                    scope.launch {
                        snackbarHostState.showSnackbar("设置已保存")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseCard(
    course: com.example.myapplication.data.model.Course,
    settings: ScheduleSettings,
    courseIndex: Int = 0,
    pendingTaskCount: Int = 0,
    groups: List<com.example.myapplication.data.model.StudyGroup> = emptyList(),
    hideDayTime: Boolean = false,
    onViewTasks: (() -> Unit)? = null,
    onViewGroups: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // 使用课程自定义颜色，如果为空则使用配色方案
    // 结合课程名称、星期和时间段分配颜色，避免相邻课程使用相同颜色
    val courseColorStr = if (course.color.isBlank()) {
        ColorSchemeUtils.getColorForCourseByName(
            courseName = course.courseName,
            colorScheme = settings.colorScheme,
            dayOfWeek = course.dayOfWeek,
            startTime = course.startTime
        )
    } else {
        course.color
    }
    val courseColor = ColorSchemeUtils.parseColor(courseColorStr)
    val textColor = if (!course.textColor.isNullOrBlank()) {
        ColorSchemeUtils.parseColor(course.textColor)
    } else {
        when (settings.colorScheme) {
            "default", "green" -> Color.White
            "pastel" -> Color.Black.copy(alpha = 0.7f) // 浅色系使用浅黑色文字
            else -> {
                // 其他主题根据背景颜色自动计算文字颜色
                val brightness = (courseColor.red * 0.299 + courseColor.green * 0.587 + courseColor.blue * 0.114)
                if (brightness > 0.5) Color.Black else Color.White
            }
        }
    }
    val dayLabel = getDayNameForCourse(course.dayOfWeek)
    
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
                    if (!hideDayTime) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$dayLabel · ${course.startTime} - ${course.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = courseColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                    text = if (!course.weeks.isNullOrBlank()) {
                        WeekDisplayUtils.formatWeeksFromString(course.weeks)
                    } else {
                        WeekDisplayUtils.formatWeekRange(course.startWeek, course.endWeek)
                    },
                    color = courseColor
                )
                // 根据设置显示教室和教师
                if (settings.showLocation && !course.location.isNullOrEmpty()) {
                    InfoTag(
                        icon = "📍",
                        text = course.location,
                        color = courseColor
                    )
                }
                if (settings.showTeacher && !course.teacherName.isNullOrEmpty()) {
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
                            Text(
                                "查看未完成任务 ($pendingTaskCount)", 
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
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

@Composable
fun CourseDetailDialog(
    course: com.example.myapplication.data.model.Course,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = try {
        Color(android.graphics.Color.parseColor(course.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 课程名称
                CourseInfoRow("课程名称", course.courseName)
                
                // 教师
                if (!course.teacherName.isNullOrEmpty()) {
                    CourseInfoRow("任课教师", course.teacherName)
                }
                
                // 上课时间
                CourseInfoRow(
                    "上课时间",
                    "${getDayNameForCourse(course.dayOfWeek)} ${course.startTime} - ${course.endTime}"
                )
                
                // 教学周
                val weeksText = if (!course.weeks.isNullOrBlank()) {
                    WeekDisplayUtils.formatWeeksFromString(course.weeks)
                } else {
                    WeekDisplayUtils.formatWeekRange(course.startWeek, course.endWeek)
                }
                CourseInfoRow("教学周", weeksText)
                
                // 地点
                if (!course.location.isNullOrEmpty()) {
                    CourseInfoRow("上课地点", course.location)
                }
                
                // 提醒设置
                if (course.reminderEnabled) {
                    CourseInfoRow("提醒", "提前${course.reminderMinutes}分钟")
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
                TextButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
                Button(
                    onClick = {
                        onEdit()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = courseColor
                    )
                ) {
                    Text("编辑")
                }
            }
        }
    )
}

@Composable
private fun CourseInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getDayNameForCourse(dayOfWeek: Int): String {
    val days = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return days.getOrElse(dayOfWeek) { "" }
}


