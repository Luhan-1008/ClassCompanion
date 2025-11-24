package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.ui.screen.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.CourseSchedule.route) {
            CourseScheduleScreen(navController = navController)
        }
        composable(
            route = Screen.Assignments.route + "?courseId={courseId}",
            arguments = listOf(
                navArgument("courseId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId")?.takeIf { it != -1 }
            AssignmentsScreen(navController = navController, initialCourseId = courseId)
        }
        composable(Screen.StudyGroups.route) {
            StudyGroupsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.AddCourse.route) {
            AddCourseScreen(navController = navController)
        }
        composable(Screen.EditCourse.route + "/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            EditCourseScreen(navController = navController, courseId = courseId)
        }
        composable(Screen.AddAssignment.route) {
            AddAssignmentScreen(navController = navController)
        }
        composable(Screen.EditAssignment.route + "/{assignmentId}") { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId")?.toIntOrNull()
            EditAssignmentScreen(navController = navController, assignmentId = assignmentId)
        }
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(navController = navController)
        }
        composable(Screen.GroupDetail.route + "/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
            GroupDetailScreen(navController = navController, groupId = groupId)
        }
        composable("group_discovery") {
            GroupDiscoveryScreen(navController = navController)
        }
        composable("group_chat/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
            if (groupId != null) {
                GroupChatScreen(navController = navController, groupId = groupId)
            }
        }
        composable("group_files/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
            if (groupId != null) {
                GroupFileLibraryScreen(navController = navController, groupId = groupId)
            }
        }
        composable("group_announcements/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
            if (groupId != null) {
                GroupAnnouncementScreen(navController = navController, groupId = groupId)
            }
        }
        composable("group_tasks/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
            if (groupId != null) {
                GroupTaskScreen(navController = navController, groupId = groupId)
            }
        }
        composable("group_invite/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
            if (groupId != null) {
                GroupInviteScreen(navController = navController, groupId = groupId)
            }
        }
        composable("join_group_by_invite") {
            JoinGroupByInviteScreen(navController = navController)
        }
        composable("ai_note") {
            com.example.myapplication.ui.screen.AINoteScreen(navController = navController)
        }
        composable("assignment_help") {
            com.example.myapplication.ui.screen.AssignmentHelpScreen(navController = navController)
        }
        composable("learning_analytics") {
            com.example.myapplication.ui.screen.LearningAnalyticsScreen(navController = navController)
        }
    }
}

sealed class Screen(val route: String, val label: String) {
    object Login : Screen("login", "登录")
    object Register : Screen("register", "注册")
    object CourseSchedule : Screen("course_schedule", "课程表")
    object Assignments : Screen("assignments", "任务")
    object StudyGroups : Screen("study_groups", "学习小组")
    object Profile : Screen("profile", "我的")
    object AddCourse : Screen("add_course", "添加课程")
    object EditCourse : Screen("edit_course", "编辑课程")
    object AddAssignment : Screen("add_assignment", "添加任务")
    object EditAssignment : Screen("edit_assignment", "编辑任务")
    object CreateGroup : Screen("create_group", "创建小组")
    object GroupDetail : Screen("group_detail", "小组详情")
}

