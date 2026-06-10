package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.navigation.Screen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Person
import com.example.myapplication.data.model.User
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.RemoteUserRepository
import com.example.myapplication.ui.components.PrimaryGradientButton
import com.example.myapplication.ui.viewmodel.AuthResult
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.example.myapplication.ui.viewmodel.UserViewModelFactory
import android.util.Patterns
import com.example.myapplication.session.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = UserRepository(database.userDao())
    val remoteRepository = remember { null as RemoteUserRepository? }
    val tokenManager = remember { TokenManager(context) }
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(repository, remoteRepository, tokenManager)
    )
    
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    // 切换账号相关状态
    var selectedUserForSwitch by remember { mutableStateOf<User?>(null) }
    var switchPassword by remember { mutableStateOf("") }
    var switchPasswordVisible by remember { mutableStateOf(false) }
    var switchError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    
    // 加载当前用户信息和所有用户列表
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
        viewModel.loadAllUsers()
    }
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var realName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    
    // 初始化表单数据
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            username = user.username
            email = user.email ?: ""
            realName = user.realName ?: ""
            studentId = user.studentId ?: ""
        }
    }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val trimmedEmail = email.trim()
    val emailPattern = Patterns.EMAIL_ADDRESS
    val isEmailValid = trimmedEmail.isBlank() || emailPattern.matcher(trimmedEmail).matches()
    
    val passwordIssues = remember(password) {
        val issues = mutableListOf<String>()
        if (password.isNotBlank()) {
            if (password.length < 8) issues += "至少8个字符"
            if (!password.any { it.isDigit() }) issues += "至少包含一个数字"
            if (!password.any { it.isLowerCase() }) issues += "至少包含一个小写字母"
            if (!password.any { it.isUpperCase() }) issues += "至少包含一个大写字母"
        }
        issues
    }
    val isPasswordStrong = password.isBlank() || passwordIssues.isEmpty()
    
    val isSaveEnabled =
        username.isNotBlank() &&
        (password.isBlank() || (password == confirmPassword && isPasswordStrong)) &&
        isEmailValid &&
        updateResult !is AuthResult.Success
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听更新结果
    LaunchedEffect(updateResult) {
        when (updateResult) {
            is AuthResult.Success -> {
                snackbarHostState.showSnackbar("用户信息更新成功")
                viewModel.clearUpdateResult()
                navController.popBackStack()
            }
            is AuthResult.Error -> {
                // 错误信息在UI中显示
            }
            null -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "编辑资料",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.clearUpdateResult()
                        navController.popBackStack() 
                    }) {
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
            if (currentUser == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 账号信息卡片
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
                                text = "账号信息",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("用户名") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = updateResult !is AuthResult.Success
                            )
                            
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("新密码 (留空则不修改)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = password.isNotBlank() && !isPasswordStrong,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                        )
                                    }
                                },
                                supportingText = {
                                    if (password.isNotBlank() && !isPasswordStrong) {
                                        Text(
                                            text = "密码要求：${passwordIssues.joinToString("、")}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                enabled = updateResult !is AuthResult.Success
                            )
                            
                            if (password.isNotBlank()) {
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("确认新密码") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码"
                                            )
                                        }
                                    },
                                    isError = confirmPassword.isNotBlank() && password != confirmPassword,
                                    supportingText = {
                                        if (confirmPassword.isNotBlank() && password != confirmPassword) {
                                            Text(
                                                text = "两次输入的密码不一致",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    enabled = updateResult !is AuthResult.Success
                                )
                            }
                        }
                    }
                    
                    // 个人信息卡片
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
                                text = "个人信息",
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
                                enabled = false // 学号通常不允许修改
                            )
                            
                            OutlinedTextField(
                                value = realName,
                                onValueChange = { realName = it },
                                label = { Text("真实姓名") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = updateResult !is AuthResult.Success
                            )
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("邮箱") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                isError = email.isNotBlank() && !isEmailValid,
                                supportingText = {
                                    if (email.isNotBlank() && !isEmailValid) {
                                        Text(
                                            text = "邮箱格式不正确",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                enabled = updateResult !is AuthResult.Success
                            )
                        }
                    }
                    
                    // 账号管理卡片
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
                                text = "账号管理",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // 切换账号按钮
                            OutlinedButton(
                                onClick = {
                                    showSwitchAccountDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("切换账号")
                            }
                            
                            // 删除账号按钮
                            OutlinedButton(
                                onClick = {
                                    showDeleteAccountDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除账号")
                            }
                        }
                    }
                    
                    // 错误提示
                    if (updateResult is AuthResult.Error) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = (updateResult as AuthResult.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PrimaryGradientButton(
                        text = "保存修改",
                        enabled = isSaveEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val updatedUser = currentUser!!.copy(
                            username = username,
                            password = if (password.isNotBlank()) password else currentUser!!.password,
                            realName = realName.ifBlank { null },
                            email = trimmedEmail.ifBlank { null }
                        )
                        viewModel.updateUser(updatedUser)
                    }
                }
            }
        }
    }
    
    // 切换账号确认对话框
    if (showSwitchAccountDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSwitchAccountDialog = false 
                selectedUserForSwitch = null
                switchPassword = ""
                switchError = null
            },
            title = {
                Text(
                    "切换账号",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedUserForSwitch == null) {
                        Text("选择要切换的账号：", style = MaterialTheme.typography.bodyMedium)
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allUsers) { user ->
                                val isCurrentUser = user.userId == currentUser?.userId
                                Card(
                                    onClick = { 
                                        if (!isCurrentUser) {
                                            selectedUserForSwitch = user 
                                            switchError = null
                                            switchPassword = ""
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = user.username,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!user.studentId.isNullOrBlank()) {
                                                Text(
                                                    text = "学号: ${user.studentId}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        if (isCurrentUser) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "当前",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 输入密码界面
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { 
                                        selectedUserForSwitch = null 
                                        switchError = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "验证密码: ${selectedUserForSwitch?.username}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            OutlinedTextField(
                                value = switchPassword,
                                onValueChange = { switchPassword = it },
                                label = { Text("请输入密码") },
                                visualTransformation = if (switchPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { switchPasswordVisible = !switchPasswordVisible }) {
                                        Icon(
                                            imageVector = if (switchPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (switchPasswordVisible) "隐藏密码" else "显示密码"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = switchError != null,
                                supportingText = {
                                    if (switchError != null) {
                                        Text(
                                            text = switchError!!,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val userToSwitch = selectedUserForSwitch
                if (userToSwitch != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                // 简单的本地密码验证
                                if (userToSwitch.password == switchPassword) {
                                    // 登录并跳转
                                    viewModel.login(userToSwitch.studentId ?: "", userToSwitch.username, switchPassword)
                                    showSwitchAccountDialog = false
                                    navController.navigate(Screen.CourseSchedule.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    switchError = "密码错误，请重试"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("切换")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showSwitchAccountDialog = false 
                        selectedUserForSwitch = null
                        switchPassword = ""
                        switchError = null
                    }
                ) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // 删除账号确认对话框
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    "删除账号",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "确定要删除当前账号吗？",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "此操作将永久删除您的账号及其所有数据，包括：\n• 个人信息\n• 课程和任务\n• 学习记录\n• 小组信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "此操作不可恢复！",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            currentUser?.let { user ->
                                try {
                                    viewModel.deleteAccount(user)
                                    showDeleteAccountDialog = false
                                    // 导航到登录页面并清除返回栈
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    // 处理删除失败的情况
                                    showDeleteAccountDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
