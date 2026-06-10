package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.service.PreciseReminderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel(
    private val repository: CourseRepository,
    private val userRepository: com.example.myapplication.data.repository.UserRepository? = null
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()
    
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _insertSuccess = MutableStateFlow<Boolean>(false)
    val insertSuccess: StateFlow<Boolean> = _insertSuccess.asStateFlow()
    
    private val _lastImportedCourseIds = MutableStateFlow<List<Int>>(emptyList())
    val lastImportedCourseIds: StateFlow<List<Int>> = _lastImportedCourseIds.asStateFlow()

    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0
    
    init {
        loadCourses()
    }
    
    fun loadAllCourses() {
        loadCourses()
    }
    
    private fun loadCourses() {
        viewModelScope.launch {
            if (userId > 0) {
                try {
                    repository.syncCourses(userId)
                } catch (e: Exception) {
                    _errorMessage.value = "同步课程失败: ${e.message}"
                }
            }
            repository.getCoursesByUser(userId).collect { courseList ->
                _courses.value = courseList
            }
        }
    }
    
    fun getCoursesByDay(dayOfWeek: Int) {
        viewModelScope.launch {
            if (userId > 0) {
                try {
                    repository.syncCourses(userId)
                } catch (_: Exception) {
                }
            }
            repository.getCoursesByDay(userId, dayOfWeek).collect { courseList ->
                _courses.value = courseList
            }
        }
    }
    
    fun selectCourse(course: Course?) {
        _selectedCourse.value = course
    }
    
    fun insertCourse(course: Course, context: Context? = null) {
        viewModelScope.launch {
            try {
                val currentUserId = com.example.myapplication.session.CurrentSession.userIdInt
                if (currentUserId == null || currentUserId == 0) {
                    _errorMessage.value = "请先登录后再添加课程"
                    return@launch
                }
                
                if (userRepository != null) {
                    val user = userRepository.getUserById(currentUserId)
                    if (user == null) {
                        _errorMessage.value = "用户不存在，请重新登录"
                        return@launch
                    }
                }
                
                if (course.userId != currentUserId) {
                    _errorMessage.value = "用户ID不匹配，请重新登录"
                    return@launch
                }
                
                val courseWithCorrectUserId = course.copy(userId = currentUserId)
                
                val courseId = repository.insertCourse(courseWithCorrectUserId)
                val insertedCourse = repository.getCourseById(courseId.toInt())
                
                if (context != null && insertedCourse != null) {
                    PreciseReminderService.scheduleCourseReminders(context, insertedCourse)
                }
                
                _errorMessage.value = null
                _insertSuccess.value = true
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误"
                if (errorMsg.contains("FOREIGN KEY")) {
                    _errorMessage.value = "添加课程失败: 用户不存在，请重新登录"
                } else {
                    _errorMessage.value = "添加课程失败: $errorMsg"
                }
                _insertSuccess.value = false
                e.printStackTrace()
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun resetInsertSuccess() {
        _insertSuccess.value = false
    }
    
    fun updateCourse(course: Course, context: Context? = null) {
        viewModelScope.launch {
            repository.updateCourse(course)
            if (context != null) {
                PreciseReminderService.cancelCourseReminder(context, course.courseId)
                PreciseReminderService.scheduleCourseReminders(context, course)
            }
        }
    }
    
    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }
    
    fun importCourses(courses: List<Course>) {
        viewModelScope.launch {
            try {
                val currentUserId = com.example.myapplication.session.CurrentSession.userIdInt
                if (currentUserId == null || currentUserId == 0) {
                    _errorMessage.value = "请先登录后再导入课程"
                    return@launch
                }
                
                if (userRepository != null) {
                    val user = userRepository.getUserById(currentUserId)
                    if (user == null) {
                        _errorMessage.value = "用户不存在，请重新登录"
                        return@launch
                    }
                }
                
                if (courses.isEmpty()) {
                    _errorMessage.value = "没有可导入的课程"
                    return@launch
                }
                
                var successCount = 0
                var failCount = 0
                val importedIds = mutableListOf<Int>()
                val errorDetails = mutableListOf<String>()
                
                courses.forEachIndexed { index, course ->
                    try {
                        if (course.courseName.isBlank()) {
                            errorDetails.add("第${index + 1}门课程：课程名称为空")
                            failCount++
                            return@forEachIndexed
                        }
                        
                        if (course.dayOfWeek !in 1..7) {
                            errorDetails.add("第${index + 1}门课程：星期无效 (${course.dayOfWeek})")
                            failCount++
                            return@forEachIndexed
                        }
                        
                        val courseWithUserId = course.copy(
                            userId = currentUserId,
                            reminderEnabled = course.reminderEnabled,
                            reminderMinutes = course.reminderMinutes
                        )
                        
                        val id = repository.insertCourse(courseWithUserId)
                        importedIds.add(id.toInt())
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                        val errorMsg = e.message ?: "未知错误"
                        val detailMsg = if (errorMsg.contains("FOREIGN KEY")) {
                            "第${index + 1}门课程 (${course.courseName}): 用户不存在，请重新登录"
                        } else {
                            "第${index + 1}门课程 (${course.courseName}): $errorMsg"
                        }
                        errorDetails.add(detailMsg)
                        e.printStackTrace()
                    }
                }
                
                if (failCount == 0) {
                    _errorMessage.value = null
                    _insertSuccess.value = true
                    _lastImportedCourseIds.value = importedIds
                } else {
                    val errorSummary = if (errorDetails.size <= 3) {
                        "成功导入 $successCount 门课程，失败 $failCount 门\n${errorDetails.joinToString("\n")}"
                    } else {
                        "成功导入 $successCount 门课程，失败 $failCount 门\n${errorDetails.take(3).joinToString("\n")}\n..."
                    }
                    _errorMessage.value = errorSummary
                    if (successCount > 0) {
                        _insertSuccess.value = true
                        _lastImportedCourseIds.value = importedIds
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "导入课程失败: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun undoLastImport() {
        viewModelScope.launch {
            val idsToDelete = _lastImportedCourseIds.value
            if (idsToDelete.isNotEmpty()) {
                try {
                    idsToDelete.forEach { id ->
                        repository.deleteCourseById(id)
                    }
                    _lastImportedCourseIds.value = emptyList()
                    _errorMessage.value = "已撤销导入"
                } catch (e: Exception) {
                    _errorMessage.value = "撤销失败: ${e.message}"
                }
            }
        }
    }
    
    fun clearAllCourses() {
        viewModelScope.launch {
            try {
                val currentUserId = com.example.myapplication.session.CurrentSession.userIdInt
                if (currentUserId != null && currentUserId != 0) {
                    repository.deleteAllCourses(currentUserId)
                    _errorMessage.value = "已清空所有课程"
                }
            } catch (e: Exception) {
                _errorMessage.value = "清空课程失败: ${e.message}"
            }
        }
    }
}

class CourseViewModelFactory(
    private val repository: CourseRepository,
    private val userRepository: com.example.myapplication.data.repository.UserRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseViewModel(repository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
