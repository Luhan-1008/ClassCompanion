package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = CourseRepository(database.courseDao())
    val viewModel: CourseViewModel = viewModel(
        factory = CourseViewModelFactory(repository)
    )
    
    var courseName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:40") }
    var startWeek by remember { mutableStateOf(1) }
    var endWeek by remember { mutableStateOf(16) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderMinutes by remember { mutableStateOf(15) }
    var color by remember { mutableStateOf("#2196F3") }
    
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
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            label = { Text("课程代码") },
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = startWeek.toString(),
                                onValueChange = { startWeek = it.toIntOrNull() ?: 1 },
                                label = { Text("起始周") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = endWeek.toString(),
                                onValueChange = { endWeek = it.toIntOrNull() ?: 16 },
                                label = { Text("结束周") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
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
                        if (courseName.isNotBlank()) {
                            val currentUserId = com.example.myapplication.session.CurrentSession.userIdInt
                            if (currentUserId == null || currentUserId == 0) {
                                viewModel.insertCourse(
                                    Course(
                                        userId = 0, // 这会触发错误处理
                                        courseName = courseName,
                                        courseCode = courseCode.ifBlank { null },
                                        teacherName = teacherName.ifBlank { null },
                                        location = location.ifBlank { null },
                                        dayOfWeek = dayOfWeek,
                                        startTime = startTime,
                                        endTime = endTime,
                                        startWeek = startWeek,
                                        endWeek = endWeek,
                                        reminderEnabled = reminderEnabled,
                                        reminderMinutes = reminderMinutes,
                                        color = color
                                    )
                                )
                                return@Button
                            }
                            
                            val course = Course(
                                userId = currentUserId,
                                courseName = courseName,
                                courseCode = courseCode.ifBlank { null },
                                teacherName = teacherName.ifBlank { null },
                                location = location.ifBlank { null },
                                dayOfWeek = dayOfWeek,
                                startTime = startTime,
                                endTime = endTime,
                                startWeek = startWeek,
                                endWeek = endWeek,
                                reminderEnabled = reminderEnabled,
                                reminderMinutes = reminderMinutes,
                                color = color
                            )
                            viewModel.insertCourse(course)
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

