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
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.CourseViewModel
import com.example.myapplication.ui.viewmodel.CourseViewModelFactory
import com.example.myapplication.utils.ScheduleSettingsManager
import com.example.myapplication.utils.ColorSchemeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = CourseRepository(database.courseDao())
    val userRepository = com.example.myapplication.data.repository.UserRepository(database.userDao())
    val viewModel: CourseViewModel = viewModel(
        factory = CourseViewModelFactory(repository, userRepository)
    )
    
    // 获取设置
    val settingsManager = remember { ScheduleSettingsManager(context) }
    val settings = remember { settingsManager.getSettings() }
    
    var courseName by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:35") }
    var selectedWeeks by remember { mutableStateOf<Set<Int>>(setOf(1)) }
    var showWeekSelector by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderMinutes by remember { mutableStateOf(15) }
    var color by remember { mutableStateOf("#2196F3") } // 默认颜色为蓝色（与导入颜色列表的第一个颜色一致）
    var textColor by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val errorMessage by viewModel.errorMessage.collectAsState()
    val insertSuccess by viewModel.insertSuccess.collectAsState()
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    // 监听插入成功
    LaunchedEffect(insertSuccess) {
        if (insertSuccess) {
            viewModel.resetInsertSuccess()
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "添加课程",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
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
                                placeholder = { Text("HH:mm") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("结束时间") },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("HH:mm") },
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
                                    selectedWeeks.size == settings.totalWeeks -> "全部周（${settings.totalWeeks}周）"
                                    else -> {
                                        val sorted = selectedWeeks.sorted()
                                        if (sorted.size <= 8) {
                                            // 显示所有周数
                                            sorted.joinToString("、") { "第${it}周" }
                                        } else {
                                            // 显示前5个和后3个，中间用省略号
                                            val firstFive = sorted.take(5).joinToString("、") { "第${it}周" }
                                            val lastThree = sorted.takeLast(3).joinToString("、") { "第${it}周" }
                                            "$firstFive...$lastThree（共${selectedWeeks.size}周）"
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 显示错误消息
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
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
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("关闭")
                            }
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (courseName.isNotBlank() && selectedWeeks.isNotEmpty()) {
                            // 验证：必须至少选择一周
                            val currentUserId = com.example.myapplication.session.CurrentSession.userIdInt
                            
                            // 将选中的周数转换为字符串
                            val weeksString = selectedWeeks.sorted().joinToString(",")
                            // 计算startWeek和endWeek用于向后兼容
                            val minWeek = selectedWeeks.minOrNull() ?: 1
                            val maxWeek = selectedWeeks.maxOrNull() ?: settings.totalWeeks
                            
                            if (currentUserId == null || currentUserId == 0) {
                                viewModel.insertCourse(
                                    Course(
                                        userId = 0, // 这会触发错误处理
                                        courseName = courseName,
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
                                )
                                return@Button
                            }
                            
                            val course = Course(
                                userId = currentUserId,
                                courseName = courseName,
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
                            viewModel.insertCourse(course, context)
                            // 成功后会通过 LaunchedEffect 自动返回
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeekSelectionDialog(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var selectedWeeksState by remember { mutableStateOf(selectedWeeks) }
    
    // 快捷操作函数
    fun selectAll() {
        selectedWeeksState = (1..totalWeeks).toSet()
    }
    
    fun selectOdd() {
        selectedWeeksState = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
    }
    
    fun selectEven() {
        selectedWeeksState = (1..totalWeeks).filter { it % 2 == 0 }.toSet()
    }
    
    fun clearAll() {
        selectedWeeksState = emptySet()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择上课周数",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 快捷操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { selectAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("全选", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = { selectOdd() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("单周", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = { selectEven() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("双周", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = { clearAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("全不选", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Divider()
                
                // 显示已选择信息（始终显示，包括空选择时）
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedWeeksState.isNotEmpty()) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedWeeksState.isNotEmpty()) {
                            Text(
                                text = "已选择 ${selectedWeeksState.size} 周",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedWeeksState.sorted().joinToString("、") { "第${it}周" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "未选择任何周数",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "请选择上课的周数（可多选）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // 网格布局显示所有周数
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..totalWeeks).forEach { week ->
                        val isSelected = selectedWeeksState.contains(week)
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedWeeksState = if (isSelected) {
                                    selectedWeeksState - week
                                } else {
                                    selectedWeeksState + week
                                }
                            },
                            label = { Text("第${week}周") },
                            modifier = Modifier.padding(vertical = 2.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 允许空选择，全不选时也会清除选项
                    onConfirm(selectedWeeksState)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

// 颜色分类枚举
enum class ColorCategory(val displayName: String) {
    RED("红色系"),
    ORANGE("橙色系"),
    YELLOW("黄色系"),
    GREEN("绿色系"),
    CYAN("青色系"),
    BLUE("蓝色系"),
    PURPLE("紫色系"),
    PINK("粉色系"),
    NEUTRAL("中性色") // 黑白灰等无彩色
}

// 获取颜色的分类
fun getColorCategory(colorStr: String): ColorCategory {
    try {
        val color = android.graphics.Color.parseColor(colorStr)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color),
            hsv
        )
        val hue = hsv[0] // 色相 0-360
        val saturation = hsv[1] // 饱和度 0-1
        val brightness = hsv[2] // 亮度 0-1
        
        // 如果饱和度很低或亮度很低/很高，归类为中性色
        if (saturation < 0.1 || brightness < 0.1 || brightness > 0.95) {
            return ColorCategory.NEUTRAL
        }
        
        // 根据色相范围分类，放宽所有颜色的范围，让相近的颜色归为一类
        return when {
            // 红色系：包括红色、橙色、粉色（0-60度和300-360度）
            (hue >= 0f && hue < 60f) || (hue >= 300f && hue < 360f) -> ColorCategory.RED
            // 黄色系：包括黄色、黄绿色（60-100度）
            hue >= 60f && hue < 100f -> ColorCategory.YELLOW
            // 绿色系：包括绿色、青绿色（100-170度）
            hue >= 100f && hue < 170f -> ColorCategory.GREEN
            // 青色系：包括青色、蓝绿色（170-200度）
            hue >= 170f && hue < 200f -> ColorCategory.CYAN
            // 蓝色系：包括蓝色、蓝紫色（200-280度）
            hue >= 200f && hue < 280f -> ColorCategory.BLUE
            // 紫色系：包括紫色、紫红色（280-300度）
            hue >= 280f && hue < 300f -> ColorCategory.PURPLE
            else -> ColorCategory.NEUTRAL
        }
    } catch (e: Exception) {
        return ColorCategory.NEUTRAL
    }
}

// 获取颜色的HSV值用于排序
fun getColorHSV(colorStr: String): FloatArray? {
    return try {
        val color = android.graphics.Color.parseColor(colorStr)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color),
            hsv
        )
        hsv
    } catch (e: Exception) {
        null
    }
}

// 按分类分组颜色，并对每个分类内的颜色进行排序
fun groupColorsByCategory(colors: List<String>): Map<ColorCategory, List<String>> {
    val grouped = colors.groupBy { getColorCategory(it) }
    // 对每个分类内的颜色进行排序：先按色相（hue），再按亮度（brightness）
    return grouped.mapValues { (category, colorList) ->
        colorList.sortedWith(compareBy(
            { colorStr ->
                val hsv = getColorHSV(colorStr)
                val hue = hsv?.get(0) ?: 0f
                // 对于红色系，将300-360度的色相值标准化，使其排在0-60度之后
                if (category == ColorCategory.RED && hue >= 300f) {
                    hue - 360f // 将300-360度转换为-60到0度，排在0-60度之后
                } else {
                    hue
                }
            },
            { colorStr ->
                val hsv = getColorHSV(colorStr)
                -(hsv?.get(2) ?: 0f) // 按亮度从深到浅排序（降序）
            }
        ))
    }
}

// 将颜色字符串转换为RGB值
fun colorToRgb(colorStr: String): Triple<Int, Int, Int> {
    return try {
        val color = android.graphics.Color.parseColor(if (colorStr.isBlank()) "#000000" else colorStr)
        Triple(
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color)
        )
    } catch (e: Exception) {
        Triple(0, 0, 0)
    }
}

// 将RGB值转换为颜色字符串
fun rgbToColor(r: Int, g: Int, b: Int): String {
    val rClamped = r.coerceIn(0, 255)
    val gClamped = g.coerceIn(0, 255)
    val bClamped = b.coerceIn(0, 255)
    return String.format("#%02X%02X%02X", rClamped, gClamped, bClamped)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    currentColor: String,
    currentTextColor: String?,
    onDismiss: () -> Unit,
    onColorSelected: (String, String?) -> Unit
) {
    var selectedBgColor by remember { mutableStateOf(currentColor) }
    var selectedTextColor by remember { mutableStateOf<String?>(currentTextColor) }
    var isSelectingTextColor by remember { mutableStateOf(false) }
    
    // 初始化RGB值
    val initialRgb = remember(currentColor) { colorToRgb(currentColor) }
    
    // 使用滑块值作为主要数据源
    var rSlider by remember(currentColor) { mutableStateOf(initialRgb.first.toFloat()) }
    var gSlider by remember(currentColor) { mutableStateOf(initialRgb.second.toFloat()) }
    var bSlider by remember(currentColor) { mutableStateOf(initialRgb.third.toFloat()) }
    
    // 根据滑块值计算颜色
    selectedBgColor = remember(rSlider, gSlider, bSlider) {
        rgbToColor(rSlider.toInt(), gSlider.toInt(), bSlider.toInt())
    }
    
    // 输入框值（与滑块同步显示）
    val rValue = remember(rSlider) { rSlider.toInt().toString() }
    val gValue = remember(gSlider) { gSlider.toInt().toString() }
    val bValue = remember(bSlider) { bSlider.toInt().toString() }
    
    // 当前预览颜色
    val previewColor = ColorSchemeUtils.parseColor(selectedBgColor)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSelectingTextColor) "选择文字颜色" else "选择背景颜色",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isSelectingTextColor) {
                    // 颜色预览样例
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = previewColor
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                        Text(
                                text = "颜色预览",
                                style = MaterialTheme.typography.titleMedium,
                                color = if ((previewColor.red * 0.299 + previewColor.green * 0.587 + previewColor.blue * 0.114) > 0.5) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // RGB颜色值
                    Text(
                        text = "RGB颜色值",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                        )
                    
                    // R (红色) 滑块和输入
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "R (红色)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = rSlider.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                        }
                        Slider(
                            value = rSlider,
                            onValueChange = { rSlider = it },
                            valueRange = 0f..255f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE53935),
                                activeTrackColor = Color(0xFFE53935),
                                inactiveTrackColor = Color(0xFFE53935).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    // G (绿色) 滑块和输入
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "G (绿色)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = gSlider.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Slider(
                            value = gSlider,
                            onValueChange = { gSlider = it },
                            valueRange = 0f..255f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50),
                                inactiveTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    // B (蓝色) 滑块和输入
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "B (蓝色)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = bSlider.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        Slider(
                            value = bSlider,
                            onValueChange = { bSlider = it },
                            valueRange = 0f..255f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2196F3),
                                activeTrackColor = Color(0xFF2196F3),
                                inactiveTrackColor = Color(0xFF2196F3).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    // RGB输入框（可选，用于精确输入）
                    Text(
                        text = "精确输入（可选）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // R值输入
                        var rInputValue by remember(rSlider) { mutableStateOf(rSlider.toInt().toString()) }
                        OutlinedTextField(
                            value = rInputValue,
                            onValueChange = { 
                                rInputValue = it
                                it.toIntOrNull()?.coerceIn(0, 255)?.let { value ->
                                    rSlider = value.toFloat()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("R") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        // G值输入
                        var gInputValue by remember(gSlider) { mutableStateOf(gSlider.toInt().toString()) }
                        OutlinedTextField(
                            value = gInputValue,
                            onValueChange = { 
                                gInputValue = it
                                it.toIntOrNull()?.coerceIn(0, 255)?.let { value ->
                                    gSlider = value.toFloat()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("G") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        // B值输入
                        var bInputValue by remember(bSlider) { mutableStateOf(bSlider.toInt().toString()) }
                        OutlinedTextField(
                            value = bInputValue,
                            onValueChange = { 
                                bInputValue = it
                                it.toIntOrNull()?.coerceIn(0, 255)?.let { value ->
                                    bSlider = value.toFloat()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("B") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    
                    // 显示颜色代码
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "颜色代码：",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = selectedBgColor.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 添加"使用主题颜色"选项
                    TextButton(
                        onClick = { 
                            onColorSelected("", null)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用主题颜色（推荐）")
                    }
                } else {
                    // 文字颜色预览样例（在背景颜色上显示文字）
                    val bgColorForPreview = ColorSchemeUtils.parseColor(selectedBgColor)
                    val textColorForPreview = selectedTextColor?.let { ColorSchemeUtils.parseColor(it) } 
                        ?: if ((bgColorForPreview.red * 0.299 + bgColorForPreview.green * 0.587 + bgColorForPreview.blue * 0.114) > 0.5) Color.Black else Color.White
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = bgColorForPreview
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "文字颜色预览",
                                style = MaterialTheme.typography.titleMedium,
                                color = textColorForPreview,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = "选择文字颜色（留空则自动根据背景计算）",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    TextButton(
                        onClick = { selectedTextColor = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用自动颜色（推荐）")
                    }
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 增加浅黑色选项
                        listOf("#000000", "#333333", "#808080", "#FFFFFF").forEach { colorStr ->
                            val color = ColorSchemeUtils.parseColor(colorStr)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { selectedTextColor = colorStr },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedTextColor == colorStr) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = if (colorStr == "#FFFFFF" || colorStr == "#808080") Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSelectingTextColor) {
                        // 如果用户选择了默认颜色，可以设置为空字符串以使用主题颜色
                        // 但为了简单，我们直接使用选择的颜色
                        onColorSelected(selectedBgColor, selectedTextColor)
                    } else {
                        isSelectingTextColor = true
                    }
                }
            ) {
                Text(if (isSelectingTextColor) "确定" else "下一步")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isSelectingTextColor) {
                    isSelectingTextColor = false
                } else {
                    onDismiss()
                }
            }) {
                Text(if (isSelectingTextColor) "返回" else "取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

