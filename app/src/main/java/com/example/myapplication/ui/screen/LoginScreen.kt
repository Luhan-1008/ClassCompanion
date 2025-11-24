package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.RemoteUserRepository
import com.example.myapplication.ui.components.PrimaryGradientButton
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.AuthResult
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.example.myapplication.ui.viewmodel.UserViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = UserRepository(database.userDao())
    // 不使用后端，直接使用本地登录
    val remoteRepository = remember { null as RemoteUserRepository? }
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(repository, remoteRepository)
    )
    
    var studentId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val loginResult by viewModel.loginResult.collectAsState()
    
    // 处理登录结果
    LaunchedEffect(loginResult) {
        when (loginResult) {
            is AuthResult.Success -> {
                navController.navigate(Screen.CourseSchedule.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is AuthResult.Error -> {
                // 错误已在UI中显示
            }
            null -> {}
        }
    }
    
    val isLoginEnabled = studentId.isNotBlank() && username.isNotBlank() && password.isNotBlank() && loginResult !is AuthResult.Success
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "登录",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Logo/标题区域
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "北航课程伴侣",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "欢迎回来",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 登录表单卡片
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
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "登录账户",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            OutlinedTextField(
                                value = studentId,
                                onValueChange = { studentId = it },
                                label = { Text("学号") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = loginResult !is AuthResult.Success
                            )
                            
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("用户名") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = loginResult !is AuthResult.Success
                            )
                            
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("密码") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                        )
                                    }
                                },
                                enabled = loginResult !is AuthResult.Success
                            )
                            
                            // 错误提示
                            if (loginResult is AuthResult.Error) {
                                Text(
                                    text = (loginResult as AuthResult.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            PrimaryGradientButton(
                                text = "登录",
                                enabled = isLoginEnabled,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewModel.login(studentId, username, password)
                            }
                        }
                    }
                    
                    // 注册链接
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "还没有账户？",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                viewModel.clearLoginResult()
                                navController.navigate(Screen.Register.route)
                            }
                        ) {
                            Text(
                                text = "立即注册",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

