package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.RemoteUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.myapplication.session.CurrentSession

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class UserViewModel(
    private val repository: UserRepository,
    private val remoteRepository: RemoteUserRepository? = null
) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _loginResult = MutableStateFlow<AuthResult?>(null)
    val loginResult: StateFlow<AuthResult?> = _loginResult.asStateFlow()
    
    private val _registerResult = MutableStateFlow<AuthResult?>(null)
    val registerResult: StateFlow<AuthResult?> = _registerResult.asStateFlow()
    
    fun login(studentId: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                if (remoteRepository != null) {
                    val resp = remoteRepository.login(username, password)
                    // 保存会话信息（token 和 userId）
                    if (resp.token.isNotBlank()) {
                        CurrentSession.token = resp.token
                        CurrentSession.userId = resp.userId
                        // 如果需要本地缓存用户，可尝试查询或插入本地用户
                        val localUser = repository.login(studentId, username, password)
                        _currentUser.value = localUser ?: User(username = username, password = password, studentId = studentId)
                        _loginResult.value = AuthResult.Success
                        return@launch
                    } else {
                        _loginResult.value = AuthResult.Error("远端登录失败")
                        return@launch
                    }
                }
                // 回退到本地 Room 登录
                val user = repository.login(studentId, username, password)
                if (user != null) {
                    // 设置会话信息
                    CurrentSession.userId = user.userId.toLong()
                    _currentUser.value = user
                    _loginResult.value = AuthResult.Success
                } else {
                    _loginResult.value = AuthResult.Error("用户名或密码错误")
                }
            } catch (e: Exception) {
                _loginResult.value = AuthResult.Error("登录失败: ${e.message}")
            }
        }
    }
    
    fun register(user: User) {
        viewModelScope.launch {
            try {
                if (remoteRepository != null) {
                    val resp = remoteRepository.register(
                        username = user.username,
                        password = user.password,
                        email = user.email,
                        realName = user.realName
                    )
                    if (resp.success) {
                        // 远端成功后本地插入（若未存在）以便离线使用
                        if (!repository.usernameExists(user.username)) {
                            repository.register(user)
                        }
                        _registerResult.value = AuthResult.Success
                        return@launch
                    } else {
                        _registerResult.value = AuthResult.Error(resp.message)
                        return@launch
                    }
                }
                // 回退到本地注册
                val exists = repository.usernameExists(user.username)
                if (exists) {
                    _registerResult.value = AuthResult.Error("用户名已存在")
                    return@launch
                }
                val userId = repository.register(user)
                if (userId > 0) {
                    // 注册成功后自动登录，设置会话信息
                    CurrentSession.userId = userId.toLong()
                    _currentUser.value = user.copy(userId = userId.toInt())
                    _registerResult.value = AuthResult.Success
                } else {
                    _registerResult.value = AuthResult.Error("注册失败")
                }
            } catch (e: Exception) {
                _registerResult.value = AuthResult.Error("注册失败: ${e.message}")
            }
        }
    }
    
    fun clearLoginResult() {
        _loginResult.value = null
    }
    
    fun clearRegisterResult() {
        _registerResult.value = null
    }
    
    fun logout() {
        _currentUser.value = null
        CurrentSession.userId = null
        CurrentSession.token = null
    }
    
    fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = CurrentSession.userIdInt
            if (userId != null && userId > 0) {
                val user = repository.getUserById(userId)
                _currentUser.value = user
            }
        }
    }
}

class UserViewModelFactory(
    private val repository: UserRepository,
    private val remoteRepository: RemoteUserRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

