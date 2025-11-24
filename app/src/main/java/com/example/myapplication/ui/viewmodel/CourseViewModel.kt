package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel(private val repository: CourseRepository) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()
    
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _insertSuccess = MutableStateFlow<Boolean>(false)
    val insertSuccess: StateFlow<Boolean> = _insertSuccess.asStateFlow()
    
    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0
    
    init {
        loadCourses()
    }
    
    private fun loadCourses() {
        viewModelScope.launch {
            repository.getCoursesByUser(userId).collect { courseList ->
                _courses.value = courseList
            }
        }
    }
    
    fun getCoursesByDay(dayOfWeek: Int) {
        viewModelScope.launch {
            repository.getCoursesByDay(userId, dayOfWeek).collect { courseList ->
                _courses.value = courseList
            }
        }
    }
    
    fun selectCourse(course: Course?) {
        _selectedCourse.value = course
    }
    
    fun insertCourse(course: Course) {
        viewModelScope.launch {
            try {
                // 检查用户是否已登录
                val currentUserId = com.example.myapplication.session.CurrentSession.userIdInt
                if (currentUserId == null || currentUserId == 0) {
                    _errorMessage.value = "请先登录后再添加课程"
                    return@launch
                }
                
                // 验证 userId 是否有效（外键约束）
                if (course.userId != currentUserId) {
                    _errorMessage.value = "用户ID不匹配，请重新登录"
                    return@launch
                }
                
                repository.insertCourse(course)
                _errorMessage.value = null
                _insertSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "添加课程失败: ${e.message}"
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
    
    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
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
                
                var successCount = 0
                var failCount = 0
                
                courses.forEach { course ->
                    try {
                        // 确保userId正确
                        val courseWithUserId = course.copy(userId = currentUserId)
                        repository.insertCourse(courseWithUserId)
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                        e.printStackTrace()
                    }
                }
                
                if (failCount == 0) {
                    _errorMessage.value = null
                    _insertSuccess.value = true
                } else {
                    _errorMessage.value = "成功导入 $successCount 门课程，失败 $failCount 门"
                }
            } catch (e: Exception) {
                _errorMessage.value = "导入课程失败: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}

class CourseViewModelFactory(private val repository: CourseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

