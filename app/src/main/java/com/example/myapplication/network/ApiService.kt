package com.example.myapplication.network

import com.example.myapplication.network.dto.CourseCreateRequest
import com.example.myapplication.network.dto.GroupInviteCreateRequest
import com.example.myapplication.network.dto.GroupInviteResponse
import com.example.myapplication.network.dto.GroupMemberCreateRequest
import com.example.myapplication.network.dto.GroupMemberResponse
import com.example.myapplication.network.dto.LoginRequest
import com.example.myapplication.network.dto.LoginResponse
import com.example.myapplication.network.dto.RegisterRequest
import com.example.myapplication.network.dto.RegisterResponse
import com.example.myapplication.network.dto.RemoteCourseDto
import com.example.myapplication.network.dto.UpdateUserProfileRequest
import com.example.myapplication.network.dto.UserProfileResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// 客户端明确走后端的接口
interface ApiService {

    // 登录相关API
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // 注册相关API
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") userId: Long): UserProfileResponse

    @PUT("api/users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: Long,
        @Body body: UpdateUserProfileRequest
    ): UserProfileResponse

    @GET("api/courses")
    suspend fun listCourses(@Query("userId") userId: Int): List<RemoteCourseDto>

    @POST("api/courses")
    suspend fun createCourse(@Body body: CourseCreateRequest): RemoteCourseDto

    @PUT("api/courses/{courseId}")
    suspend fun updateCourse(
        @Path("courseId") courseId: Int,
        @Body body: CourseCreateRequest
    ): RemoteCourseDto

    @DELETE("api/courses/{courseId}")
    suspend fun deleteCourse(@Path("courseId") courseId: Int)

    // 邀请码相关API
    @POST("api/groups/invites")
    suspend fun createInvite(@Body body: GroupInviteCreateRequest): GroupInviteResponse

    @GET("api/groups/invites")
    suspend fun getInviteByCode(@Query("inviteCode") inviteCode: String): GroupInviteResponse

    @POST("api/groups/invites/{inviteId}/increment")
    suspend fun incrementInviteUseCount(@Path("inviteId") inviteId: Int): GroupInviteResponse
    
    // 小组成员相关API
    @POST("api/groups/members")
    suspend fun addMember(@Body body: GroupMemberCreateRequest): GroupMemberResponse
    
    @GET("api/groups/members")
    suspend fun listMembers(@Query("groupId") groupId: Int): List<GroupMemberResponse>
}
