package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.CourseDifficultyTag
import com.example.myapplication.data.model.CourseResourceType
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.CourseResourceRepository
import com.example.myapplication.data.repository.CourseReviewRepository
import com.example.myapplication.ui.viewmodel.CourseCommunityUiState
import com.example.myapplication.ui.viewmodel.CourseCommunityViewModel
import com.example.myapplication.ui.viewmodel.CourseCommunityViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCommunityScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: CourseCommunityViewModel = viewModel(
        factory = CourseCommunityViewModelFactory(
            CourseRepository(database.courseDao()),
            CourseReviewRepository(database.courseReviewDao()),
            CourseResourceRepository(database.courseResourceDao())
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    var showReviewDialog by remember { mutableStateOf(false) }
    var showResourceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程评价与资源社区") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = rememberLazyListState()
        ) {
            uiState.errorMessage?.let { error ->
                item {
                    AssistChip(onClick = { viewModel.clearError() }, label = { Text(error) })
                }
            }
            item {
                CourseSelector(uiState, onSelect = { course ->
                    viewModel.selectCourse(course)
                })
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showReviewDialog = true },
                        enabled = uiState.selectedCourse != null
                    ) { Text("写评价") }
                    OutlinedButton(
                        onClick = { showResourceDialog = true },
                        enabled = uiState.selectedCourse != null
                    ) { Text("分享资料") }
                }
            }

            item {
                OverviewCard(uiState, ratingDisplay = viewModel.formatRatingDisplay())
            }

            if (uiState.reviews.isNotEmpty()) {
                item { Text("同学评价", style = MaterialTheme.typography.titleMedium) }
                items(uiState.reviews) { review ->
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("评分：${review.rating} / 5", fontWeight = FontWeight.Bold)
                            Text("难度：${review.difficulty.name} · 课业约 ${review.workloadHoursPerWeek}h/周")
                            if (review.highlightTags.isNotBlank()) {
                                Text("关键词：${review.highlightTags.replace("|", " / ")}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(review.comment)
                            review.suggestion?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("建议：$it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (uiState.resources.isNotEmpty()) {
                item { Text("资料共享", style = MaterialTheme.typography.titleMedium) }
                items(uiState.resources) { resource ->
                    ListItem(
                        headlineContent = { Text(resource.title) },
                        supportingContent = {
                            Text(resource.description ?: "无描述")
                        },
                        trailingContent = { Text(resource.resourceType.name) }
                    )
                }
            }
        }
    }

    if (showReviewDialog) {
        ReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, difficulty, workload, tags, comment, suggestion ->
                uiState.selectedCourse?.let {
                    viewModel.submitReview(
                        courseId = it.courseId,
                        rating = rating,
                        difficultyTag = difficulty,
                        workloadHours = workload,
                        highlightTags = tags,
                        comment = comment,
                        suggestion = suggestion
                    )
                }
                showReviewDialog = false
            }
        )
    }

    if (showResourceDialog) {
        ResourceDialog(
            onDismiss = { showResourceDialog = false },
            onSubmit = { title, url, type, description ->
                uiState.selectedCourse?.let {
                    viewModel.submitResource(
                        courseId = it.courseId,
                        title = title,
                        url = url,
                        resourceType = type,
                        description = description
                    )
                }
                showResourceDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseSelector(
    uiState: CourseCommunityUiState,
    onSelect: (Course) -> Unit
) {
    if (uiState.courses.isEmpty()) {
        Text("请先在课程表中添加课程。")
        return
    }
    SingleChoiceSegmentedButtonRow {
        uiState.courses.forEachIndexed { index, course ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, uiState.courses.size),
                selected = uiState.selectedCourse == course,
                onClick = { onSelect(course) },
                label = { Text(course.courseName.take(6)) }
            )
        }
    }
}

@Composable
private fun OverviewCard(uiState: CourseCommunityUiState, ratingDisplay: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(uiState.selectedCourse?.courseName ?: "未选择课程", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("综合评分：$ratingDisplay")
            Text("口碑标签：${uiState.difficultyLabel}")
            Text("已有 ${uiState.reviews.size} 条评价 · ${uiState.resources.size} 条资料")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, difficulty: CourseDifficultyTag, workload: Int, tags: List<String>, comment: String, suggestion: String?) -> Unit
) {
    var rating by remember { mutableStateOf(4f) }
    var difficulty by remember { mutableStateOf(CourseDifficultyTag.MEDIUM) }
    var workload by remember { mutableStateOf("4") }
    var tags by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var suggestion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSubmit(
                    rating.toInt().coerceIn(1, 5),
                    difficulty,
                    workload.toIntOrNull() ?: 4,
                    tags.split("，", ",").map { it.trim() },
                    comment,
                    suggestion.ifBlank { null }
                )
            }) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("撰写课程评价") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("评分：${rating.toInt()} 星")
                Slider(value = rating, onValueChange = { rating = it }, steps = 3, valueRange = 1f..5f)
                SingleChoiceSegmentedButtonRow {
                    CourseDifficultyTag.values().forEachIndexed { index, tag ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, CourseDifficultyTag.values().size),
                            selected = difficulty == tag,
                            onClick = { difficulty = tag },
                            label = { Text(tag.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = workload,
                    onValueChange = { workload = it.filter { ch -> ch.isDigit() } },
                    label = { Text("每周投入小时数") }
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("标签（逗号分隔）") }
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("详细评价") }
                )
                OutlinedTextField(
                    value = suggestion,
                    onValueChange = { suggestion = it },
                    label = { Text("学习建议（可选）") }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, url: String, type: CourseResourceType, description: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(CourseResourceType.NOTE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSubmit(title, url, type, description.ifBlank { null })
            }) { Text("分享") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("分享课程资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("链接") })
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("资料类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        CourseResourceType.values().forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.name) },
                                onClick = {
                                    type = candidate
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("简单介绍") }
                )
            }
        }
    )
}

