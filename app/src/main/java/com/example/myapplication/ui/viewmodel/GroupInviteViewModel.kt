package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GroupInvite
import com.example.myapplication.data.repository.GroupInviteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class GroupInviteViewModel(
    private val inviteRepository: GroupInviteRepository,
    private val groupId: Int
) : ViewModel() {
    private val _invites = MutableStateFlow<List<GroupInvite>>(emptyList())
    val invites: StateFlow<List<GroupInvite>> = _invites.asStateFlow()
    
    private val _currentInvite = MutableStateFlow<GroupInvite?>(null)
    val currentInvite: StateFlow<GroupInvite?> = _currentInvite.asStateFlow()
    
    init {
        loadInvites()
    }
    
    private fun loadInvites() {
        viewModelScope.launch {
            inviteRepository.getInvitesByGroup(groupId).collect { inviteList ->
                _invites.value = inviteList.filter { it.inviteId != _currentInvite.value?.inviteId }
                // 如果没有当前邀请，使用最新的一个
                if (_currentInvite.value == null && inviteList.isNotEmpty()) {
                    _currentInvite.value = inviteList.first()
                }
            }
        }
    }
    
    fun createNewInvite(creatorId: Int) {
        viewModelScope.launch {
            val inviteCode = generateInviteCode()
            val invite = GroupInvite(
                groupId = groupId,
                creatorId = creatorId,
                inviteCode = inviteCode,
                maxUses = null, // 默认无限制
                expiresAt = null // 默认永不过期
            )
            val inviteId = inviteRepository.insertInvite(invite)
            // 重新加载以获取新创建的邀请
            loadInvites()
        }
    }
    
    fun deleteInvite(invite: GroupInvite) {
        viewModelScope.launch {
            inviteRepository.deleteInvite(invite)
            if (_currentInvite.value?.inviteId == invite.inviteId) {
                _currentInvite.value = null
            }
        }
    }
    
    private fun generateInviteCode(): String {
        // 生成6位随机邀请码（字母+数字）
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}

class GroupInviteViewModelFactory(
    private val inviteRepository: GroupInviteRepository,
    private val groupId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupInviteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupInviteViewModel(inviteRepository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

