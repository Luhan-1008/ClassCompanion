package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.navigation.NavGraph
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.session.CurrentSession
import com.example.myapplication.session.TokenManager
import com.example.myapplication.work.ReminderScheduler
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

@Composable
fun MyApplicationApp() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    // 请求通知权限
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，可以启动提醒调度器
            val userId = tokenManager.getUserId()
            if (userId > 0) {
                ReminderScheduler.schedule(context.applicationContext)
            }
        }
    }
    
    // 检查并请求通知权限（Android 13+）
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // 权限已授予，如果已登录则启动提醒调度器
                val userId = tokenManager.getUserId()
                if (userId > 0) {
                    ReminderScheduler.schedule(context.applicationContext)
                }
            }
        } else {
            // Android 12 及以下不需要请求权限，直接启动
            val userId = tokenManager.getUserId()
            if (userId > 0) {
                ReminderScheduler.schedule(context.applicationContext)
            }
        }
    }
    
    // 检查登录状态并决定起始页面
    // 如果用户已登录（有userId），则直接进入课程表页面，保持登录状态
    // 否则进入登录页面
    val startDestination = remember {
        val userId = tokenManager.getUserId()
        if (userId > 0) {
            CurrentSession.userId = userId
            val token = tokenManager.getToken()
            if (!token.isNullOrBlank()) {
                CurrentSession.token = token
            }
            Screen.CourseSchedule.route
        } else {
            Screen.Login.route
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 根据当前路由判断是否显示底部导航栏
    val showBottomNav = currentRoute != Screen.Login.route && currentRoute != Screen.Register.route
    
    // 获取当前选中的屏幕（根据路由判断，包括子页面）
    val currentScreen = when {
        currentRoute == Screen.CourseSchedule.route || 
        currentRoute?.startsWith("add_course") == true ||
        currentRoute?.startsWith("edit_course") == true -> Screen.CourseSchedule
        currentRoute?.startsWith(Screen.Assignments.route) == true ||
        currentRoute?.startsWith("add_assignment") == true ||
        currentRoute?.startsWith("edit_assignment") == true -> Screen.Assignments
        currentRoute == Screen.StudyGroups.route ||
        currentRoute?.startsWith("create_group") == true ||
        currentRoute?.startsWith("group_detail") == true ||
        currentRoute?.startsWith("group_announcements") == true ||
        currentRoute?.startsWith("group_chat") == true ||
        currentRoute?.startsWith("group_invite") == true ||
        currentRoute?.startsWith("join_group") == true -> Screen.StudyGroups
        currentRoute == Screen.Profile.route ||
        currentRoute?.startsWith("ai_note") == true ||
        currentRoute?.startsWith("course_community") == true ||
        currentRoute?.startsWith("assignment_help") == true ||
        currentRoute?.startsWith("learning_analytics") == true ||
        currentRoute?.startsWith("smart_planner") == true -> Screen.Profile
        else -> null  // 不选中任何项，而不是默认选中课程表
    }

    if (showBottomNav) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
            item(
                icon = {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = Screen.CourseSchedule.label
                    )
                },
                label = { Text(Screen.CourseSchedule.label) },
                selected = currentScreen == Screen.CourseSchedule,
                onClick = {
                    navController.navigate(Screen.CourseSchedule.route) {
                        popUpTo(Screen.CourseSchedule.route) { inclusive = true }
                    }
                }
            )
            item(
                icon = {
                    Icon(
                        Icons.Default.List,
                        contentDescription = Screen.Assignments.label
                    )
                },
                label = { Text(Screen.Assignments.label) },
                selected = currentScreen == Screen.Assignments,
                onClick = {
                    navController.navigate(Screen.Assignments.route) {
                        popUpTo(Screen.CourseSchedule.route) { inclusive = false }
                    }
                }
            )
            item(
                icon = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = Screen.StudyGroups.label
                    )
                },
                label = { Text(Screen.StudyGroups.label) },
                selected = currentScreen == Screen.StudyGroups,
                onClick = {
                    navController.navigate(Screen.StudyGroups.route) {
                        popUpTo(Screen.CourseSchedule.route) { inclusive = false }
                    }
                }
            )
            item(
                icon = {
                    Icon(
                        Icons.Default.AccountBox,
                        contentDescription = Screen.Profile.label
                    )
                },
                label = { Text(Screen.Profile.label) },
                selected = currentScreen == Screen.Profile,
                onClick = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.CourseSchedule.route) { inclusive = false }
                    }
                }
            )
        }
    ) {
        NavGraph(navController = navController, startDestination = startDestination)
    }
    } else {
        NavGraph(navController = navController, startDestination = startDestination)
    }
}