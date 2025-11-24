package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.GroupFile
import com.example.myapplication.data.model.FileType
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupFileDao {
    @Query("SELECT * FROM group_files WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getFilesByGroup(groupId: Int): Flow<List<GroupFile>>
    
    @Query("SELECT * FROM group_files WHERE groupId = :groupId AND fileType = :fileType ORDER BY createdAt DESC")
    fun getFilesByGroupAndType(groupId: Int, fileType: FileType): Flow<List<GroupFile>>
    
    @Query("SELECT * FROM group_files WHERE groupId = :groupId AND fileName LIKE :query ORDER BY createdAt DESC")
    fun searchFiles(groupId: Int, query: String): Flow<List<GroupFile>>
    
    @Query("SELECT * FROM group_files WHERE fileId = :fileId")
    suspend fun getFileById(fileId: Int): GroupFile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: GroupFile): Long
    
    @Delete
    suspend fun deleteFile(file: GroupFile)
    
    @Query("DELETE FROM group_files WHERE fileId = :fileId")
    suspend fun deleteFileById(fileId: Int)
}

