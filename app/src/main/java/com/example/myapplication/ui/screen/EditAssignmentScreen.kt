package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.myapplication.data.model.AssignmentType
import com.example.myapplication.data.model.Priority
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.ui.components.showDateTimePicker
import com.example.myapplication.ui.viewmodel.AssignmentViewModel
import com.example.myapplication.ui.viewmodel.AssignmentViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAssignmentScreen(navController: NavHostController, assignmentId: Int?) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = AssignmentRepository(database.assignmentDao())
    val courseRepository = CourseRepository(database.courseDao())
    val viewModel: AssignmentViewModel = viewModel(
        factory = AssignmentViewModelFactory(repository)
    )
    
    val assignment by viewModel.selectedAssignment.collectAsState()
    val userId = com.example.myapplication.session.CurrentSession.userIdInt ?: 0
    val courses by courseRepository.getCoursesByUser(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(assignmentId) {
        if (assignmentId != null) {
            scope.launch {
                val a = repository.getAssignmentById(assignmentId)
                if (a != null) {
                    viewModel.selectAssignment(a)
                }
            }
        }
    }
    
    if (assignment == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    var title by remember { mutableStateOf(assignment!!.title) }
    var description by remember { mutableStateOf(assignment!!.description ?: "") }
    var type by remember { mutableStateOf<AssignmentType>(assignment!!.type) }
    var dueDate by remember { mutableStateOf(Date(assignment!!.dueDate)) }
    var reminderEnabled by remember { mutableStateOf(assignment!!.reminderEnabled) }
    var priority by remember { mutableStateOf<Priority>(assignment!!.priority) }
    var selectedCourseId by remember { mutableStateOf(assignment!!.courseId) }
    var progress by remember { mutableFloatStateOf(assignment!!.progress.toFloat()) }
    var showCourseDropdown by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dueDateDisplay = remember(dueDate) { dateFormatter.format(dueDate) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "编辑任务",
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
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val progressValue = progress.toInt()
                            val adjustedStatus = when {
                                progressValue >= 100 -> com.example.myapplication.data.model.AssignmentStatus.COMPLETED
                                progressValue > 0 -> com.example.myapplication.data.model.AssignmentStatus.IN_PROGRESS
                                else -> com.example.myapplication.data.model.AssignmentStatus.NOT_STARTED
                            }
                            val updatedAssignment = assignment!!.copy(
                                title = title,
                                description = description.ifBlank { null },
                                type = type,
                                dueDate = dueDate.time,
                                reminderEnabled = reminderEnabled,
                                reminderTime = if (reminderEnabled) dueDate.time - 24 * 60 * 60 * 1000 else null,
                                priority = priority,
                                courseId = selectedCourseId,
                                progress = progressValue,
                                status = adjustedStatus
                            )
                            viewModel.updateAssignment(updatedAssignment)
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

