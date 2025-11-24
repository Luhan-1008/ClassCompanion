package com.example.myapplication.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.FileType
import com.example.myapplication.data.model.GroupFile
import com.example.myapplication.data.repository.GroupFileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class GroupFileViewModel(
    private val fileRepository: GroupFileRepository,
    private val groupId: Int
) : ViewModel() {
    private val _files = MutableStateFlow<List<GroupFile>>(emptyList())
    val files: StateFlow<List<GroupFile>> = _files.asStateFlow()
    
    init {
        loadFiles()
    }
    
    private fun loadFiles() {
        viewModelScope.launch {
            fileRepository.getFilesByGroup(groupId).collect { fileList ->
                _files.value = fileList
            }
        }
    }
    
    fun uploadFile(file: GroupFile) {
        viewModelScope.launch {
            fileRepository.insertFile(file)
        }
    }
    
    fun deleteFile(file: GroupFile) {
        viewModelScope.launch {
            // 删除本地文件
            try {
                File(file.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // 删除数据库记录
            fileRepository.deleteFile(file)
        }
    }
    
    suspend fun downloadFile(context: Context, file: GroupFile) {
        try {
            val fileUri = try {
                // 尝试使用FileProvider（推荐方式）
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(file.filePath)
                )
            } catch (e: Exception) {
                // 降级到直接使用Uri
                android.net.Uri.fromFile(File(file.filePath))
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, getMimeType(file.fileName))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            else -> "*/*"
        }
    }
    
    fun getFilesByType(fileType: FileType) {
        viewModelScope.launch {
            fileRepository.getFilesByGroupAndType(groupId, fileType).collect { fileList ->
                _files.value = fileList
            }
        }
    }
    
    fun searchFiles(query: String) {
        viewModelScope.launch {
            fileRepository.searchFiles(groupId, query).collect { fileList ->
                _files.value = fileList
            }
        }
    }
}

class GroupFileViewModelFactory(
    private val fileRepository: GroupFileRepository,
    private val groupId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupFileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupFileViewModel(fileRepository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

