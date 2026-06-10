package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
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
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.ui.viewmodel.CourseViewModel
import com.example.myapplication.ui.viewmodel.CourseViewModelFactory
import com.example.myapplication.utils.ScheduleSettingsManager
import com.example.myapplication.utils.ColorSchemeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCourseScreen(navController: NavHostController, courseId: Int?) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = CourseRepository(database.courseDao())
    val userRepository = com.example.myapplication.data.repository.UserRepository(database.userDao())
    val viewModel: CourseViewModel = viewModel(
        factory = CourseViewModelFactory(repository, userRepository)
    )
    
    val course by viewModel.selectedCourse.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(courseId) {
        if (courseId != null) {
            scope.launch {
                val c = repository.getCourseById(courseId)
                if (c != null) {
                    viewModel.selectCourse(c)
                }
            }
        }
    }
    
    if (course == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    // 保存course的引用以避免智能转换问题
    val currentCourse = course!!
    
    var courseName by remember { mutableStateOf(currentCourse.courseName) }
    var teacherName by remember { mutableStateOf(currentCourse.teacherName ?: "") }
    var location by remember { mutableStateOf(currentCourse.location ?: "") }
    var dayOfWeek by remember { mutableStateOf(currentCourse.dayOfWeek) }
    var startTime by remember { mutableStateOf(currentCourse.startTime) }
    var endTime by remember { mutableStateOf(currentCourse.endTime) }
    // 获取设置
    val settingsManager = remember { ScheduleSettingsManager(context) }
    val settings = remember { settingsManager.getSettings() }
    
    // 初始化选中的周数
    var selectedWeeks by remember(currentCourse) { 
        mutableStateOf<Set<Int>>(
            if (!currentCourse.weeks.isNullOrBlank()) {
                currentCourse.weeks.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            } else {
                (currentCourse.startWeek..currentCourse.endWeek).toSet()
            }
        )
    }
    var showWeekSelector by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(currentCourse.reminderEnabled) }
    var reminderMinutes by remember { mutableStateOf(currentCourse.reminderMinutes) }
    var color by remember { mutableStateOf(currentCourse.color.ifBlank { "#2196F3" }) }
    var textColor by remember { mutableStateOf<String?>(currentCourse.textColor) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "编辑课程",
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
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 基本信息卡片
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "基本信息",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = { courseName = it },
                            label = { Text("课程名称 *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text("任课教师") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("上课地点") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                // 时间设置卡片
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "时间设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "星期",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                weekDays.forEachIndexed { index, day ->
                                    val isSelected = dayOfWeek == index + 1
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { dayOfWeek = index + 1 },
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("开始时间") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("结束时间") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        Text(
                            text = "上课周数",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                val weeksText = when {
                                    selectedWeeks.isEmpty() -> "未选择"
                                    selectedWeeks.size == 1 -> "第${selectedWeeks.first()}周"
                                    selectedWeeks.size == settings.totalWeeks -> "全部周"
                                    else -> {
                                        val sorted = selectedWeeks.sorted()
                                        if (sorted.size <= 5) {
                                            sorted.joinToString("、") { "第${it}周" }
                                        } else {
                                            "已选${selectedWeeks.size}周"
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = weeksText,
                                    onValueChange = { },
                                    label = { Text("上课周数") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { showWeekSelector = true }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "选择周数"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 周数选择对话框
                        if (showWeekSelector) {
                            WeekSelectionDialog(
                                totalWeeks = settings.totalWeeks,
                                selectedWeeks = selectedWeeks,
                                onDismiss = { showWeekSelector = false },
                                onConfirm = { weeks ->
                                    selectedWeeks = weeks
                                    showWeekSelector = false
                                }
                            )
                        }
                    }
                }
                
                // 颜色设置卡片
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "颜色设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // 背景颜色选择
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "背景颜色",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (color.isBlank()) {
                                    TextButton(onClick = { showColorPicker = true }) {
                                        Text("使用主题颜色")
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                ColorSchemeUtils.parseColor(color),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { showColorPicker = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "选择",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if ((ColorSchemeUtils.parseColor(color).red * 0.299 + ColorSchemeUtils.parseColor(color).green * 0.587 + ColorSchemeUtils.parseColor(color).blue * 0.114) > 0.5) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 文字颜色选择
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "文字颜色",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (textColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                ColorSchemeUtils.parseColor(textColor!!),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { 
                                                textColor = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "自动",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if ((ColorSchemeUtils.parseColor(textColor!!).red * 0.299 + ColorSchemeUtils.parseColor(textColor!!).green * 0.587 + ColorSchemeUtils.parseColor(textColor!!).blue * 0.114) > 0.5) Color.Black else Color.White
                                        )
                                    }
                                } else {
                                    TextButton(onClick = { showColorPicker = true }) {
                                        Text("自动（根据背景）")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 提醒设置卡片
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "提醒设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "启用提醒",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        
                        if (reminderEnabled) {
                            OutlinedTextField(
                                value = reminderMinutes.toString(),
                                onValueChange = { reminderMinutes = it.toIntOrNull() ?: 15 },
                                label = { Text("提前提醒（分钟）") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 颜色选择器对话框
                if (showColorPicker) {
                    ColorPickerDialog(
                        currentColor = color,
                        currentTextColor = textColor,
                        onDismiss = { showColorPicker = false },
                        onColorSelected = { bgColor: String, txtColor: String? ->
                            color = bgColor
                            textColor = txtColor
                            showColorPicker = false
                        }
                    )
                }
                
                Button(
                    onClick = {
                        if (courseName.isNotBlank() && selectedWeeks.isNotEmpty()) {
                            // 将选中的周数转换为字符串
                            val weeksString = selectedWeeks.sorted().joinToString(",")
                            // 计算startWeek和endWeek用于向后兼容
                            val minWeek = selectedWeeks.minOrNull() ?: 1
                            val maxWeek = selectedWeeks.maxOrNull() ?: settings.totalWeeks
                            
                            val updatedCourse = currentCourse.copy(
                                courseName = courseName,
                                courseCode = currentCourse.courseCode,
                                teacherName = teacherName.ifBlank { null },
                                location = location.ifBlank { null },
                                dayOfWeek = dayOfWeek,
                                startTime = startTime,
                                endTime = endTime,
                                startWeek = minWeek,
                                endWeek = maxWeek,
                                weeks = weeksString,
                                reminderEnabled = reminderEnabled,
                                reminderMinutes = reminderMinutes,
                                color = color,
                                textColor = textColor
                            )
                            viewModel.updateCourse(updatedCourse, context)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "保存课程",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


