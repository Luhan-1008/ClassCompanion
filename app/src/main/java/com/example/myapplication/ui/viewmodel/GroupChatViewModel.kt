package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GroupMessage
import com.example.myapplication.data.repository.GroupMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupChatViewModel(
    private val messageRepository: GroupMessageRepository,
    private val groupId: Int
) : ViewModel() {
    private val _messages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val messages: StateFlow<List<GroupMessage>> = _messages.asStateFlow()
    
    init {
        loadMessages()
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getMessagesByGroup(groupId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }
    
    fun sendMessage(message: GroupMessage) {
        viewModelScope.launch {
            messageRepository.insertMessage(message)
        }
    }
    
    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }
}

class GroupChatViewModelFactory(
    private val messageRepository: GroupMessageRepository,
    private val groupId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupChatViewModel(messageRepository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

