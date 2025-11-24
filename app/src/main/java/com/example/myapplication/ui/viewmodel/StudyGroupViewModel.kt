package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.StudyGroup
import com.example.myapplication.data.repository.StudyGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyGroupViewModel(private val repository: StudyGroupRepository) : ViewModel() {
    private val _groups = MutableStateFlow<List<StudyGroup>>(emptyList())
    val groups: StateFlow<List<StudyGroup>> = _groups.asStateFlow()
    
    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0
    
    init {
        loadGroups()
    }
    
    private fun loadGroups() {
        viewModelScope.launch {
            repository.getGroupsByUser(userId).collect { groupList ->
                _groups.value = groupList
            }
        }
    }
    
    fun insertGroup(group: StudyGroup) {
        viewModelScope.launch {
            repository.insertGroup(group)
        }
    }
    
    fun updateGroup(group: StudyGroup) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }
    
    fun deleteGroup(group: StudyGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }
}

class StudyGroupViewModelFactory(private val repository: StudyGroupRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyGroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyGroupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

