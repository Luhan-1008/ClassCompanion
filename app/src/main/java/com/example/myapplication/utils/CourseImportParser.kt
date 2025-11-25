package com.example.myapplication.utils

import com.example.myapplication.data.model.Course
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

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
        try {
            WorkbookFactory.create(inputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                return if (isWeeklyTemplate(sheet)) {
                    parseWeeklyTemplate(sheet, userId)
                } else {
                    parseStandardSheet(sheet, userId)
                }
            }
        } catch (e: Exception) {
            throw Exception("解析Excel文件失败: ${e.message}", e)
        }
    }
    
    private fun parseStandardSheet(sheet: Sheet, userId: Int): List<Course> {
        val courses = mutableListOf<Course>()
        val colors = listOf("#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#F44336", "#00BCD4", "#795548")
        var colorIndex = 0
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
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
            } catch (_: Exception) {
                // ignore row
            }
        }
        return courses
    }
    
    private fun isWeeklyTemplate(sheet: Sheet): Boolean {
        val header = sheet.getRow(0) ?: return false
        var matchCount = 0
        for (col in 1..header.lastCellNum) {
            val value = getCellValue(header, col)?.trim()?.lowercase() ?: continue
            if (value.contains("周") || value.contains("星期") || value.contains("mon") || value == "1") {
                matchCount++
            }
        }
        return matchCount >= 3
    }
    
    private fun parseWeeklyTemplate(sheet: Sheet, userId: Int): List<Course> {
        val header = sheet.getRow(0) ?: return emptyList()
        val columnDayMap = mutableMapOf<Int, Int>()
        for (col in 1..header.lastCellNum) {
            val label = getCellValue(header, col)?.trim() ?: continue
            columnDayMap[col] = parseDayOfWeek(label)
        }
        val colors = listOf("#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#F44336", "#00BCD4", "#795548")
        var colorIndex = 0
        val result = mutableListOf<Course>()
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val timeLabel = getCellValue(row, 0)?.trim().orEmpty()
            val (defaultStart, defaultEnd) = parseTimeRange(timeLabel)
            for ((colIndex, dayOfWeek) in columnDayMap) {
                val cellValue = getCellValue(row, colIndex)?.trim().orEmpty()
                if (cellValue.isEmpty()) continue
                val blocks = cellValue.split("\n\n", "\r\n\r\n").map { it.trim() }.filter { it.isNotEmpty() }
                for (block in blocks) {
                    val parsed = parseWeeklyCell(block)
                    val course = Course(
                        userId = userId,
                        courseName = parsed.name,
                        teacherName = parsed.teacher,
                        location = parsed.location,
                        dayOfWeek = dayOfWeek,
                        startTime = parsed.startTime ?: defaultStart,
                        endTime = parsed.endTime ?: defaultEnd,
                        startWeek = parsed.startWeek ?: 1,
                        endWeek = parsed.endWeek ?: 16,
                        color = colors[colorIndex % colors.size]
                    )
                    result.add(course)
                    colorIndex++
                }
            }
        }
        return result
    }
    
    private data class WeeklyCellInfo(
        val name: String,
        val teacher: String?,
        val location: String?,
        val startWeek: Int?,
        val endWeek: Int?,
        val startTime: String?,
        val endTime: String?
    )
    
    private fun parseWeeklyCell(raw: String): WeeklyCellInfo {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var name = lines.firstOrNull().orEmpty()
        var teacher: String? = null
        var location: String? = null
        var startWeek: Int? = null
        var endWeek: Int? = null
        var startTime: String? = null
        var endTime: String? = null
        lines.drop(1).forEach { line ->
            when {
                line.startsWith("教师") || line.startsWith("老师") -> {
                    teacher = line.substringAfter(":").substringAfter("：").trim().ifEmpty { null }
                }
                line.startsWith("地点") || line.startsWith("教室") -> {
                    location = line.substringAfter(":").substringAfter("：").trim().ifEmpty { null }
                }
                line.contains("周") -> {
                    val matcher = Regex("(\\d+)\\D+(\\d+)").find(line)
                    if (matcher != null) {
                        startWeek = matcher.groupValues[1].toIntOrNull()
                        endWeek = matcher.groupValues[2].toIntOrNull()
                    } else {
                        line.filter { it.isDigit() }.toIntOrNull()?.let {
                            startWeek = it
                            endWeek = it
                        }
                    }
                }
                line.contains("~") || line.contains("－") || line.contains("-") -> {
                    val normalized = line.replace("～", "~").replace("－", "-")
                    val parts = normalized.split("~", "-").map { it.trim() }
                    if (parts.size == 2) {
                        startTime = normalizeTime(parts[0])
                        endTime = normalizeTime(parts[1])
                    }
                }
                else -> if (name.isBlank()) name = line
            }
        }
        if (name.isBlank()) name = "未命名课程"
        return WeeklyCellInfo(name, teacher, location, startWeek, endWeek, startTime, endTime)
    }
    
    private fun parseTimeRange(label: String): Pair<String, String> {
        val matcher = Regex("(\\d{1,2}:?\\d{2})\\s*[-~至到]\\s*(\\d{1,2}:?\\d{2})").find(label)
        if (matcher != null) {
            val start = normalizeTime(matcher.groupValues[1])
            val end = normalizeTime(matcher.groupValues[2])
            return start to end
        }
        return "08:00" to "09:40"
    }
    
    private fun normalizeTime(value: String): String {
        val cleaned = value.replace("：", ":").replace("时", ":").replace("分", "")
        return when {
            cleaned.matches(Regex("\\d{1,2}:\\d{2}")) -> cleaned.padStart(5, '0')
            cleaned.matches(Regex("\\d{3,4}")) -> {
                val padded = cleaned.padStart(4, '0')
                padded.substring(0, 2) + ":" + padded.substring(2)
            }
            else -> cleaned
        }
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
高等数学,MA001,张老师,教学楼A101,1,08:00,09:40,1,16
线性代数,MA002,李老师,教学楼B201,2,10:00,11:40,1,16
大学英语,EN001,王老师,教学楼C301,3,14:00,15:40,1,16"""
    }
    
    fun exportCoursesToWeeklyExcel(courses: List<Course>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("课程表")
        sheet.createFreezePane(1, 1)
        val headerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFont(workbook.createFont().apply {
                bold = true
            })
        }
        val cellStyle = workbook.createCellStyle().apply {
            verticalAlignment = VerticalAlignment.TOP
            wrapText = true
        }
        
        val slotDefinitions = defaultTimeSlots.toMutableList()
        courses.forEach { course ->
            if (!slotDefinitions.any { it.startTime == course.startTime && it.endTime == course.endTime }) {
                val label = "第${course.startTime}-${course.endTime}"
                slotDefinitions += TimeSlot(label, course.startTime, course.endTime)
            }
        }
        val sortedSlots = slotDefinitions.sortedBy { it.startTime }
        
        val headerRow = sheet.createRow(0)
        headerRow.heightInPoints = 28f
        headerRow.createCell(0).apply {
            setCellValue("节次")
            setCellStyle(headerStyle)
        }
        dayHeaders.forEachIndexed { index, label ->
            val cell = headerRow.createCell(index + 1)
            cell.setCellValue(label)
            cell.setCellStyle(headerStyle)
            sheet.setColumnWidth(index + 1, 22 * 256)
        }
        sheet.setColumnWidth(0, 14 * 256)
        
        val grouped = courses.groupBy { slotKey(it.startTime, it.endTime) to it.dayOfWeek }
        sortedSlots.forEachIndexed { idx, slot ->
            val row = sheet.createRow(idx + 1)
            row.heightInPoints = 160f
            row.createCell(0).apply {
                setCellValue(slot.label)
                setCellStyle(headerStyle)
            }
            for (day in 1..7) {
                val cell = row.createCell(day)
                val list = grouped[slotKey(slot.startTime, slot.endTime) to day].orEmpty()
                val content = list.joinToString("\n\n") { course ->
                    buildString {
                        append(course.courseName)
                        course.teacherName?.let { append("\n教师：$it") }
                        course.location?.let { append("\n地点：$it") }
                        append("\n周次：第${course.startWeek}-${course.endWeek}周")
                    }
                }
                cell.setCellValue(content)
                cell.setCellStyle(cellStyle)
            }
            row
        }
        
        val remarkRow = sheet.createRow(sortedSlots.size + 2)
        remarkRow.createCell(0).apply {
            setCellValue("说明：导出的时间段取自课程的起止时间，若与模板不同请手动调整。")
        }
        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        return outputStream.toByteArray()
    }
    
    private fun slotKey(start: String, end: String) = "${start}_${end}"
    
    private val defaultTimeSlots = listOf(
        TimeSlot("第1、2节", "08:00", "09:40"),
        TimeSlot("第3、4节", "10:00", "11:40"),
        TimeSlot("第5、6节", "14:00", "15:40"),
        TimeSlot("第7、8节", "16:00", "17:40"),
        TimeSlot("第9、10节", "18:30", "20:10"),
        TimeSlot("第11、12节", "20:20", "22:00")
    )
    
    private val dayHeaders = listOf("星期一","星期二","星期三","星期四","星期五","星期六","星期日")
    
    private data class TimeSlot(val label: String, val startTime: String, val endTime: String)
}

