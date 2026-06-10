package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.myapplication.utils.ColorSchemeUtils
import com.example.myapplication.utils.ScheduleDateUtils
import com.example.myapplication.utils.WeekDisplayUtils
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign

@Composable
fun WeekViewScreen(
    courses: List<Course>,
    settings: com.example.myapplication.data.model.ScheduleSettings,
    modifier: Modifier = Modifier,
    onCourseClick: (Course) -> Unit
) {
    // 使用设置中的开学日期初始化当前周
    val initialWeek = remember(settings) {
        ScheduleDateUtils.getCurrentWeek(settings)
    }
    var currentWeekStart by remember(settings, initialWeek) { 
        mutableStateOf(ScheduleDateUtils.getWeekStartDate(initialWeek, settings))
    }
    
    // 更新getCurrentWeekStart函数以使用设置
    fun getCurrentWeekStart(): Calendar {
        val currentWeek = ScheduleDateUtils.getCurrentWeek(settings)
        return ScheduleDateUtils.getWeekStartDate(currentWeek, settings)
    }
    var isWeekView by remember { mutableStateOf(true) }

    // 计算今天是周几（如果是本周）
    val todayIndex = remember(currentWeekStart) {
        val today = Calendar.getInstance()
        // 重置时间部分以进行准确的日期比较
        val current = currentWeekStart.clone() as Calendar
        current.set(Calendar.HOUR_OF_DAY, 0); current.set(
        Calendar.MINUTE,
        0
    ); current.set(Calendar.SECOND, 0); current.set(Calendar.MILLISECOND, 0)

        val todayZero = Calendar.getInstance()
        todayZero.set(Calendar.HOUR_OF_DAY, 0); todayZero.set(Calendar.MINUTE, 0); todayZero.set(
        Calendar.SECOND,
        0
    ); todayZero.set(Calendar.MILLISECOND, 0)

        val diffMillis = todayZero.timeInMillis - current.timeInMillis
        val diffDays = (diffMillis / (24 * 60 * 60 * 1000)).toInt()

        if (diffDays in 0..6) diffDays else -1
    }

    var selectedDayIndex by remember { mutableStateOf(if (todayIndex != -1) todayIndex else 0) }

    // 确保 selectedDayIndex 在有效范围内
    LaunchedEffect(todayIndex) {
        if (todayIndex != -1 && isWeekView) {
            // 如果回到本周且是周视图，不需要强制改变 selectedDayIndex，但在日视图切换时可能需要
        }
    }

    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val periodSlots = generatePeriodSlots()
    val weekNumber = ScheduleDateUtils.calculateWeekNumber(currentWeekStart, settings)

    // 计算本周日期
    val weekDates = remember(currentWeekStart) {
        val calendar = currentWeekStart.clone() as Calendar
        (0..6).map {
            val date = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            SimpleDateFormat("MM.dd", Locale.getDefault()).format(date)
        }
    }

    // 根据设置过滤可见的日期
    val visibleDayIndices = remember(settings.showSaturday, settings.showSunday, isWeekView, selectedDayIndex) {
        if (isWeekView) {
            val indices = mutableListOf<Int>()
            (0..4).forEach { indices.add(it) } // 周一到周五
            if (settings.showSaturday) indices.add(5) // 周六
            if (settings.showSunday) indices.add(6) // 周日
            indices
        } else {
            listOf(selectedDayIndex)
        }
    }

    fun onPreviousClick() {
        if (isWeekView) {
            currentWeekStart = getPreviousWeek(currentWeekStart)
        } else {
            if (selectedDayIndex > 0) {
                selectedDayIndex--
            } else {
                currentWeekStart = getPreviousWeek(currentWeekStart)
                selectedDayIndex = 6
            }
        }
    }

    fun onNextClick() {
        if (isWeekView) {
            currentWeekStart = getNextWeek(currentWeekStart)
        } else {
            if (selectedDayIndex < 6) {
                selectedDayIndex++
            } else {
                currentWeekStart = getNextWeek(currentWeekStart)
                selectedDayIndex = 0
            }
        }
    }

    val scrollState = rememberScrollState()

    val slotHeights = remember(periodSlots) {
        periodSlots.map { slot ->
            56.dp * (slot.durationMinutes / 45f).coerceAtLeast(1f)
        }
    }

    val slotTopOffsets = remember(periodSlots) {
        val offsets = mutableListOf<androidx.compose.ui.unit.Dp>()
        var currentOffset = 0.dp
        periodSlots.forEachIndexed { index, slot ->
            offsets.add(currentOffset)
            currentOffset += slotHeights[index]
            if (slot.label == "5" || slot.label == "10") {
                currentOffset += 10.dp
            }
        }
        offsets
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                                .width(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            var showWeekSelector by remember { mutableStateOf(false) }
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "第${weekNumber}周",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { showWeekSelector = true }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showWeekSelector,
                                    onDismissRequest = { showWeekSelector = false }
                                ) {
                                    (1..settings.totalWeeks).forEach { week ->
                                        DropdownMenuItem(
                                            text = { Text("第${week}周") },
                                            onClick = {
                                                currentWeekStart = ScheduleDateUtils.getWeekStartDate(week, settings)
                                                showWeekSelector = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        visibleDayIndices.forEach { index ->
                            val day = weekDays[index]
                            val isToday = index == todayIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.2f
                                        ) else Color.Transparent
                                    ),
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

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
val dayWidth = (this.maxWidth - 36.dp) / visibleDayIndices.size

                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            Box {
                                // Grid Layer
                                Column {
                                    periodSlots.forEachIndexed { index, slot ->
                                        WeekViewGridRow(
                                            slot = slot,
                                            visibleDayIndices = visibleDayIndices,
                                            todayIndex = todayIndex,
                                            height = slotHeights[index]
                                        )
                                        if (slot.label == "5" || slot.label == "10") {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }
                                }

                                // Courses Layer
                                courses.forEachIndexed { courseIdx, course ->
                                    // 检查课程是否在当前周
                                    val isInWeek = if (!course.weeks.isNullOrBlank()) {
                                        // 如果weeks字段不为空，使用精确的周数列表
                                        course.weeks.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(weekNumber)
                                    } else {
                                        // 否则使用startWeek和endWeek的范围
                                        weekNumber >= course.startWeek && weekNumber <= course.endWeek
                                    }
                                    if (!isInWeek) {
                                        return@forEachIndexed
                                    }

                                    val isOverlapping = courses.any { other ->
                                        if (other == course) return@any false
                                        if (other.dayOfWeek != course.dayOfWeek) return@any false
                                        
                                        val otherInWeek = if (!other.weeks.isNullOrBlank()) {
                                            other.weeks.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(weekNumber)
                                        } else {
                                            weekNumber >= other.startWeek && weekNumber <= other.endWeek
                                        }
                                        if (!otherInWeek) return@any false
                                        
                                        isTimeOverlapping(course.startTime, course.endTime, other.startTime, other.endTime)
                                    }

                                    val overlappingSlots = periodSlots.indices.filter {
                                        isCourseInSlot(course.startTime, course.endTime, periodSlots[it])
                                    }

                                    if (overlappingSlots.isNotEmpty()) {
                                        val startSlotIndex = overlappingSlots.first()
                                        val endSlotIndex = overlappingSlots.last()

                                        val dayIndex = course.dayOfWeek - 1
                                        val colIndex = visibleDayIndices.indexOf(dayIndex)

                                        if (colIndex != -1) {
                                            val topOffset = slotTopOffsets[startSlotIndex]
                                            val bottomOffset = slotTopOffsets[endSlotIndex] + slotHeights[endSlotIndex]
                                            val height = bottomOffset - topOffset
                                            val leftOffset = 36.dp + dayWidth * colIndex

                                            CourseWeekCell(
                                                course = course,
                                                settings = settings,
                                                courseIndex = courseIdx,
                                                dayOfWeek = course.dayOfWeek,
                                                isToday = dayIndex == todayIndex,
                                                isOverlapping = isOverlapping,
                                                modifier = Modifier
                                                    .offset(x = leftOffset, y = topOffset)
                                                    .size(width = dayWidth, height = height)
                                                    .clickable { onCourseClick(course) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    @Composable
    fun WeekViewGridRow(
        slot: PeriodSlot,
        visibleDayIndices: List<Int>,
        todayIndex: Int,
        height: androidx.compose.ui.unit.Dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 时间列
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${slot.start}\n${slot.end}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 每天的课程格子
            visibleDayIndices.forEach { dayIndex ->
                val isToday = dayIndex == todayIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.1f
                            ) else Color.Transparent
                        )
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                )
            }
        }
    }

    @Composable
    fun CourseWeekCell(
        course: Course,
        settings: com.example.myapplication.data.model.ScheduleSettings,
        courseIndex: Int,
        dayOfWeek: Int, // 1-7, 1为周一
        isToday: Boolean = false,
        isOverlapping: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        // 使用课程自定义颜色，如果为空则使用配色方案
        // 结合课程名称、星期和时间段分配颜色，避免相邻课程使用相同颜色
        val courseColorStr = if (course.color.isBlank()) {
            ColorSchemeUtils.getColorForCourseByName(
                courseName = course.courseName,
                colorScheme = settings.colorScheme,
                dayOfWeek = course.dayOfWeek,
                startTime = course.startTime
            )
        } else {
            course.color
        }
        val courseColor = ColorSchemeUtils.parseColor(courseColorStr)
        
        // 使用课程自定义文字颜色，如果没有则根据背景颜色自动计算
        val textColor = if (!course.textColor.isNullOrBlank()) {
            ColorSchemeUtils.parseColor(course.textColor)
        } else {
            when (settings.colorScheme) {
                "default", "green" -> Color.White
                "pastel" -> Color.Black.copy(alpha = 0.7f) // 浅色系使用浅黑色文字
                else -> {
                    // 其他主题根据背景颜色自动计算文字颜色
                    val brightness = (courseColor.red * 0.299 + courseColor.green * 0.587 + courseColor.blue * 0.114).toFloat()
                    if (brightness > 0.5f) Color.Black else Color.White
                }
            }
        }
        // 判断是否为浅色背景（用于分隔线颜色）
        val isLightColor = when {
            (settings.colorScheme == "default" || settings.colorScheme == "green") && course.textColor.isNullOrBlank() -> false
            settings.colorScheme == "pastel" && course.textColor.isNullOrBlank() -> true // 浅色系使用浅黑色文字，所以是浅色背景
            else -> {
                val brightness = (courseColor.red * 0.299 + courseColor.green * 0.587 + courseColor.blue * 0.114).toFloat()
                brightness > 0.5f
            }
        }
        val dividerColor = if (isLightColor) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f)

        Card(
            modifier = modifier
                .padding(2.dp)
                .shadow(if (isToday) 4.dp else 2.dp, shape = RoundedCornerShape(8.dp))
                .then(
                    if (isToday) Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    ) else Modifier
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = courseColor.copy(alpha = 0.95f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                        color = textColor,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (settings.showLocation || settings.showTeacher) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            thickness = 0.5.dp,
                            color = dividerColor
                        )
                    }
                    
                    if (settings.showLocation && !course.location.isNullOrEmpty()) {
                        Text(
                            text = course.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = if (isLightColor) 0.8f else 0.9f),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (settings.showTeacher && !course.teacherName.isNullOrEmpty()) {
                        Text(
                            text = course.teacherName,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = if (isLightColor) 0.8f else 0.9f),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isOverlapping) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "叠",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CourseDetailDialog(
        course: Course,
        onDismiss: () -> Unit,
        onEdit: () -> Unit
    ) {
        val courseColor = try {
            Color(android.graphics.Color.parseColor(course.color))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
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
                    InfoRow(
                        "上课时间",
                        "${getDayName(course.dayOfWeek)} ${course.startTime} - ${course.endTime}"
                    )

                    // 教学周
                    val weeksText = if (!course.weeks.isNullOrBlank()) {
                        WeekDisplayUtils.formatWeeksFromString(course.weeks)
                    } else {
                        WeekDisplayUtils.formatWeekRange(course.startWeek, course.endWeek)
                    }
                    InfoRow("教学周", weeksText)

                    // 地点
                    if (!course.location.isNullOrEmpty()) {
                        InfoRow("上课地点", course.location)
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

    // 工具函数（已在上方定义，使用设置）

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
        PeriodSlot("14", "21:30", "22:15")
    )

    fun isCourseInSlot(courseStart: String, courseEnd: String, slot: PeriodSlot): Boolean {
        val slotStart = parseTimeToMinutes(slot.start)
        val slotEnd = parseTimeToMinutes(slot.end)
        val courseStartMinutes = parseTimeToMinutes(courseStart)
        val courseEndMinutes = parseTimeToMinutes(courseEnd)
        return courseStartMinutes < slotEnd && courseEndMinutes > slotStart
    }

    fun parseTimeToMinutes(time: String): Int {
        try {
            val parts = time.split(":")
            if (parts.size >= 2) {
                return parts[0].toInt() * 60 + parts[1].toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun isTimeOverlapping(start1: String, end1: String, start2: String, end2: String): Boolean {
        val s1 = parseTimeToMinutes(start1)
        val e1 = parseTimeToMinutes(end1)
        val s2 = parseTimeToMinutes(start2)
        val e2 = parseTimeToMinutes(end2)
        return s1 < e2 && s2 < e1
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



