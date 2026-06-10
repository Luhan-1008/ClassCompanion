package com.example.myapplication.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.RemoteUserRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.session.TokenManager
import com.example.myapplication.ui.components.PrimaryGradientButton
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.viewmodel.AuthResult
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.example.myapplication.ui.viewmodel.UserViewModelFactory
import com.example.myapplication.work.ReminderScheduler
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = UserRepository(database.userDao())
    val remoteRepository = remember { RemoteUserRepository() }
    val tokenManager = remember { TokenManager(context) }
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(repository, remoteRepository, tokenManager)
    )

    var studentId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loginResult by viewModel.loginResult.collectAsState()

    var paperFolded by remember { mutableStateOf(false) }
    var paperFlewAway by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        paperFolded = true
        delay(1000)
        paperFlewAway = true
        delay(1200)
        showContent = true
    }

    LaunchedEffect(loginResult) {
        when (loginResult) {
            is AuthResult.Success -> {
                ReminderScheduler.schedule(context.applicationContext)
                navController.navigate(Screen.CourseSchedule.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is AuthResult.Error -> {
            }
            null -> {}
        }
    }

    val isLoginEnabled =
        username.isNotBlank() &&
            password.isNotBlank() &&
            loginResult !is AuthResult.Success

    val density = LocalDensity.current

    val paperScale by animateFloatAsState(
        targetValue = if (paperFolded) 0.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "paperScale"
    )

    val paperRotation by animateFloatAsState(
        targetValue = when {
            paperFlewAway -> -30f
            paperFolded -> -15f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "paperRotation"
    )

    val paperRotationX by animateFloatAsState(
        targetValue = if (paperFolded) 30f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "paperRotationX"
    )

    val paperOffsetX by animateFloatAsState(
        targetValue = if (paperFlewAway) 600f else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = LinearOutSlowInEasing
        ),
        label = "paperOffsetX"
    )

    val paperOffsetY by animateFloatAsState(
        targetValue = if (paperFlewAway) -500f else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = LinearOutSlowInEasing
        ),
        label = "paperOffsetY"
    )

    val paperAlpha by animateFloatAsState(
        targetValue = if (paperFlewAway) 0f else 1f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "paperAlpha"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "contentAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .size(300.dp, 220.dp)
                    .align(Alignment.Center)
                    .offset(
                        x = paperOffsetX.dp,
                        y = paperOffsetY.dp
                    )
                    .graphicsLayer {
                        scaleX = paperScale
                        scaleY = paperScale
                        rotationZ = paperRotation
                        rotationX = paperRotationX
                        alpha = paperAlpha
                        cameraDistance = 8f * density.density
                    }
                    .shadow(
                        elevation = if (paperFolded) 8.dp else 12.dp,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!paperFolded) {
                        Text(
                            text = "ClassCompanion",
                            color = Color.Gray,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp, 80.dp)
                                .graphicsLayer {
                                    rotationZ = -10f
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }

        if (showContent || contentAlpha > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha }
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "欢迎回来",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "使用后端账号登录，支持跨设备重新登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = { studentId = it },
                            label = { Text("学号（本地缓存可选）") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = "学号"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "用户名"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "密码"
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            }
                        )

                        val errorMessage = (loginResult as? AuthResult.Error)?.message
                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        PrimaryGradientButton(
                            text = "登录",
                            onClick = {
                                viewModel.clearLoginResult()
                                viewModel.login(studentId.trim(), username.trim(), password)
                            },
                            enabled = isLoginEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextButton(
                            onClick = {
                                viewModel.clearLoginResult()
                                navController.navigate(Screen.Register.route)
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("还没有账号？去注册")
                        }
                    }
                }
            }
        }
    }
}
