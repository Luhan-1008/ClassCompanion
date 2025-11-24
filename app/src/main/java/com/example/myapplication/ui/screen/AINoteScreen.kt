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
fun AINoteScreen(navController: NavHostController) {
    var noteText by remember { mutableStateOf("") }
    var aiSummary by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI笔记助手") },
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
                text = "上传课堂录音或笔记图片，AI将自动生成知识提纲",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = {
                    // TODO: 实现文件选择和上传功能
                    isLoading = true
                    // 模拟AI处理
                    scope.launch {
                        kotlinx.coroutines.delay(2000)
                        aiSummary = "这是AI生成的笔记摘要示例。\n\n1. 主要知识点\n2. 重点内容\n3. 复习建议"
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择文件（录音/图片）")
            }
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            if (aiSummary.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "AI生成的摘要",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

