package com.example.myapplication.utils

import com.example.myapplication.data.model.ScheduleSettings
import java.text.SimpleDateFormat
import java.util.*

object ScheduleDateUtils {
    /**
     * 根据设置中的开学日期计算指定日期是第几周
     */
    fun calculateWeekNumber(date: Calendar, settings: ScheduleSettings): Int {
        if (settings.startDate.isBlank()) {
            // 如果没有设置开学日期，使用默认逻辑
            return calculateAcademicWeekDefault(date)
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = try {
            sdf.parse(settings.startDate) ?: return calculateAcademicWeekDefault(date)
        } catch (e: Exception) {
            return calculateAcademicWeekDefault(date)
        }
        
        val academicStart = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 找到开学日期所在周的周一
        while (academicStart.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            academicStart.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        val targetDate = date.clone() as Calendar
        targetDate.set(Calendar.HOUR_OF_DAY, 0)
        targetDate.set(Calendar.MINUTE, 0)
        targetDate.set(Calendar.SECOND, 0)
        targetDate.set(Calendar.MILLISECOND, 0)
        
        // 找到目标日期所在周的周一
        while (targetDate.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            targetDate.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        val diffMillis = targetDate.timeInMillis - academicStart.timeInMillis
        val weekMillis = 7L * 24 * 60 * 60 * 1000
        val weekNumber = (diffMillis / weekMillis).toInt() + 1
        
        return maxOf(1, weekNumber)
    }
    
    /**
     * 根据设置中的开学日期和总周数，计算指定周数的开始日期
     */
    fun getWeekStartDate(targetWeek: Int, settings: ScheduleSettings): Calendar {
        if (settings.startDate.isBlank()) {
            // 如果没有设置开学日期，使用默认逻辑
            return getWeekStartDateDefault(targetWeek)
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = try {
            sdf.parse(settings.startDate) ?: return getWeekStartDateDefault(targetWeek)
        } catch (e: Exception) {
            return getWeekStartDateDefault(targetWeek)
        }
        
        val academicStart = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 找到开学日期所在周的周一
        while (academicStart.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            academicStart.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        val targetDate = academicStart.clone() as Calendar
        targetDate.add(Calendar.WEEK_OF_YEAR, targetWeek - 1)
        return targetDate
    }
    
    /**
     * 获取当前周数（使用设置）
     */
    fun getCurrentWeek(settings: ScheduleSettings): Int {
        return calculateWeekNumber(Calendar.getInstance(), settings)
    }
    
    /**
     * 默认的周数计算逻辑（当没有设置开学日期时使用）
     */
    private fun calculateAcademicWeekDefault(date: Calendar): Int {
        val referenceYear = if (date.get(Calendar.MONTH) >= Calendar.SEPTEMBER) {
            date.get(Calendar.YEAR)
        } else {
            date.get(Calendar.YEAR) - 1
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
        val diffMillis = date.timeInMillis - academicStart.timeInMillis
        val weekMillis = 7L * 24 * 60 * 60 * 1000
        return maxOf(1, (diffMillis / weekMillis).toInt() + 1)
    }
    
    /**
     * 默认的周开始日期计算逻辑（当没有设置开学日期时使用）
     */
    private fun getWeekStartDateDefault(targetWeek: Int): Calendar {
        val now = Calendar.getInstance()
        val referenceYear = if (now.get(Calendar.MONTH) >= Calendar.SEPTEMBER) {
            now.get(Calendar.YEAR)
        } else {
            now.get(Calendar.YEAR) - 1
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
        val targetDate = academicStart.clone() as Calendar
        targetDate.add(Calendar.WEEK_OF_YEAR, targetWeek - 1)
        return targetDate
    }
}

