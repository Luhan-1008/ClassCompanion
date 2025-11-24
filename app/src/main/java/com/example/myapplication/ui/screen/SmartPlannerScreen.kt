package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.domain.ai.PlannedSession
import com.example.myapplication.domain.ai.SessionKind
import com.example.myapplication.ui.viewmodel.AiAssistViewModel
import com.example.myapplication.ui.viewmodel.AiAssistViewModelFactory
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: AiAssistViewModel = viewModel(
        factory = AiAssistViewModelFactory(
            CourseRepository(database.courseDao()),
            AssignmentRepository(database.assignmentDao()),
            GroupMessageRepository(database.groupMessageDao())
        )
    )
    val plannerState by viewModel.plannerUiState.collectAsState()
    var dayCount by remember { mutableStateOf(5f) }

    LaunchedEffect(Unit) {
        viewModel.generateSmartPlanner(dayCount.toInt())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能日程规划") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "结合课程表与临近截止任务，自动生成未来数日的推荐学习时间安排，可直接参考并微调。",
                style = MaterialTheme.typography.bodyMedium
            )

            Column {
                Text("规划天数：${dayCount.toInt()} 天")
                Slider(
                    value = dayCount,
                    onValueChange = {
                        dayCount = it
                        viewModel.generateSmartPlanner(dayCount.toInt())
                    },
                    valueRange = 3f..7f,
                    steps = 3
                )
            }

            if (plannerState.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            plannerState.errorMessage?.let {
                AssistChip(onClick = { viewModel.clearPlannerError() }, label = { Text(it) })
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(plannerState.plan) { day ->
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.date.format(DateTimeFormatter.ofPattern("MM-dd E")),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text("专注指数 ${day.focusScore}") }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            day.sessions.forEach { session ->
                                SessionRow(session)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            if (day.priorityAssignments.isNotEmpty()) {
                                Text("当天优先任务", style = MaterialTheme.typography.titleSmall)
                                day.priorityAssignments.forEach {
                                    Text("· $it", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: PlannedSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.label, style = MaterialTheme.typography.titleSmall)
            Text(session.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SuggestionChip(
            onClick = {},
            label = { Text(session.timeRange) },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = session.type.color()
            )
        )
    }
}

@Composable
private fun SessionKind.color(): androidx.compose.ui.graphics.Color {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        SessionKind.CLASS -> scheme.primaryContainer
        SessionKind.REVIEW -> scheme.secondaryContainer
        SessionKind.ASSIGNMENT -> scheme.tertiaryContainer
        SessionKind.DISCUSSION -> scheme.surfaceVariant
        SessionKind.BUFFER -> scheme.surface
    }
}

