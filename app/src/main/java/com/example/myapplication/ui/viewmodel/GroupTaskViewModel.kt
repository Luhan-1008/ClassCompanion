package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GroupTask
import com.example.myapplication.data.model.TaskStatus
import com.example.myapplication.data.repository.GroupTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupTaskViewModel(
    private val taskRepository: GroupTaskRepository,
    private val groupId: Int
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<GroupTask>>(emptyList())
    val tasks: StateFlow<List<GroupTask>> = _tasks.asStateFlow()
    
    init {
        loadTasks()
    }
    
    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getTasksByGroup(groupId).collect { taskList ->
                _tasks.value = taskList
            }
        }
    }
    
    fun createTask(task: GroupTask) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
        }
    }
    
    fun updateTask(task: GroupTask) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }
    
    fun deleteTask(task: GroupTask) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
    
    fun updateTaskStatus(taskId: Int, status: TaskStatus, completedAt: Long?) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, status, completedAt)
        }
    }
}

class GroupTaskViewModelFactory(
    private val taskRepository: GroupTaskRepository,
    private val groupId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupTaskViewModel(taskRepository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

