package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.AssignmentRepository
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.GroupMessageRepository
import com.example.myapplication.domain.ai.ChapterLink
import com.example.myapplication.ui.viewmodel.AiAssistViewModel
import com.example.myapplication.ui.viewmodel.AiAssistViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssignmentHelpScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: AiAssistViewModel = viewModel(
        factory = AiAssistViewModelFactory(
            CourseRepository(database.courseDao()),
            AssignmentRepository(database.assignmentDao()),
            GroupMessageRepository(database.groupMessageDao())
        )
    )
    var question by remember { mutableStateOf("") }
    val uiState by viewModel.assignmentUiState.collectAsState()
    val presetQuestions = listOf(
        "如何理解傅里叶变换的物理意义？",
        "AVL 树插入的时间复杂度如何证明？",
        "列车最短路径问题怎么建模？"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("作业辅导提示") },
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
                text = "聚焦思路而非直接答案，自动聚合概念、公式、解题步骤与小组讨论。",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("问题描述") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                maxLines = 6
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presetQuestions.forEach { preset ->
                    SuggestionChip(onClick = { question = preset }, label = { Text(preset) })
                }
            }

            Button(
                onClick = { viewModel.requestAssignmentHelp(question) },
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && !uiState.isProcessing
            ) {
                Text("生成思路提示")
            }

            if (uiState.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.errorMessage?.let {
                AssistChip(onClick = { viewModel.clearAssignmentError() }, label = { Text(it) })
            }

            uiState.hint?.let { hint ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        HintCard(
                            title = "概念梳理",
                            items = hint.relatedConcepts
                        )
                    }
                    item {
                        HintCard(
                            title = "常用公式 / 模型",
                            items = hint.formulas
                        )
                    }
                    item {
                        HintCard(
                            title = "解题步骤建议",
                            items = hint.solutionSteps,
                            leadingIcon = Icons.Default.TipsAndUpdates
                        )
                    }
                    item {
                        ChapterLinkSection(chapters = hint.chapterRecommendations)
                    }
                    item {
                        if (hint.relatedDiscussions.isNotEmpty()) {
                            Card {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("小组讨论串", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    hint.relatedDiscussions.forEach { message ->
                                        Text(text = "· $message", style = MaterialTheme.typography.bodySmall)
                                    }
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
private fun HintCard(
    title: String,
    items: List<String>,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    if (items.isEmpty()) return
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                leadingIcon?.let {
                    Icon(it, contentDescription = null)
                }
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, text ->
                Text(text = "${index + 1}. $text", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ChapterLinkSection(chapters: List<ChapterLink>) {
    if (chapters.isEmpty()) return
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("推荐复习章节", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            chapters.forEach {
                Text("· ${it.courseName} - ${it.chapterLabel}", style = MaterialTheme.typography.bodyMedium)
                Text(it.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
