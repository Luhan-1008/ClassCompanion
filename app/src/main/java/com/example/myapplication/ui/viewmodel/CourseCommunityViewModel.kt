package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Course
import com.example.myapplication.data.model.CourseDifficultyTag
import com.example.myapplication.data.model.CourseResource
import com.example.myapplication.data.model.CourseResourceType
import com.example.myapplication.data.model.CourseReview
import com.example.myapplication.data.repository.CourseRepository
import com.example.myapplication.data.repository.CourseResourceRepository
import com.example.myapplication.data.repository.CourseReviewRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CourseCommunityUiState(
    val courses: List<Course> = emptyList(),
    val selectedCourse: Course? = null,
    val reviews: List<CourseReview> = emptyList(),
    val resources: List<CourseResource> = emptyList(),
    val averageRating: Double = 0.0,
    val difficultyLabel: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CourseCommunityViewModel(
    private val courseRepository: CourseRepository,
    private val reviewRepository: CourseReviewRepository,
    private val resourceRepository: CourseResourceRepository
) : ViewModel() {

    private val userId: Int
        get() = com.example.myapplication.session.CurrentSession.userIdInt ?: 0

    private val _uiState = MutableStateFlow(CourseCommunityUiState(isLoading = true))
    val uiState: StateFlow<CourseCommunityUiState> = _uiState.asStateFlow()

    private var reviewJob: Job? = null
    private var resourceJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val courses = courseRepository.getCoursesSnapshot(userId)
                _uiState.value = _uiState.value.copy(
                    courses = courses,
                    selectedCourse = courses.firstOrNull(),
                    isLoading = false
                )
                courses.firstOrNull()?.let { observeCourseData(it.courseId) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加载课程信息失败：${e.message}"
                )
            }
        }
    }

    fun selectCourse(course: Course) {
        _uiState.value = _uiState.value.copy(selectedCourse = course)
        observeCourseData(course.courseId)
    }

    private fun observeCourseData(courseId: Int) {
        reviewJob?.cancel()
        resourceJob?.cancel()

        reviewJob = viewModelScope.launch {
            reviewRepository.getReviewsByCourse(courseId).collect { reviews ->
                val avg = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
                val diffLabel = reviews
                    .groupBy { it.difficulty }
                    .maxByOrNull { it.value.size }
                    ?.key
                    ?.let { mapDifficultyLabel(it) }
                    ?: "暂无"
                _uiState.value = _uiState.value.copy(
                    reviews = reviews,
                    averageRating = avg,
                    difficultyLabel = diffLabel
                )
            }
        }

        resourceJob = viewModelScope.launch {
            resourceRepository.getResourcesByCourse(courseId).collect { resources ->
                _uiState.value = _uiState.value.copy(resources = resources)
            }
        }
    }

    fun submitReview(
        courseId: Int,
        rating: Int,
        difficultyTag: CourseDifficultyTag,
        workloadHours: Int,
        highlightTags: List<String>,
        comment: String,
        suggestion: String?
    ) {
        viewModelScope.launch {
            try {
                val review = CourseReview(
                    courseId = courseId,
                    userId = userId,
                    rating = rating,
                    difficulty = difficultyTag,
                    workloadHoursPerWeek = workloadHours,
                    highlightTags = highlightTags.filter { it.isNotBlank() }.joinToString("|"),
                    comment = comment,
                    suggestion = suggestion
                )
                reviewRepository.insertReview(review)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "提交评价失败：${e.message}"
                )
            }
        }
    }

    fun submitResource(
        courseId: Int,
        title: String,
        url: String,
        resourceType: CourseResourceType,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                val resource = CourseResource(
                    courseId = courseId,
                    userId = userId,
                    title = title,
                    resourceUrl = url,
                    resourceType = resourceType,
                    description = description
                )
                resourceRepository.insertResource(resource)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "分享资料失败：${e.message}"
                )
            }
        }
    }

    private fun mapDifficultyLabel(tag: CourseDifficultyTag): String {
        return when (tag) {
            CourseDifficultyTag.EASY -> "水课"
            CourseDifficultyTag.MEDIUM -> "适中"
            CourseDifficultyTag.HARD -> "硬核"
        }
    }

    fun formatRatingDisplay(): String {
        val avg = _uiState.value.averageRating
        return if (avg == 0.0) "暂无评分" else "${(avg * 10).roundToInt() / 10.0} / 5"
    }

    fun parseTags(tagString: String): List<String> =
        tagString.split("|").map { it.trim() }.filter { it.isNotEmpty() }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class CourseCommunityViewModelFactory(
    private val courseRepository: CourseRepository,
    private val reviewRepository: CourseReviewRepository,
    private val resourceRepository: CourseResourceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseCommunityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseCommunityViewModel(courseRepository, reviewRepository, resourceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

