package com.example.myapplication.utils

import com.example.myapplication.data.model.Course
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream

/**
 * 课程导入解析器
 * 支持Excel (.xlsx, .xls) 和 CSV 格式
 */
object CourseImportParser {
    
    /**
     * 从输入流解析课程数据
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于判断文件类型）
     * @param userId 用户ID
     * @return 解析后的课程列表
     */
    fun parseCourses(
        inputStream: InputStream,
        fileName: String,
        userId: Int
    ): List<Course> {
        return when {
            fileName.endsWith(".csv", ignoreCase = true) -> {
                parseCsv(inputStream, userId)
            }
            fileName.endsWith(".xlsx", ignoreCase = true) || 
            fileName.endsWith(".xls", ignoreCase = true) -> {
                parseExcel(inputStream, userId)
            }
            else -> {
                throw IllegalArgumentException("不支持的文件格式，请使用 .csv, .xlsx 或 .xls 文件")
            }
        }
    }
    
    /**
     * 解析CSV文件
     * CSV格式：课程名称,课程代码,教师,地点,星期,开始时间,结束时间,开始周,结束周
     * 示例：高等数学,MA001,张老师,教学楼A101,1,08:00,09:40,1,16
     */
    private fun parseCsv(inputStream: InputStream, userId: Int): List<Course> {
        val courses = mutableListOf<Course>()
        val colors = listOf("#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#F44336", "#00BCD4", "#795548")
        var colorIndex = 0
        
        inputStream.bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (index == 0) {
                    // 跳过表头
                    return@forEachIndexed
                }
                
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) {
                    return@forEachIndexed
                }
                
                try {
                    // 处理CSV中的引号和逗号
                    val parts = parseCsvLine(trimmedLine)
                    if (parts.size >= 6) {
                        val course = Course(
                            userId = userId,
                            courseName = parts[0].trim(),
                            courseCode = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() },
                            teacherName = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() },
                            location = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() },
                            dayOfWeek = parseDayOfWeek(parts.getOrNull(4)?.trim() ?: "1"),
                            startTime = parts.getOrNull(5)?.trim() ?: "08:00",
                            endTime = parts.getOrNull(6)?.trim() ?: "09:40",
                            startWeek = parts.getOrNull(7)?.trim()?.toIntOrNull() ?: 1,
                            endWeek = parts.getOrNull(8)?.trim()?.toIntOrNull() ?: 16,
                            color = colors[colorIndex % colors.size]
                        )
                        courses.add(course)
                        colorIndex++
                    }
                } catch (e: Exception) {
                    // 跳过解析失败的行
                    e.printStackTrace()
                }
            }
        }
        
        return courses
    }
    
    /**
     * 解析CSV行，处理引号内的逗号
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        result.add(current.toString())
        return result
    }
    
    /**
     * 解析Excel文件
     * Excel格式：第一行为表头，从第二行开始为数据
     * 列顺序：课程名称,课程代码,教师,地点,星期,开始时间,结束时间,开始周,结束周
     */
    private fun parseExcel(inputStream: InputStream, userId: Int): List<Course> {
        val courses = mutableListOf<Course>()
        val colors = listOf("#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#F44336", "#00BCD4", "#795548")
        var colorIndex = 0
        
        try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0) // 读取第一个工作表
            
            // 跳过第一行（表头）
            for (rowIndex in 1..sheet.lastRowNum) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) continue
                
                try {
                    val courseName = getCellValue(row, 0)?.trim() ?: continue
                    if (courseName.isEmpty()) continue
                    
                    val course = Course(
                        userId = userId,
                        courseName = courseName,
                        courseCode = getCellValue(row, 1)?.trim()?.takeIf { it.isNotEmpty() },
                        teacherName = getCellValue(row, 2)?.trim()?.takeIf { it.isNotEmpty() },
                        location = getCellValue(row, 3)?.trim()?.takeIf { it.isNotEmpty() },
                        dayOfWeek = parseDayOfWeek(getCellValue(row, 4) ?: "1"),
                        startTime = getCellValue(row, 5)?.trim() ?: "08:00",
                        endTime = getCellValue(row, 6)?.trim() ?: "09:40",
                        startWeek = getCellValue(row, 7)?.trim()?.toIntOrNull() ?: 1,
                        endWeek = getCellValue(row, 8)?.trim()?.toIntOrNull() ?: 16,
                        color = colors[colorIndex % colors.size]
                    )
                    courses.add(course)
                    colorIndex++
                } catch (e: Exception) {
                    // 跳过解析失败的行
                    e.printStackTrace()
                }
            }
            
            workbook.close()
        } catch (e: Exception) {
            throw Exception("解析Excel文件失败: ${e.message}", e)
        }
        
        return courses
    }
    
    /**
     * 获取单元格值
     */
    private fun getCellValue(row: Row, columnIndex: Int): String? {
        val cell = row.getCell(columnIndex) ?: return null
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    cell.numericCellValue.toInt().toString()
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            org.apache.poi.ss.usermodel.CellType.FORMULA -> cell.cellFormula
            else -> null
        }
    }
    
    /**
     * 解析星期
     * 支持：1-7, 周一-周日, Monday-Sunday, Mon-Sun
     */
    private fun parseDayOfWeek(dayStr: String): Int {
        val trimmed = dayStr.trim().lowercase()
        return when {
            trimmed.matches(Regex("\\d+")) -> {
                val day = trimmed.toIntOrNull() ?: 1
                if (day in 1..7) day else 1
            }
            trimmed.contains("一") || trimmed.contains("mon") -> 1
            trimmed.contains("二") || trimmed.contains("tue") -> 2
            trimmed.contains("三") || trimmed.contains("wed") -> 3
            trimmed.contains("四") || trimmed.contains("thu") -> 4
            trimmed.contains("五") || trimmed.contains("fri") -> 5
            trimmed.contains("六") || trimmed.contains("sat") -> 6
            trimmed.contains("日") || trimmed.contains("sun") -> 7
            else -> 1
        }
    }
    
    /**
     * 生成CSV模板内容
     */
    fun generateCsvTemplate(): String {
        return """课程名称,课程代码,教师,地点,星期,开始时间,结束时间,开始周,结束周
高等数学,MA001, 张老师,教学楼A101,1,08:00,09:40,1,16
线性代数,MA002, 李老师,教学楼B201,2,10:00,11:40,1,16
大学英语,EN001, 王老师,教学楼C301,3,14:00,15:40,1,16"""
    }

    /**
     * 导出课程为CSV格式
     */
    fun exportCoursesToCsv(courses: List<Course>): String {
        val header = "课程名称,课程代码,教师,地点,星期,开始时间,结束时间,开始周,结束周\n"
        val content = courses.joinToString("\n") { course ->
            val dayStr = when (course.dayOfWeek) {
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                7 -> "周日"
                else -> course.dayOfWeek.toString()
            }
            
            // 处理可能包含逗号的字段，用引号包裹
            val name = escapeCsv(course.courseName)
            val code = escapeCsv(course.courseCode ?: "")
            val teacher = escapeCsv(course.teacherName ?: "")
            val location = escapeCsv(course.location ?: "")
            
            "$name,$code,$teacher,$location,$dayStr,${course.startTime},${course.endTime},${course.startWeek},${course.endWeek}"
        }
        return header + content
    }
    
    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}

