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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 根据当前路由判断是否显示底部导航栏
    val showBottomNav = currentRoute != Screen.Login.route && currentRoute != Screen.Register.route
    
    // 获取当前选中的屏幕
    val currentScreen = when {
        currentRoute == Screen.CourseSchedule.route -> Screen.CourseSchedule
        currentRoute?.startsWith(Screen.Assignments.route) == true -> Screen.Assignments
        currentRoute == Screen.StudyGroups.route -> Screen.StudyGroups
        currentRoute == Screen.Profile.route -> Screen.Profile
        else -> Screen.CourseSchedule
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
        NavGraph(navController = navController)
    }
    } else {
        NavGraph(navController = navController)
    }
}