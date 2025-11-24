package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.AssignmentType
import com.example.myapplication.data.model.Priority
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.ui.components.showDateTimePicker
import com.example.myapplication.ui.viewmodel.AssignmentViewModel
import com.example.myapplication.ui.viewmodel.AssignmentViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssignmentScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = AssignmentRepository(database.assignmentDao())
    val courseRepository = CourseRepository(database.courseDao())
    val viewModel: AssignmentViewModel = viewModel(
        factory = AssignmentViewModelFactory(repository)
    )
    
    val userId = com.example.myapplication.session.CurrentSession.userIdInt ?: 0
    val courses by courseRepository.getCoursesByUser(userId).collectAsState(initial = emptyList())
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<AssignmentType>(AssignmentType.HOMEWORK) }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var dueDate by remember { mutableStateOf(Date()) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var firstReminderDays by remember { mutableStateOf(3) } // 首次提醒：提前3天
    var urgentReminderHours by remember { mutableStateOf(6) } // 紧急提醒：提前6小时
    var priority by remember { mutableStateOf<Priority>(Priority.MEDIUM) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    var showCourseDropdown by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dueDateDisplay = remember(dueDate) { dateFormatter.format(dueDate) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "添加任务",
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
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("任务标题 *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("任务描述") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        // 关联课程选择
                        Text(
                            text = "关联课程（可选）",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        val selectedCourse = courses.find { it.courseId == selectedCourseId }
                        ExposedDropdownMenuBox(
                            expanded = showCourseDropdown,
                            onExpandedChange = { showCourseDropdown = !showCourseDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedCourse?.courseName ?: "未选择课程",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("选择课程") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCourseDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showCourseDropdown,
                                onDismissRequest = { showCourseDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("不关联课程") },
                                    onClick = {
                                        selectedCourseId = null
                                        showCourseDropdown = false
                                    }
                                )
                                courses.forEach { course ->
                                    DropdownMenuItem(
                                        text = { Text(course.courseName) },
                                        onClick = {
                                            selectedCourseId = course.courseId
                                            showCourseDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "截止时间",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dueDateDisplay,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "请选择准确的截止日期和时间",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        showDateTimePicker(context, dueDate) { selected ->
                                            dueDate = selected
                                        }
                                    }
                                ) {
                                    Text("调整")
                                }
                            }
                        }

                        Text(
                            text = "任务进度",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { progress = it },
                                valueRange = 0f..100f,
                                steps = 9,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${progress.toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // 类型和优先级卡片
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
                            text = "分类设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "任务类型",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AssignmentType.values().forEach { assignmentType ->
                                val isSelected = type == assignmentType
                                val displayName = when (assignmentType) {
                                    AssignmentType.HOMEWORK -> "作业"
                                    AssignmentType.EXPERIMENT -> "实验"
                                    AssignmentType.OTHER -> "其他"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { type = assignmentType },
                                    label = { 
                                        Text(
                                            displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "优先级",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Priority.values().forEach { priorityOption ->
                                val isSelected = priority == priorityOption
                                val displayName = when (priorityOption) {
                                    Priority.LOW -> "低"
                                    Priority.MEDIUM -> "中"
                                    Priority.HIGH -> "高"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { priority = priorityOption },
                                    label = { 
                                        Text(
                                            displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        selectedBorderColor = MaterialTheme.colorScheme.secondary,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        selectedBorderWidth = 2.dp,
                                        borderWidth = 1.dp
                                    )
                                )
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "首次提醒（提前天数）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = firstReminderDays.toFloat(),
                                    onValueChange = { firstReminderDays = it.toInt() },
                                    valueRange = 1f..7f,
                                    steps = 6,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${firstReminderDays}天",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "紧急提醒（提前小时）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = urgentReminderHours.toFloat(),
                                    onValueChange = { urgentReminderHours = it.toInt() },
                                    valueRange = 1f..24f,
                                    steps = 23,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${urgentReminderHours}小时",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val progressValue = progress.toInt()
                            val initialStatus = when {
                                progressValue >= 100 -> com.example.myapplication.data.model.AssignmentStatus.COMPLETED
                                progressValue > 0 -> com.example.myapplication.data.model.AssignmentStatus.IN_PROGRESS
                                else -> com.example.myapplication.data.model.AssignmentStatus.NOT_STARTED
                            }
                            val assignment = Assignment(
                                userId = userId,
                                courseId = selectedCourseId,
                                title = title,
                                description = description.ifBlank { null },
                                type = type,
                                dueDate = dueDate.time,
                                reminderEnabled = reminderEnabled,
                                reminderTime = if (reminderEnabled) {
                                    // 首次提醒时间：提前N天
                                    dueDate.time - firstReminderDays * 24 * 60 * 60 * 1000L
                                } else null,
                                status = initialStatus,
                                priority = priority,
                                progress = progressValue
                            )
                            viewModel.insertAssignment(assignment)
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
                        text = "保存任务",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

