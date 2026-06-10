package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GroupAnnouncement
import com.example.myapplication.data.repository.GroupAnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupAnnouncementViewModel(
    private val announcementRepository: GroupAnnouncementRepository,
    private val groupId: Int
) : ViewModel() {
    private val _announcements = MutableStateFlow<List<GroupAnnouncement>>(emptyList())
    val announcements: StateFlow<List<GroupAnnouncement>> = _announcements.asStateFlow()
    
    init {
        loadAnnouncements()
    }
    
    private fun loadAnnouncements() {
        viewModelScope.launch {
            announcementRepository.getAnnouncementsByGroup(groupId).collect { announcementList ->
                _announcements.value = announcementList
            }
        }
    }
    
    fun createAnnouncement(announcement: GroupAnnouncement) {
        viewModelScope.launch {
            announcementRepository.insertAnnouncement(announcement)
        }
    }
    
    fun updateAnnouncement(announcement: GroupAnnouncement) {
        viewModelScope.launch {
            announcementRepository.updateAnnouncement(announcement)
        }
    }
    
    fun deleteAnnouncement(announcement: GroupAnnouncement) {
        viewModelScope.launch {
            announcementRepository.deleteAnnouncement(announcement)
        }
    }
    
    fun togglePin(announcement: GroupAnnouncement) {
        viewModelScope.launch {
            announcementRepository.updatePinStatus(announcement.announcementId, !announcement.isPinned)
        }
    }
}

class GroupAnnouncementViewModelFactory(
    private val announcementRepository: GroupAnnouncementRepository,
    private val groupId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupAnnouncementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupAnnouncementViewModel(announcementRepository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

