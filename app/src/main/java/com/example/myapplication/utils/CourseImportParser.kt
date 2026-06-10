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
     * @param colorScheme 配色方案名称（用于分配颜色）
     * @return 解析后的课程列表
     */
    fun parseCourses(
        inputStream: InputStream,
        fileName: String,
        userId: Int,
        colorScheme: String = "default"
    ): List<Course> {
        return when {
            fileName.endsWith(".csv", ignoreCase = true) -> {
                parseCsv(inputStream, userId, colorScheme)
            }
            fileName.endsWith(".xlsx", ignoreCase = true) || 
            fileName.endsWith(".xls", ignoreCase = true) -> {
                parseExcel(inputStream, userId, colorScheme)
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
    private fun parseCsv(inputStream: InputStream, userId: Int, colorScheme: String = "default"): List<Course> {
        val courses = mutableListOf<Course>()
        
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
                        val startWeek = parts.getOrNull(7)?.trim()?.toIntOrNull() ?: 1
                        val endWeek = parts.getOrNull(8)?.trim()?.toIntOrNull() ?: 16
                        // 解析周数：支持范围（如"1-16"）或列表（如"1,2,3,5,7"）
                        val weeksString = parts.getOrNull(8)?.trim()?.let { weekStr ->
                            parseWeekStringToSet(weekStr)?.sorted()?.joinToString(",")
                        } ?: (startWeek..endWeek).toList().joinToString(",")
                        
                        val course = Course(
                            userId = userId,
                            courseName = parts[0].trim(),
                            courseCode = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() },
                            teacherName = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() },
                            location = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() },
                            dayOfWeek = parseDayOfWeek(parts.getOrNull(4)?.trim() ?: "1"),
                            startTime = parts.getOrNull(5)?.trim() ?: "08:00",
                            endTime = parts.getOrNull(6)?.trim() ?: "09:40",
                            startWeek = startWeek,
                            endWeek = endWeek,
                            weeks = weeksString,
                            reminderEnabled = true,
                            reminderMinutes = 15,
                            color = "", // 空字符串表示使用主题颜色，切换主题时会自动更新
                            textColor = null // null表示根据背景颜色自动计算
                        )
                        courses.add(course)
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
     */
    private fun parseExcel(inputStream: InputStream, userId: Int, colorScheme: String = "default"): List<Course> {
        try {
            WorkbookFactory.create(inputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                // 尝试寻找网格状表头
                val headerRowIndex = findHeaderRowIndex(sheet)
                val courses = if (headerRowIndex != -1) {
                    parseWeeklyTemplate(sheet, headerRowIndex, userId, colorScheme)
                } else {
                    parseStandardSheet(sheet, userId, colorScheme)
                }
                println("Debug: 解析出原始课程数: ${courses.size}")
                    // 不合并课程：识别到就导入一次，保留所有解析结果
                    return courses
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("解析Excel文件失败: ${e.message}", e)
        }
    }

    // 寻找包含"星期"或"周一"的行作为表头
    private fun findHeaderRowIndex(sheet: Sheet): Int {
        var bestRowIndex = -1
        var maxWeekKeywords = 0

        for (i in 0..min(20, sheet.lastRowNum)) {
            val row = sheet.getRow(i) ?: continue
            var currentWeekKeywords = 0
            for (cell in row) {
                val text = getCellValue(row, cell.columnIndex)?.trim() ?: ""
                // 只要能解析出星期几，就算一个关键词
                if (parseDayOfWeek(text) != 0) {
                    currentWeekKeywords++
                } else if (text.contains("星期") || text.contains("周") || text.lowercase().contains("mon")) {
                    // 包含关键字但没解析出具体数字的，也算潜在表头特征
                    currentWeekKeywords++
                }
            }
            
            // 优先选择包含更多星期列的行
            if (currentWeekKeywords > maxWeekKeywords) {
                maxWeekKeywords = currentWeekKeywords
                bestRowIndex = i
            }
        }
        
        // 如果找到了包含星期信息的行，返回该行索引
        if (maxWeekKeywords > 0) {
            return bestRowIndex
        }
        return -1
    }

    private fun parseStandardSheet(sheet: Sheet, userId: Int, colorScheme: String = "default"): List<Course> {
        val courses = mutableListOf<Course>()
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val courseName = getCellValue(row, 0)?.trim() ?: continue
                if (courseName.isEmpty()) continue
                val startWeek = getCellValue(row, 7)?.trim()?.toIntOrNull() ?: 1
                val endWeek = getCellValue(row, 8)?.trim()?.toIntOrNull() ?: 16
                // 解析周数：支持范围或列表
                val weekStr = getCellValue(row, 8)?.trim() ?: "$startWeek-$endWeek"
                val weeksString = parseWeekStringToSet(weekStr)?.sorted()?.joinToString(",")
                    ?: (startWeek..endWeek).toList().joinToString(",")
                
                val course = Course(
                    userId = userId,
                    courseName = courseName,
                    courseCode = getCellValue(row, 1)?.trim()?.takeIf { it.isNotEmpty() },
                    teacherName = getCellValue(row, 2)?.trim()?.takeIf { it.isNotEmpty() },
                    location = getCellValue(row, 3)?.trim()?.takeIf { it.isNotEmpty() },
                    dayOfWeek = parseDayOfWeek(getCellValue(row, 4) ?: "1"),
                    startTime = getCellValue(row, 5)?.trim() ?: "08:00",
                    endTime = getCellValue(row, 6)?.trim() ?: "09:40",
                    startWeek = startWeek,
                    endWeek = endWeek,
                    weeks = weeksString,
                    reminderEnabled = true,
                    reminderMinutes = 15,
                    color = "", // 空字符串表示使用主题颜色，切换主题时会自动更新
                    textColor = null // null表示根据背景颜色自动计算
                )
                courses.add(course)
            } catch (_: Exception) {}
        }
        return courses
    }
    
    private fun isWeeklyTemplate(sheet: Sheet): Boolean {
        return findHeaderRowIndex(sheet) != -1
    }
    
    private fun parseWeeklyTemplate(sheet: Sheet, headerRowIndex: Int, userId: Int, colorScheme: String = "default"): List<Course> {
        val header = sheet.getRow(headerRowIndex) ?: return emptyList()
        val columnDayMap = mutableMapOf<Int, Int>()
        
        // 建立列索引到星期的映射
        for (col in 0..header.lastCellNum.toInt()) {
            val label = getCellValue(header, col)?.trim() ?: continue
            val day = parseDayOfWeek(label)
            if (day != 0) {
                columnDayMap[col] = day
            }
        }
        
        val result = mutableListOf<Course>()
        
        // 从表头的下一行开始遍历
        for (rowIndex in (headerRowIndex + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            
            // 尝试获取左侧的时间/节次标签
            var timeLabel = ""
            // 通常第一列或第二列是节次
            for (c in 0..1) {
                val valStr = getCellValue(row, c)?.trim()
                if (!valStr.isNullOrEmpty()) {
                    timeLabel = valStr
                    break
                }
            }
            val (defaultStart, defaultEnd) = parseTimeRange(timeLabel)
            
            for ((colIndex, dayOfWeek) in columnDayMap) {
                val cellValue = getCellValue(row, colIndex)?.trim().orEmpty()
                if (cellValue.isEmpty()) continue
                
                // 处理单元格内可能包含多门课（用双换行或分隔线分隔）
                val blocks = cellValue.split("\n\n", "\r\n\r\n", "----------------", "——————").map { it.trim() }.filter { it.isNotEmpty() }
                
                for (block in blocks) {
                    // 如果包含"不排课"，则跳过该课程块
                    if (block.contains("不排课")) continue

                    val parsedList = parseWeeklyCell(block)
                    for (parsed in parsedList) {
                        // 将周数集合转换为逗号分隔的字符串
                        val weeksString = parsed.weeks?.sorted()?.joinToString(",")
                            ?: run {
                                val start = parsed.startWeek ?: 1
                                val end = parsed.endWeek ?: 16
                                (start..end).toList().joinToString(",")
                            }
                        
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
                            weeks = weeksString,
                            reminderEnabled = true,
                            reminderMinutes = 15,
                            color = "", // 空字符串表示使用主题颜色，切换主题时会自动更新
                            textColor = null // null表示根据背景颜色自动计算
                        )
                        result.add(course)
                    }
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
        val weeks: Set<Int>?, // 精确的周数集合
        val startTime: String?,
        val endTime: String?
    )
    
    private fun parseWeeklyCell(raw: String): List<WeeklyCellInfo> {
        // 预处理：统一括号格式，处理全角符号
        val clean = raw.replace("（", "(").replace("）", ")")
                  .replace("【", "[").replace("】", "]")
        
        val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val results = mutableListOf<WeeklyCellInfo>()
        
        var currentBlock = mutableListOf<String>()
        
        for (line in lines) {
            currentBlock.add(line)
            // 检查是否是时间行（作为课程块的结束标志）
            if (isTimeLine(line)) {
                results.addAll(parseCourseBlock(currentBlock))
                currentBlock = mutableListOf()
            }
        }
        // 处理剩余行（如果有）
        if (currentBlock.isNotEmpty()) {
             results.addAll(parseCourseBlock(currentBlock))
        }
        
        return results
    }

    private fun isTimeLine(line: String): Boolean {
         return (line.startsWith("第") && line.contains("节")) || 
                line.matches(Regex(".*\\d{1,2}:\\d{2}.*"))
    }

    private fun parseCourseBlock(lines: List<String>): List<WeeklyCellInfo> {
        if (lines.isEmpty()) return emptyList()
        
        // 识别时间（最后一行）
        val timeLine = lines.lastOrNull { isTimeLine(it) }
        val (startTime, endTime) = if (timeLine != null) parseTimeRange(timeLine) else (null to null)
        
        // 识别课程名（第一行）
        val rawName = lines.first()
        // 去除 (本) 标记
        val name = rawName.replace("(本)", "").replace("（本）", "")
        
        // 中间行（教师、周次、地点）
        val middleLines = lines.filter { it != rawName && it != timeLine }
        val middleText = middleLines.joinToString("\n")
        
        // 提取周次（支持跨行）- 返回精确的周数集合
        val weekSet = parseWeeksToSet(middleText)
        
        // 移除 [周次] 信息以提取教师和地点
        val textWithoutWeeks = middleText.replace(Regex("\\[[\\s\\S]*?\\]"), "").trim()
        
        val remainingParts = textWithoutWeeks.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var teacher: String? = null
        var location: String? = null
        
        if (remainingParts.isNotEmpty()) {
            teacher = remainingParts[0]
        }
        if (remainingParts.size > 1) {
            location = remainingParts.subList(1, remainingParts.size).joinToString(" ")
        }
        
        // 计算startWeek和endWeek用于向后兼容
        val startWeek = weekSet.minOrNull() ?: 1
        val endWeek = weekSet.maxOrNull() ?: 16
        
        return listOf(WeeklyCellInfo(name, teacher, location, startWeek, endWeek, weekSet, startTime, endTime))
    }

    /**
     * 解析周数文本，返回精确的周数集合（支持多选周数）
     */
    private fun parseWeeksToSet(text: String): Set<Int> {
        val validWeeks = mutableSetOf<Int>()
        
        // 1. 优先匹配括号内的内容，支持跨行匹配
        val bracketMatches = Regex("\\[([\\s\\S]*?)\\]").findAll(text)
        for (match in bracketMatches) {
            val content = match.groupValues[1]
            val weeks = parseWeekContent(content)

            if (content.contains("单")) {
                validWeeks.addAll(weeks.filter { it % 2 != 0 })
            } else if (content.contains("双")) {
                validWeeks.addAll(weeks.filter { it % 2 == 0 })
            } else {
                validWeeks.addAll(weeks)
            }
        }
        
        // 2. 匹配无括号的 "x-y周" 或 "x,y周" 模式
        // 先移除掉已经处理过的括号内容，避免重复
        val textWithoutBrackets = text.replace(Regex("\\[[\\s\\S]*?\\]"), "")
        
        val rawWeekMatches = Regex("([\\d,\\-~to至 ]+)周(\\([单双]\\))?").findAll(textWithoutBrackets)
        for (match in rawWeekMatches) {
            val content = match.groupValues.getOrNull(1) ?: continue
            val modifier = match.groupValues.getOrNull(2) ?: "" // (单) 或 (双) 或 空
            val weeks = parseWeekContent(content)
            
            if (modifier.contains("单")) {
                validWeeks.addAll(weeks.filter { it % 2 != 0 })
            } else if (modifier.contains("双")) {
                validWeeks.addAll(weeks.filter { it % 2 == 0 })
            } else {
                validWeeks.addAll(weeks)
            }
        }
        
        // 如果没有找到任何周次，默认1-16
        if (validWeeks.isEmpty()) {
            return (1..16).toSet()
        }
        
        return validWeeks
    }
    
    /**
     * 解析周数字符串（用于CSV和标准Excel格式），支持范围或列表
     * 例如："1-16" 或 "1,2,3,5,7" 或 "1-8,10-16"
     */
    private fun parseWeekStringToSet(weekStr: String): Set<Int>? {
        if (weekStr.isEmpty()) return null
        
        val weeks = mutableSetOf<Int>()
        // 分割逗号
        val parts = weekStr.split(",", "，")
        
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            
            // 匹配范围 x-y
            val rangeMatch = Regex("(\\d+)[-~至](\\d+)").find(trimmed)
            if (rangeMatch != null) {
                val start = rangeMatch.groupValues[1].toInt()
                val end = rangeMatch.groupValues[2].toInt()
                for (i in start..end) weeks.add(i)
            } else {
                // 匹配单数字
                val singleMatch = Regex("(\\d+)").find(trimmed)
                if (singleMatch != null) {
                    weeks.add(singleMatch.groupValues[1].toInt())
                }
            }
        }
        
        return if (weeks.isEmpty()) null else weeks
    }
    
    private fun parseWeekContent(content: String): Set<Int> {
        val weeks = mutableSetOf<Int>()
        // 分割逗号、换行符
        val parts = content.split(",", "，", " ", "\n", "\r")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            
            // 匹配范围 x-y
            val rangeMatch = Regex("(\\d+)[-~to至](\\d+)").find(trimmed)
            if (rangeMatch != null) {
                val start = rangeMatch.groupValues[1].toInt()
                val end = rangeMatch.groupValues[2].toInt()
                for (i in start..end) weeks.add(i)
            } else {
                // 匹配单数字
                val singleMatch = Regex("(\\d+)").find(trimmed)
                if (singleMatch != null) {
                    weeks.add(singleMatch.groupValues[1].toInt())
                }
            }
        }
        return weeks
    }
    
    private fun isLikelyTeacherName(str: String): Boolean {
        // 简单的启发式规则：长度短，不含楼/室/数字
        return str.length in 2..4 && !str.contains("楼") && !str.contains("室") && !str.contains("机房") && !str.matches(Regex(".*\\d.*"))
    }
    
    private fun parseTimeRange(label: String): Pair<String, String> {
        // 1. 优先尝试匹配 "第x-y节" 或 "第x,y,z节" 格式
        // 这样可以强制使用标准作息时间，即使文本中包含具体时间（如 "第1-2节 08:00-09:40"）也以节次为准
        if (label.contains("节")) {
            val numbers = Regex("\\d+").findAll(label).map { it.value.toInt() }.toList()
            if (numbers.isNotEmpty()) {
                val startSection = numbers.first()
                val endSection = numbers.last()
                return getSectionTimeRange(startSection, endSection)
            }
        }

        // 2. 尝试匹配标准时间格式 HH:MM-HH:MM
        val matcher = Regex("(\\d{1,2}:?\\d{2})\\s*[-~至到]\\s*(\\d{1,2}:?\\d{2})").find(label)
        if (matcher != null) {
            val start = normalizeTime(matcher.groupValues[1])
            val end = normalizeTime(matcher.groupValues[2])
            return start to end
        }
        
        return "08:00" to "09:40"
    }

    private fun getSectionTimeRange(start: Int, end: Int): Pair<String, String> {
        val times = mapOf(
            1 to ("08:00" to "08:45"),
            2 to ("08:50" to "09:35"),
            3 to ("09:50" to "10:35"),
            4 to ("10:40" to "11:25"),
            5 to ("11:30" to "12:15"),
            6 to ("14:00" to "14:45"),
            7 to ("14:50" to "15:35"),
            8 to ("15:50" to "16:35"),
            9 to ("16:40" to "17:25"),
            10 to ("17:30" to "18:15"),
            11 to ("19:00" to "19:45"),
            12 to ("19:50" to "20:35"),
            13 to ("20:40" to "21:25"),
            14 to ("21:30" to "22:15")
        )
        
        val startTime = times[start]?.first ?: "08:00"
        val endTime = times[end]?.second ?: times[start]?.second ?: "09:40"
        return startTime to endTime
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
     * 支持：1-7, 周一-周日, Monday-Sunday, Mon-Sun, 星期1-7
     */
    private fun parseDayOfWeek(dayStr: String): Int {
        val trimmed = dayStr.trim().lowercase()
        
        // 1. 纯数字 (1-7)
        if (trimmed.matches(Regex("\\d+"))) {
            val day = trimmed.toIntOrNull() ?: 0
            return if (day in 1..7) day else 0
        }
        
        // 2. 中文/英文关键词 (优先匹配明确的)
        if (trimmed.contains("一") || trimmed.contains("mon")) return 1
        if (trimmed.contains("二") || trimmed.contains("tue")) return 2
        if (trimmed.contains("三") || trimmed.contains("wed")) return 3
        if (trimmed.contains("四") || trimmed.contains("thu")) return 4
        if (trimmed.contains("五") || trimmed.contains("fri")) return 5
        if (trimmed.contains("六") || trimmed.contains("sat")) return 6
        if (trimmed.contains("日") || trimmed.contains("sun") || trimmed.contains("天")) return 7
        
        // 3. 混合数字 (e.g. 星期1, 周2)
        val numberMatch = Regex("\\d+").find(trimmed)
        if (numberMatch != null) {
            val day = numberMatch.value.toIntOrNull() ?: 0
            if (day in 1..7) return day
        }

        return 0 // 无法识别
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
    
    private data class RowDef(val periodName: String, val timeRange: String, val startHour: Int, val endHour: Int)

    fun exportCoursesToWeeklyExcel(courses: List<Course>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("课程表")
        
        // 设置列宽
        sheet.setColumnWidth(0, 6 * 256)  // 时段列 (上午/下午)
        sheet.setColumnWidth(1, 8 * 256)  // 节次列
        for (i in 2..8) {
            sheet.setColumnWidth(i, 18 * 256) // 星期列
        }

        // 使用支持中文的字体名称（SimSun是宋体的标准英文名称，在大多数系统中都可用）
        val chineseFontName = "SimSun"
        
        // 样式定义
        val titleStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFont(workbook.createFont().apply {
                fontName = chineseFontName
                fontHeightInPoints = 16.toShort()
                bold = true
            })
        }

        val headerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply {
                fontName = chineseFontName
                fontHeightInPoints = 11.toShort()
                bold = true
            })
        }

        val contentStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.TOP
            wrapText = true
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            setFont(workbook.createFont().apply {
                fontName = chineseFontName
                fontHeightInPoints = 10.toShort()
            })
        }
        
        val centerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            wrapText = true
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            setFont(workbook.createFont().apply {
                fontName = chineseFontName
                fontHeightInPoints = 10.toShort()
            })
        }

        // 1. 大标题
        val titleRow = sheet.createRow(0)
        titleRow.heightInPoints = 30f
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("课程表")
        titleCell.setCellStyle(titleStyle)
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8))

        // 2. 星期表头
        val headerRow = sheet.createRow(1)
        headerRow.heightInPoints = 25f
        val headers = listOf("", "", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        headers.forEachIndexed { index, text ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(text)
            cell.setCellStyle(headerStyle)
        }

        // 定义行结构
        val rows = listOf(
            RowDef("第1,2节", "08:00-09:35", 8, 9),
            RowDef("第3,4,5节", "09:50-12:15", 9, 13),
            RowDef("第6,7节", "14:00-15:35", 14, 15),
            RowDef("第8,9,10节", "15:50-18:15", 15, 18),
            RowDef("第11-14节", "19:00-22:45", 19, 23)
        )

        // 3. 填充数据
        var currentRowIndex = 2
        rows.forEach { rowDef ->
            val row = sheet.createRow(currentRowIndex)
            row.heightInPoints = 100f // 增加行高以容纳多行文本
            
            // 节次列
            val cell1 = row.createCell(1)
            cell1.setCellValue("${rowDef.periodName}\n") // 可以加上时间范围
            cell1.setCellStyle(centerStyle)

            // 课程数据
            for (day in 1..7) {
                val cell = row.createCell(day + 1)
                cell.setCellStyle(contentStyle)
                
                // 查找该时段该天的课程
                // 简单的逻辑：课程开始时间的小时数落在该行的 startHour 和 endHour 之间 (左闭右开，或者根据具体时间字符串比较)
                val dayCourses = courses.filter { course ->
                    course.dayOfWeek == day && isCourseInRow(course.startTime, rowDef)
                }
                
                if (dayCourses.isNotEmpty()) {
                    val content = dayCourses.joinToString("\n\n") { course ->
                        buildString {
                            append(course.courseName)
                            if (!course.teacherName.isNullOrEmpty()) append("\n${course.teacherName}")
                            // 显示周数：优先使用weeks字段，否则使用startWeek-endWeek
                            val weeksDisplay = if (!course.weeks.isNullOrBlank()) {
                                val weeks = course.weeks.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
                                val formatted = WeekDisplayUtils.formatWeeks(weeks)
                                "[$formatted]"
                            } else {
                                val formatted = WeekDisplayUtils.formatWeekRange(course.startWeek, course.endWeek)
                                "[$formatted]"
                            }
                            append("\n$weeksDisplay")
                            if (!course.location.isNullOrEmpty()) append("\n${course.location}")
                            // append("\n${course.startTime}-${course.endTime}")
                        }
                    }
                    cell.setCellValue(content)
                }
            }
            currentRowIndex++
        }

        // 4. 处理左侧合并 (上午/下午/晚上)
        // 上午: Row 2, 3 (Index 2, 3)
        val mergeCol0Style = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            wrapText = true
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            setFont(workbook.createFont().apply {
                fontName = chineseFontName
                fontHeightInPoints = 12.toShort()
                bold = true
            })
        }

        // 上午
        val amRow = sheet.getRow(2)
        val amCell = amRow.createCell(0)
        amCell.setCellValue("上午")
        amCell.setCellStyle(mergeCol0Style)
        // 补全边框
        sheet.getRow(3).createCell(0).setCellStyle(mergeCol0Style)
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(2, 3, 0, 0))

        // 下午
        val pmRow = sheet.getRow(4)
        val pmCell = pmRow.createCell(0)
        pmCell.setCellValue("下午")
        pmCell.setCellStyle(mergeCol0Style)
        sheet.getRow(5).createCell(0).setCellStyle(mergeCol0Style)
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(4, 5, 0, 0))

        // 晚上
        val nightRow = sheet.getRow(6)
        val nightCell = nightRow.createCell(0)
        nightCell.setCellValue("晚上")
        nightCell.setCellStyle(mergeCol0Style)
        // 晚上只有一行，不需要合并，或者如果以后扩展了行数再合并

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        return outputStream.toByteArray()
    }

    private fun isCourseInRow(startTime: String, rowDef: RowDef): Boolean {
        // 简单的基于小时的判断
        val startHour = startTime.split(":").firstOrNull()?.toIntOrNull() ?: 0
        // 特殊处理：第3,4,5节 从 9:50 开始，所以 9点多的课应该算在这里，而不是第1,2节(8点多)
        // 第1,2节: 8:xx - 9:xx
        // 第3,4,5节: 9:50 - 12:xx
        
        // 细化判断逻辑
        val startMinutes = parseTimeToMinutes(startTime)
        val rowStartMinutes = parseTimeToMinutes(rowDef.timeRange.split("-")[0])
        val rowEndMinutes = parseTimeToMinutes(rowDef.timeRange.split("-")[1])
        
        // 允许一定的误差，或者只要开始时间在范围内
        // 比如课程 10:00 开始，Row2 是 09:50-12:15，Row1 是 08:00-09:35
        // 我们可以判断 startMinutes 是否 >= rowStartMinutes (with some buffer) 并且 < nextRowStart
        
        // 简化逻辑：
        // Row 1: < 09:40
        // Row 2: 09:40 <= x < 13:00
        // Row 3: 13:00 <= x < 15:40
        // Row 4: 15:40 <= x < 18:30
        // Row 5: >= 18:30
        
        return when(rowDef.periodName) {
            "第1,2节" -> startMinutes < parseTimeToMinutes("09:40")
            "第3,4,5节" -> startMinutes >= parseTimeToMinutes("09:40") && startMinutes < parseTimeToMinutes("13:00")
            "第6,7节" -> startMinutes >= parseTimeToMinutes("13:00") && startMinutes < parseTimeToMinutes("15:40")
            "第8,9,10节" -> startMinutes >= parseTimeToMinutes("15:40") && startMinutes < parseTimeToMinutes("18:30")
            "第11-14节" -> startMinutes >= parseTimeToMinutes("18:30")
            else -> false
        }
    }

    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size < 2) return 0
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}

