package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Course
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun WeekViewScreen(
    courses: List<Course>,
    modifier: Modifier = Modifier,
    onCourseClick: (Course) -> Unit
) {
    var currentWeekStart by remember { mutableStateOf(getCurrentWeekStart()) }
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val periodSlots = generatePeriodSlots()
    val weekNumber = calculateAcademicWeek(currentWeekStart)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "周视图",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatWeekRange(currentWeekStart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { currentWeekStart = getPreviousWeek(currentWeekStart) }) {
                    Text("◀", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = { currentWeekStart = getCurrentWeekStart() }) {
                    Text("第${weekNumber}周")
                }
                IconButton(onClick = { currentWeekStart = getNextWeek(currentWeekStart) }) {
                    Text("▶", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // 表头
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "节次",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    weekDays.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(periodSlots) { slot ->
                        WeekViewRow(
                            slot = slot,
                            weekDays = weekDays,
                            courses = courses,
                            onCourseClick = onCourseClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekViewRow(
    slot: PeriodSlot,
    weekDays: List<String>,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit
) {
    val slotHeight = 56.dp * (slot.durationMinutes / 45f).coerceAtLeast(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(slotHeight)
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 时间列
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${slot.start} - ${slot.end}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 每天的课程格子
        weekDays.forEachIndexed { dayIndex, _ ->
            val dayOfWeek = dayIndex + 1
            val coursesInSlot = courses.filter { course ->
                course.dayOfWeek == dayOfWeek &&
                isCourseInSlot(course.startTime, course.endTime, slot)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = coursesInSlot.isNotEmpty()) {
                        coursesInSlot.firstOrNull()?.let { onCourseClick(it) }
                    }
            ) {
                coursesInSlot.forEach { course ->
                    CourseWeekCell(
                        course = course,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CourseWeekCell(
    course: Course,
    modifier: Modifier = Modifier
) {
    val courseColor = Color(android.graphics.Color.parseColor(course.color))
    
    Card(
        modifier = modifier
            .padding(2.dp)
            .shadow(2.dp, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = courseColor.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!course.location.isNullOrEmpty()) {
                Text(
                    text = course.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CourseDetailDialog(
    course: Course,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val courseColor = Color(android.graphics.Color.parseColor(course.color))
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 课程名称
                InfoRow("课程名称", course.courseName)
                
                // 课程编号（可选）
                if (!course.courseCode.isNullOrEmpty()) {
                    InfoRow("课程编号", course.courseCode)
                }
                
                // 教师
                if (!course.teacherName.isNullOrEmpty()) {
                    InfoRow("任课教师", course.teacherName)
                }
                
                // 上课时间
                InfoRow("上课时间", "${getDayName(course.dayOfWeek)} ${course.startTime} - ${course.endTime}")
                
                // 教学周
                InfoRow("教学周", "第${course.startWeek}-${course.endWeek}周")
                
                // 地点
                if (!course.location.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoRow("上课地点", course.location)
                        TextButton(
                            onClick = {
                                openMapNavigation(context, course.location)
                                onDismiss()
                            }
                        ) {
                            Text("导航")
                        }
                    }
                }
                
                // 提醒设置
                if (course.reminderEnabled) {
                    InfoRow("提醒", "提前${course.reminderMinutes}分钟")
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
                Button(
                    onClick = {
                        onEdit()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = courseColor
                    )
                ) {
                    Text("编辑")
                }
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 工具函数
fun getCurrentWeekStart(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    return calendar
}

fun getPreviousWeek(weekStart: Calendar): Calendar {
    val newWeek = weekStart.clone() as Calendar
    newWeek.add(Calendar.WEEK_OF_YEAR, -1)
    return newWeek
}

fun getNextWeek(weekStart: Calendar): Calendar {
    val newWeek = weekStart.clone() as Calendar
    newWeek.add(Calendar.WEEK_OF_YEAR, 1)
    return newWeek
}

fun formatWeekRange(weekStart: Calendar): String {
    val end = weekStart.clone() as Calendar
    end.add(Calendar.DAY_OF_WEEK, 6)
    val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
    return "${sdf.format(weekStart.time)} - ${sdf.format(end.time)}"
}

fun generatePeriodSlots(): List<PeriodSlot> = listOf(
    PeriodSlot("1", "08:00", "09:35"),
    PeriodSlot("2", "09:50", "11:25"),
    PeriodSlot("3", "11:35", "12:15"),
    PeriodSlot("4", "14:00", "15:35"),
    PeriodSlot("5", "15:50", "17:25"),
    PeriodSlot("6", "17:35", "18:15"),
    PeriodSlot("7", "19:00", "20:35"),
    PeriodSlot("8", "20:45", "22:15")
)

fun isCourseInSlot(courseStart: String, courseEnd: String, slot: PeriodSlot): Boolean {
    val slotStart = parseTimeToMinutes(slot.start)
    val slotEnd = parseTimeToMinutes(slot.end)
    val courseStartMinutes = parseTimeToMinutes(courseStart)
    val courseEndMinutes = parseTimeToMinutes(courseEnd)
    return courseStartMinutes < slotEnd && courseEndMinutes > slotStart
}

fun parseTimeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

data class PeriodSlot(
    val label: String,
    val start: String,
    val end: String
) {
    val durationMinutes: Int = parseTimeToMinutes(end) - parseTimeToMinutes(start)
}

fun getDayName(dayOfWeek: Int): String {
    val days = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return days.getOrElse(dayOfWeek) { "" }
}

fun calculateAcademicWeek(weekStart: Calendar): Int {
    val referenceYear = if (weekStart.get(Calendar.MONTH) >= Calendar.SEPTEMBER) {
        weekStart.get(Calendar.YEAR)
    } else {
        weekStart.get(Calendar.YEAR) - 1
    }
    val academicStart = Calendar.getInstance().apply {
        set(Calendar.YEAR, referenceYear)
        set(Calendar.MONTH, Calendar.SEPTEMBER)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    while (academicStart.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        academicStart.add(Calendar.DAY_OF_MONTH, 1)
    }
    val diffMillis = weekStart.timeInMillis - academicStart.timeInMillis
    val weekMillis = 7L * 24 * 60 * 60 * 1000
    return max(1, (diffMillis / weekMillis).toInt() + 1)
}

