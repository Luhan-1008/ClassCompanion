package com.example.myapplication.utils

import androidx.compose.ui.graphics.Color
import com.example.myapplication.data.model.ScheduleSettings

object ColorSchemeUtils {
    private val colorSchemes = mapOf(
        "default" to listOf("#216ad8", "#cd1e2b", "#9830b0", "#ffc107", "#216ad8", "#cd1e2b", "#9830b0", "#ffc107", "#216ad8", "#cd1e2b", "#9830b0", "#ffc107", "#216ad8"),
        "blue" to listOf("#1f8bef", "#1f6def", "#1f4fef", "#1f31ef", "#2a1fef", "#1f8bef", "#1f6def", "#1f4fef", "#1f31ef", "#2a1fef", "#1f8bef", "#1f6def", "#1f4fef"),
        "green" to listOf("#0b5317", "#117a22", "#16a02d", "#1bc738", "#0b5317", "#117a22", "#16a02d", "#1bc738", "#0b5317", "#117a22", "#16a02d", "#1bc738", "#0b5317"),
        "purple" to listOf("#4A148C", "#6A1B9A", "#7B1FA2", "#8E24AA", "#9C27B0", "#8E24AA", "#7B1FA2", "#6A1B9A", "#4A148C", "#6A1B9A", "#7B1FA2", "#8E24AA", "#7B1FA2"),
        "red" to listOf("#e7135f", "#e71340", "#e71322", "#e72213", "#e74013", "#e7135f", "#e71340", "#e71322", "#e72213", "#e74013", "#e7135f", "#e71340", "#e71322"),
        "pastel" to listOf("#fff8dc", "#e8d6ec", "#f8d6dd", "#d6ecfc", "#d4eae0", "#fff8dc", "#e8d6ec", "#f8d6dd", "#d6ecfc", "#d4eae0", "#fff8dc", "#e8d6ec", "#f8d6dd")
    )
    
    /**
     * 根据课程索引获取颜色（向后兼容）
     */
    fun getColorForCourse(courseIndex: Int, colorScheme: String): String {
        val colors = colorSchemes[colorScheme] ?: colorSchemes["default"]!!
        return colors[courseIndex % colors.size]
    }
    
    /**
     * 根据课程名称获取颜色（推荐使用）
     * 结合课程名称、星期和时间段来分配颜色，避免相邻课程使用相同颜色
     * @param courseName 课程名称
     * @param dayOfWeek 星期（1-7，1为周一）
     * @param startTime 开始时间（用于区分时间段）
     * @param colorScheme 配色方案
     */
    fun getColorForCourseByName(
        courseName: String, 
        colorScheme: String,
        dayOfWeek: Int = 0,
        startTime: String = ""
    ): String {
        val colors = colorSchemes[colorScheme] ?: colorSchemes["default"]!!
        
        // 结合课程名称、星期和时间段生成唯一标识符
        // 这样同一门课程在不同星期或不同时间段会使用不同颜色，避免相邻课程颜色相同
        val identifier = if (dayOfWeek > 0 && startTime.isNotEmpty()) {
            "${courseName}_${dayOfWeek}_${startTime}"
        } else {
            courseName
        }
        
        // 使用更好的哈希算法来分配颜色，确保颜色分布更均匀
        // 使用 FNV-1a 哈希算法的简化版本，确保相邻课程颜色不同
        var hash = 2166136261L // FNV offset basis
        for (char in identifier) {
            hash = hash xor char.code.toLong()
            hash = hash * 16777619L // FNV prime
        }
        
        // 确保索引为正数
        val index = ((hash and 0x7FFFFFFF) % colors.size).toInt()
        
        return colors[index]
    }
    
    fun parseColor(colorStr: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorStr))
        } catch (e: Exception) {
            Color.Gray
        }
    }
    
    /**
     * 获取所有配色方案
     * @return 配色方案名称到颜色列表的映射
     */
    fun getAllColorSchemes(): Map<String, List<String>> {
        return colorSchemes
    }
    
    /**
     * 获取指定配色方案的颜色列表
     * @param colorScheme 配色方案名称
     * @return 颜色列表
     */
    fun getColorSchemeColors(colorScheme: String): List<String> {
        return colorSchemes[colorScheme] ?: colorSchemes["default"]!!
    }
}

/**
 * 格式化周数显示，将连续周数合并为范围格式
 * 例如：[1,2,3,4] -> "1-4周"
 *      [1,3,5,7] -> "第1周、第3周、第5周、第7周"
 *      [1,2,3,5,7,8,9] -> "1-3周、第5周、7-9周"
 */
object WeekDisplayUtils {
    /**
     * 将周数列表格式化为显示字符串
     * @param weeks 周数列表（已排序）
     * @param useRange 是否使用范围格式（默认true）
     * @return 格式化后的字符串
     */
    fun formatWeeks(weeks: List<Int>, useRange: Boolean = true): String {
        if (weeks.isEmpty()) return ""
        if (weeks.size == 1) return "第${weeks.first()}周"
        
        if (!useRange) {
            // 如果不使用范围格式，直接列出所有周数
            return weeks.joinToString("、") { "第${it}周" }
        }
        
        // 将连续周数合并为范围
        val ranges = mutableListOf<String>()
        var start = weeks[0]
        var end = start
        
        for (i in 1 until weeks.size) {
            if (weeks[i] == end + 1) {
                // 连续，扩展范围
                end = weeks[i]
            } else {
                // 不连续，保存当前范围并开始新范围
                if (start == end) {
                    ranges.add("第${start}周")
                } else {
                    ranges.add("${start}-${end}周")
                }
                start = weeks[i]
                end = start
            }
        }
        
        // 处理最后一个范围
        if (start == end) {
            ranges.add("第${start}周")
        } else {
            ranges.add("${start}-${end}周")
        }
        
        return ranges.joinToString("、")
    }
    
    /**
     * 从weeks字符串（逗号分隔）格式化显示
     */
    fun formatWeeksFromString(weeksString: String?): String {
        if (weeksString.isNullOrBlank()) return ""
        val weeks = weeksString.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        return formatWeeks(weeks)
    }
    
    /**
     * 从startWeek和endWeek格式化显示（向后兼容）
     */
    fun formatWeekRange(startWeek: Int, endWeek: Int): String {
        if (startWeek == endWeek) {
            return "第${startWeek}周"
        }
        return "${startWeek}-${endWeek}周"
    }
}
