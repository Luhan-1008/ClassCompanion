package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.dao.GroupInviteDao
import com.example.myapplication.data.model.GroupInvite
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.dto.GroupInviteCreateRequest
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class GroupInviteRepository(private val inviteDao: GroupInviteDao) {
    private val api = RetrofitClient.api
    
    fun getInvitesByGroup(groupId: Int): Flow<List<GroupInvite>> =
        inviteDao.getInvitesByGroup(groupId)
    
    suspend fun getInviteByCode(inviteCode: String): GroupInvite? {
        // 先从服务器查询
        return try {
            val response = api.getInviteByCode(inviteCode)
            Log.d("GroupInviteRepository", "从服务器获取到邀请码: $inviteCode")
            
            // 将服务器响应转换为本地实体
            val createdAt = try {
                response.createdAt?.let { dateStr ->
                    try {
                        // 尝试解析ISO 8601格式的日期时间字符串
                        java.time.LocalDateTime.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        // 如果解析失败，使用当前时间
                        System.currentTimeMillis()
                    }
                } ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            
            val invite = GroupInvite(
                inviteId = response.id ?: 0,
                groupId = response.groupId,
                creatorId = response.creatorId,
                inviteCode = response.inviteCode,
                maxUses = response.maxUses,
                currentUses = response.currentUses,
                expiresAt = response.expiresAt,
                createdAt = createdAt
            )
            // 保存到本地数据库
            inviteDao.insertInvite(invite)
            invite
        } catch (e: HttpException) {
            // 如果是404，说明邀请码不存在，直接返回null
            if (e.code() == 404) {
                Log.d("GroupInviteRepository", "服务器返回404，邀请码不存在: $inviteCode")
                // 尝试从本地查询（可能是之前缓存的数据）
                inviteDao.getInviteByCode(inviteCode)
            } else {
                // 其他HTTP错误，记录并尝试从本地查询
                Log.e("GroupInviteRepository", "服务器查询邀请码失败: ${e.code()}, ${e.message()}")
                inviteDao.getInviteByCode(inviteCode)
            }
        } catch (e: Exception) {
            // 网络错误或其他异常，尝试从本地查询
            Log.e("GroupInviteRepository", "查询邀请码异常: ${e.message}", e)
            inviteDao.getInviteByCode(inviteCode)
        }
    }
    
    suspend fun getInviteById(inviteId: Int): GroupInvite? =
        inviteDao.getInviteById(inviteId)
    
    suspend fun insertInvite(invite: GroupInvite): Long {
        // 先保存到本地
        val localId = inviteDao.insertInvite(invite)
        Log.d("GroupInviteRepository", "本地保存邀请码: ${invite.inviteCode}, localId: $localId")
        
        // 同步到服务器（必须成功，否则无法跨设备使用）
        try {
            val request = GroupInviteCreateRequest(
                groupId = invite.groupId,
                creatorId = invite.creatorId,
                inviteCode = invite.inviteCode,
                maxUses = invite.maxUses,
                expiresAt = invite.expiresAt
            )
            Log.d("GroupInviteRepository", "开始同步邀请码到服务器: ${invite.inviteCode}")
            val response = api.createInvite(request)
            Log.d("GroupInviteRepository", "服务器创建邀请码成功: ${invite.inviteCode}, serverId: ${response.id}")
            
            // 如果服务器返回了ID，更新本地记录
            if (response.id != null && response.id != 0) {
                val updatedInvite = invite.copy(inviteId = response.id.toInt())
                inviteDao.updateInvite(updatedInvite)
                return response.id.toLong()
            }
            return localId
        } catch (e: HttpException) {
            // HTTP错误
            val errorMsg = "同步邀请码到服务器失败: HTTP ${e.code()}, ${e.message()}"
            Log.e("GroupInviteRepository", errorMsg, e)
            throw Exception(errorMsg, e)
        } catch (e: Exception) {
            // 网络错误或其他异常
            val errorMsg = "同步邀请码到服务器失败: ${e.message ?: "未知错误"}。请检查网络连接和后端服务是否正常运行。"
            Log.e("GroupInviteRepository", errorMsg, e)
            throw Exception(errorMsg, e)
        }
    }
    
    suspend fun updateInvite(invite: GroupInvite) =
        inviteDao.updateInvite(invite)
    
    suspend fun deleteInvite(invite: GroupInvite) =
        inviteDao.deleteInvite(invite)
    
    suspend fun incrementUseCount(inviteId: Int) {
        // 更新本地
        inviteDao.incrementUseCount(inviteId)
        
        // 同步到服务器
        try {
            api.incrementInviteUseCount(inviteId)
        } catch (e: Exception) {
            // 如果同步失败，记录错误但继续使用本地数据
            e.printStackTrace()
        }
    }
}

