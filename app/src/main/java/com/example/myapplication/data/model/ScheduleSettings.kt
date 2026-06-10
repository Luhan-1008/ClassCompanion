package com.example.myapplication.data.model

data class ScheduleSettings(
    val scheduleName: String = "我的课表",
    val totalWeeks: Int = 16,
    val startDate: String = "", // ��ʽ��yyyy-MM-dd
    val showTeacher: Boolean = false, // ��ʾ��ʦ����Ӱ���ܿγ̱�
    val showLocation: Boolean = true,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true,
    val colorScheme: String = "default" // default, blue, green, purple, red, pastel
)

