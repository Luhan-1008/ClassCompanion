package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Assignment
import com.example.myapplication.data.model.AssignmentStatus
import com.example.myapplication.data.repository.AssignmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssignmentViewModel(private val repository: AssignmentRepository) : ViewModel() {
    private val _assignments = MutableStateFlow<List<Assignment>>(emptyList())
    val assignments: StateFlow<List<Assignment>> = _assignments.asStateFlow()
    
    private val _selectedAssignment = MutableStateFlow<Assignment?>(null)
    val selectedAssignment: StateFlow<Assignment?> = _selectedAssignment.asStateFlow()
    
    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0
    
    init {
        loadAssignments()
    }
    
    private fun loadAssignments() {
        viewModelScope.launch {
            repository.getAssignmentsByUser(userId).collect { assignmentList ->
                _assignments.value = assignmentList
            }
        }
    }
    
    fun getAssignmentsByStatus(status: AssignmentStatus) {
        viewModelScope.launch {
            repository.getAssignmentsByStatus(userId, status).collect { assignmentList ->
                _assignments.value = assignmentList
            }
        }
    }
    
    fun getUpcomingAssignments() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000 // 未来7天
            repository.getUpcomingAssignments(userId, timestamp).collect { assignmentList ->
                _assignments.value = assignmentList
            }
        }
    }
    
    fun selectAssignment(assignment: Assignment?) {
        _selectedAssignment.value = assignment
    }
    
    fun insertAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.insertAssignment(assignment)
        }
    }
    
    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.updateAssignment(assignment)
        }
    }
    
    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.deleteAssignment(assignment)
        }
    }
    
    fun updateAssignmentStatus(assignmentId: Int, status: AssignmentStatus) {
        viewModelScope.launch {
            repository.updateAssignmentStatus(assignmentId, status)
        }
    }

    fun updateAssignmentProgress(assignmentId: Int, progress: Int) {
        viewModelScope.launch {
            repository.updateAssignmentProgress(assignmentId, progress)
        }
    }
}

class AssignmentViewModelFactory(private val repository: AssignmentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssignmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssignmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

