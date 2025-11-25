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
import androidx.compose.ui.unit.sp
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
    
    // 计算本周日期
    val weekDates = remember(currentWeekStart) {
        val calendar = currentWeekStart.clone() as Calendar
        (0..6).map {
            val date = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            SimpleDateFormat("MM.dd", Locale.getDefault()).format(date)
        }
    }
    
    // 计算今天是周几（如果是本周）
    val todayIndex = remember(currentWeekStart) {
        val today = Calendar.getInstance()
        // 重置时间部分以进行准确的日期比较
        val current = currentWeekStart.clone() as Calendar
        current.set(Calendar.HOUR_OF_DAY, 0); current.set(Calendar.MINUTE, 0); current.set(Calendar.SECOND, 0); current.set(Calendar.MILLISECOND, 0)
        
        val todayZero = Calendar.getInstance()
        todayZero.set(Calendar.HOUR_OF_DAY, 0); todayZero.set(Calendar.MINUTE, 0); todayZero.set(Calendar.SECOND, 0); todayZero.set(Calendar.MILLISECOND, 0)
        
        val diffMillis = todayZero.timeInMillis - current.timeInMillis
        val diffDays = (diffMillis / (24 * 60 * 60 * 1000)).toInt()
        
        if (diffDays in 0..6) diffDays else -1
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text(
                                    text = "◀",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.clickable { currentWeekStart = getPreviousWeek(currentWeekStart) }
                                )
                                Text(
                                    text = "▶",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.clickable { currentWeekStart = getNextWeek(currentWeekStart) }
                                )
                            }
                            Text(
                                text = "第${weekNumber}周",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { currentWeekStart = getCurrentWeekStart() }
                            )
                        }
                    }
                    weekDays.forEachIndexed { index, day ->
                        val isToday = index == todayIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Bold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = weekDates.getOrElse(index) { "" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(periodSlots) { slot ->
                        Column {
                            WeekViewRow(
                                slot = slot,
                                weekDays = weekDays,
                                courses = courses,
                                todayIndex = todayIndex,
                                onCourseClick = onCourseClick
                            )
                            // 5-6节（午休）和10-11节（晚饭）之间增加空隙
                            if (slot.label == "5" || slot.label == "10") {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
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
    todayIndex: Int,
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
                    text = "${slot.start}\n${slot.end}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 每天的课程格子
        weekDays.forEachIndexed { dayIndex, _ ->
            val dayOfWeek = dayIndex + 1
            val isToday = dayIndex == todayIndex
            val coursesInSlot = courses.filter { course ->
                course.dayOfWeek == dayOfWeek &&
                isCourseInSlot(course.startTime, course.endTime, slot)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    .clickable(enabled = coursesInSlot.isNotEmpty()) {
                        coursesInSlot.firstOrNull()?.let { onCourseClick(it) }
                    }
            ) {
                coursesInSlot.forEach { course ->
                    CourseWeekCell(
                        course = course,
                        isToday = isToday,
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
    isToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val courseColor = Color(android.graphics.Color.parseColor(course.color))
    
    Card(
        modifier = modifier
            .padding(2.dp)
            .shadow(if (isToday) 4.dp else 2.dp, shape = RoundedCornerShape(8.dp))
            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier),
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 课程名称
                InfoRow("课程名称", course.courseName)
                
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "上课地点",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = course.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
    PeriodSlot("1", "08:00", "08:45"),
    PeriodSlot("2", "08:50", "09:35"),
    PeriodSlot("3", "09:50", "10:35"),
    PeriodSlot("4", "10:40", "11:25"),
    PeriodSlot("5", "11:30", "12:15"),
    PeriodSlot("6", "14:00", "14:45"),
    PeriodSlot("7", "14:50", "15:35"),
    PeriodSlot("8", "15:50", "16:35"),
    PeriodSlot("9", "16:40", "17:25"),
    PeriodSlot("10", "17:30", "18:15"),
    PeriodSlot("11", "19:00", "19:45"),
    PeriodSlot("12", "19:50", "20:35"),
    PeriodSlot("13", "20:40", "21:25"),
    PeriodSlot("14", "21:30", "22:45")
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

