package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentHelpScreen(navController: NavHostController) {
    var question by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
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
                text = "输入你的问题，AI将提供解题思路提示",
                style = MaterialTheme.typography.bodyMedium
            )
            
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("问题描述") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            Button(
                onClick = {
                    if (question.isNotBlank()) {
                        isLoading = true
                        // TODO: 调用AI API
                        scope.launch {
                            kotlinx.coroutines.delay(2000)
                            aiResponse = "解题思路提示：\n\n1. 首先理解问题的核心要求\n2. 分析已知条件和未知量\n3. 选择合适的解题方法\n4. 逐步推导并验证结果\n\n相关知识点：\n- 知识点1\n- 知识点2\n- 知识点3"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && !isLoading
            ) {
                Text("获取提示")
            }
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            if (aiResponse.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "AI回答",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiResponse,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

