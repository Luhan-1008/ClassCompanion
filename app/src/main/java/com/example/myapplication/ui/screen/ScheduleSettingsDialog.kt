package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ScheduleSettings
import com.example.myapplication.utils.ColorSchemeUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsDialog(
    settings: ScheduleSettings,
    onDismiss: () -> Unit,
    onSave: (ScheduleSettings) -> Unit
) {
    var scheduleName by remember { mutableStateOf(settings.scheduleName) }
    var totalWeeks by remember { mutableStateOf(settings.totalWeeks.toString()) }
    var startDate by remember { mutableStateOf(settings.startDate) }
    var showTeacher by remember { mutableStateOf(settings.showTeacher) }
    var showLocation by remember { mutableStateOf(settings.showLocation) }
    var showSaturday by remember { mutableStateOf(settings.showSaturday) }
    var showSunday by remember { mutableStateOf(settings.showSunday) }
    var selectedColorScheme by remember { mutableStateOf(settings.colorScheme) }
    
    // 从 ColorSchemeUtils 获取所有配色方案，确保与代码中的颜色列表一致
    val colorSchemes = remember { ColorSchemeUtils.getAllColorSchemes().toList() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "课表设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 基础信息
                Text(
                    text = "基础信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = scheduleName,
                    onValueChange = { scheduleName = it },
                    label = { Text("课表名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = totalWeeks,
                    onValueChange = { if (it.all { char -> char.isDigit() }) totalWeeks = it },
                    label = { Text("共多少周") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("开学日期 (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("例如：2024-09-01") }
                )
                
                Divider()
                
                // 课程格子显示设置
                Text(
                    text = "课表显示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                SwitchItem(
                    label = "显示教师",
                    checked = showTeacher,
                    onCheckedChange = { showTeacher = it }
                )
                
                SwitchItem(
                    label = "显示教室",
                    checked = showLocation,
                    onCheckedChange = { showLocation = it }
                )
                
                SwitchItem(
                    label = "显示周六",
                    checked = showSaturday,
                    onCheckedChange = { showSaturday = it }
                )
                
                SwitchItem(
                    label = "显示周日",
                    checked = showSunday,
                    onCheckedChange = { showSunday = it }
                )
                
                Divider()
                
                // 课程色系
                Text(
                    text = "课程色系",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                colorSchemes.forEach { (schemeName, colors) ->
                    ColorSchemeOption(
                        name = when(schemeName) {
                            "default" -> "默认"
                            "blue" -> "蓝色系"
                            "green" -> "绿色系"
                            "purple" -> "紫色系"
                            "red" -> "红色系"
                            "pastel" -> "浅色系"
                            else -> schemeName
                        },
                        colors = colors,
                        isSelected = selectedColorScheme == schemeName,
                        onClick = { selectedColorScheme = schemeName }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weeks = totalWeeks.toIntOrNull() ?: 16
                    onSave(
                        ScheduleSettings(
                            scheduleName = scheduleName,
                            totalWeeks = weeks,
                            startDate = startDate,
                            showTeacher = showTeacher,
                            showLocation = showLocation,
                            showSaturday = showSaturday,
                            showSunday = showSunday,
                            colorScheme = selectedColorScheme
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ColorSchemeOption(
    name: String,
    colors: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.take(7).forEach { colorStr ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(colorStr))
                                } catch (e: Exception) {
                                    Color.Gray
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
