package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.GroupFileDao
import com.example.myapplication.data.model.GroupFile
import com.example.myapplication.data.model.FileType
import kotlinx.coroutines.flow.Flow

class GroupFileRepository(private val groupFileDao: GroupFileDao) {
    fun getFilesByGroup(groupId: Int): Flow<List<GroupFile>> =
        groupFileDao.getFilesByGroup(groupId)
    
    fun getFilesByGroupAndType(groupId: Int, fileType: FileType): Flow<List<GroupFile>> =
        groupFileDao.getFilesByGroupAndType(groupId, fileType)
    
    fun searchFiles(groupId: Int, query: String): Flow<List<GroupFile>> =
        groupFileDao.searchFiles(groupId, "%$query%")
    
    suspend fun getFileById(fileId: Int): GroupFile? =
        groupFileDao.getFileById(fileId)
    
    suspend fun insertFile(file: GroupFile): Long =
        groupFileDao.insertFile(file)
    
    suspend fun deleteFile(file: GroupFile) =
        groupFileDao.deleteFile(file)
    
    suspend fun deleteFileById(fileId: Int) =
        groupFileDao.deleteFileById(fileId)
}

