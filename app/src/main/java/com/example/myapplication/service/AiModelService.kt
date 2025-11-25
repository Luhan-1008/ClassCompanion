package com.example.myapplication.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.webkit.MimeTypeMap
import kotlin.math.min

/**
 * 大模型服务类
 * 支持调用OpenAI API或其他兼容的大模型API
 */
class AiModelService(private val context: Context) {
    
    // 这里需要配置你的API密钥和端点
    // 可以在local.properties中配置，或使用环境变量
    // 注意：在生产环境中，应该从安全的地方读取API密钥，不要硬编码
    private val apiKey: String = "e25e651255de49b3a361395dda2b4c36.ErWVuTcvHPsqGmlX"
    private val chatCompletionUrl: String = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    private val speechToTextUrl: String = "https://open.bigmodel.cn/api/paas/v4/audio/transcriptions"
    private val visionModel = "glm-4v"
    private val textModel = "glm-4-flash"
    private val sttModel = "glm-asr"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * 生成知识提纲
     * @param textContent 文本内容
     * @param imageBase64 图片的base64编码（可选）
     * @param audioTranscript 音频转文字结果（可选）
     * @return 结构化的知识提纲JSON
     */
    suspend fun generateKnowledgeOutline(
        textContent: String,
        imageBase64: String? = null,
        audioTranscript: String? = null
    ): Result<KnowledgeOutline> = withContext(Dispatchers.IO) {
        try {
            // 验证是否有内容
            val hasText = textContent.isNotBlank()
            val hasImage = imageBase64 != null
            val hasAudio = audioTranscript != null && audioTranscript.isNotBlank()
            
            if (!hasText && !hasImage && !hasAudio) {
                return@withContext Result.failure(
                    Exception("请提供文本、图片或音频内容以生成知识提纲")
                )
            }
            
            // 构建提示词
            val systemPrompt = """你是一位专业的教学助手，擅长将课堂内容整理成结构清晰、重点突出的知识提纲。
请根据提供的课堂内容（包括文本、图片和录音转文字），生成以下格式的JSON知识提纲：

{
  "summary": "用2-3句话概括主要内容",
  "structuredOutline": [
    {
      "title": "主题标题",
      "bulletPoints": ["要点1", "要点2", "要点3"]
    }
  ],
  "keyPoints": ["关键知识点1", "关键知识点2"],
  "mindMapBranches": [
    {
      "title": "中心主题",
      "nodes": ["节点1", "节点2"]
    }
  ],
  "chapterLinks": [
    {
      "courseName": "课程名",
      "chapterLabel": "章节",
      "reason": "关联理由"
    }
  ]
}

请确保返回严格的JSON格式，不要包含任何其他文本。"""
            
            val userContent = buildString {
                if (hasText) {
                    append("文本内容：\n$textContent\n\n")
                }
                if (hasAudio) {
                    append("录音转文字：\n$audioTranscript\n\n")
                }
                if (hasImage) {
                    append("已上传图片，请识别图片中的文字和内容。\n\n")
                }
                append("请根据以上内容生成结构化的JSON格式知识提纲。")
            }
            
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    if (imageBase64 != null) {
                        // 使用vision模型处理图片
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", userContent)
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$imageBase64")
                                })
                            })
                        })
                    } else {
                        put("content", userContent)
                    }
                })
            }
            
            val requestBody = JSONObject().apply {
                put("model", if (imageBase64 != null) visionModel else textModel)
                put("messages", messages)
                put("temperature", 0.7)
                put("top_p", 0.9)
                put("max_tokens", 2000)
            }
            
            val request = Request.Builder()
                .url(chatCompletionUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            Log.d("AiModelService", "generateKnowledgeOutline model=${if (imageBase64 != null) visionModel else textModel}")
            Log.d("AiModelService", "requestBody=$requestBody")
            
            val response = executeWithRetry(request)
            val responseBody = response.body?.string().orEmpty()
            
            Log.d("AiModelService", "responseCode=${response.code}, body=$responseBody")
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API调用失败: ${response.code} ${response.message}")
                )
            }
            
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")
            
            val outline = parseModelResponse(content)
            
            Result.success(outline)
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("请求智谱超时，请稍后重试", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateAssignmentHint(
        question: String,
        contextInfo: String?
    ): Result<AssignmentHintResponse> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
你是一位专业的作业辅导老师，请围绕“启发式解题”提供帮助，避免直接给出作业答案。
返回严格的JSON，格式如下：
{
  "concepts": ["概念1", "..."],
  "formulas": ["公式1", "..."],
  "steps": ["步骤1", "..."],
  "chapters": [
    {"courseName":"课程名","chapterLabel":"章节","reason":"推荐理由"}
  ],
  "discussions": ["建议查阅的小组讨论或扩展资源"]
}
"""
            val userContent = buildString {
                append("问题描述：\n$question\n\n")
                contextInfo?.takeIf { it.isNotBlank() }?.let {
                    append("补充背景：\n$it")
                }
            }
            
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt.trimIndent())
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            }
            
            val requestBody = JSONObject().apply {
                put("model", textModel)
                put("messages", messages)
                put("temperature", 0.6)
                put("max_tokens", 1200)
            }
            
            val request = Request.Builder()
                .url(chatCompletionUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            Log.d("AiModelService", "generateAssignmentHint model=$textModel")
            Log.d("AiModelService", "requestBody=$requestBody")
            
            val response = executeWithRetry(request)
            val responseBody = response.body?.string().orEmpty()
            
            Log.d("AiModelService", "responseCode=${response.code}, body=$responseBody")
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("提示生成失败: ${response.code} ${response.message}")
                )
            }
            
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message").getString("content")
            
            Result.success(parseAssignmentHintResponse(message))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("请求智谱超时，请稍后重试", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 将图片URI转换为base64编码
     */
    suspend fun imageToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                bitmap?.let { bmp ->
                    val outputStream = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 语音转文字（使用智谱语音识别API）
     */
    suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = copyUriToTempFile(uri)
                ?: return@withContext Result.failure(Exception("无法读取音频文件"))

            val mimeType = getMimeType(uri) ?: "audio/mpeg"
            val normalizedMimeType = mimeType.lowercase()

            val supportedFormats = listOf("audio/mpeg", "audio/mp3", "audio/wav", "audio/wave")
            if (supportedFormats.none { normalizedMimeType.contains(it.substringAfter("/")) }) {
                tempFile.delete()
                return@withContext Result.failure(
                    Exception("不支持的音频格式: $mimeType。请使用 MP3 或 WAV 格式")
                )
            }

            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", sttModel)
                .addFormDataPart("file", tempFile.name, requestBody)
                .build()

            val request = Request.Builder()
                .url(speechToTextUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(multipartBody)
                .build()

            Log.d("AiModelService", "transcribeAudio request file=${tempFile.name}, mimeType=$mimeType")

            val response = executeWithRetry(request)
            val responseBody = response.body?.string().orEmpty()

            Log.d("AiModelService", "transcribeAudio responseCode=${response.code}, body=$responseBody")

            if (!response.isSuccessful) {
                tempFile.delete()
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    val error = errorJson.optJSONObject("error")
                    error?.optString("message") ?: responseBody
                } catch (e: Exception) {
                    responseBody
                }
                return@withContext Result.failure(Exception("语音识别失败: $errorMessage"))
            }

            tempFile.delete()

            val json = JSONObject(responseBody)
            val text = json.optString("text").ifBlank {
                json.optString("result")
            }

            if (text.isBlank()) {
                Result.failure(Exception("语音识别返回为空"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            tempFile?.delete()
            Log.e("AiModelService", "transcribeAudio error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析大模型返回的内容为结构化格式
     */
    private fun parseModelResponse(content: String): KnowledgeOutline {
        // 检查模型是否在请求内容
        if (content.contains("需要") && (content.contains("内容") || content.contains("提供"))) {
            Log.w("AiModelService", "模型返回提示需要内容: $content")
            throw Exception("模型提示需要提供内容。请确保已输入文本、上传图片或成功转录音频。")
        }
        
        // 尝试解析JSON格式的响应
        return try {
            // 尝试提取JSON部分（可能被markdown代码块包裹）
            val jsonContent = content.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val json = JSONObject(jsonContent)
            KnowledgeOutline(
                summary = json.optString("summary", ""),
                structuredOutline = parseOutlineSections(json.optJSONArray("structuredOutline")),
                keyPoints = parseStringArray(json.optJSONArray("keyPoints")),
                mindMapBranches = parseMindMapBranches(json.optJSONArray("mindMapBranches")),
                chapterLinks = parseChapterLinks(json.optJSONArray("chapterLinks"))
            )
        } catch (e: Exception) {
            Log.e("AiModelService", "模型未返回标准JSON: $content", e)
            parseTextResponse(content, reason = "AI 没有返回结构化笔记")
        }
    }
    
    private fun parseTextResponse(text: String, reason: String? = null): KnowledgeOutline {
        // 简单的文本解析逻辑
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        
        var summary = ""
        val outlineSections = mutableListOf<OutlineSection>()
        val keyPoints = mutableListOf<String>()
        val mindMapBranches = mutableListOf<MindMapBranch>()
        val chapterLinks = mutableListOf<ChapterLink>()
        
        var currentSection: OutlineSection? = null
        var currentSectionPoints = mutableListOf<String>()
        
        lines.forEach { line ->
            when {
                line.contains("总结") || line.contains("概要") -> {
                    summary = line.substringAfter("：").substringAfter(":").trim()
                }
                line.startsWith("#") || line.matches(Regex("^[一二三四五六七八九十]+[、.]")) -> {
                    // 新的章节标题
                    currentSection?.let {
                        outlineSections.add(it.copy(bulletPoints = currentSectionPoints))
                    }
                    currentSection = OutlineSection(
                        title = line.replace(Regex("^[#一二三四五六七八九十]+[、.]"), "").trim(),
                        bulletPoints = emptyList()
                    )
                    currentSectionPoints = mutableListOf()
                }
                line.startsWith("-") || line.startsWith("•") || line.matches(Regex("^\\d+[.)]")) -> {
                    val point = line.replace(Regex("^[-•\\d+.)]"), "").trim()
                    if (currentSection != null) {
                        currentSectionPoints.add(point)
                    } else {
                        keyPoints.add(point)
                    }
                }
                line.contains("关键") || line.contains("重点") -> {
                    // 提取关键点
                }
            }
        }
        
        currentSection?.let {
            outlineSections.add(it.copy(bulletPoints = currentSectionPoints))
        }
        
        val fallbackSummary = summary.ifBlank {
            when {
                reason != null -> "$reason：$text"
                text.isNotBlank() -> text
                else -> "AI 返回：$text"
            }
        }
        
        if (outlineSections.isEmpty() && lines.isNotEmpty()) {
            outlineSections.add(
                OutlineSection(
                    title = "图片识别内容",
                    bulletPoints = lines
                )
            )
        }
        
        if (keyPoints.isEmpty()) {
            if (lines.isNotEmpty()) {
                keyPoints.addAll(lines.take(8))
            } else {
                keyPoints.add(text)
            }
        }
        
        return KnowledgeOutline(
            summary = fallbackSummary,
            structuredOutline = outlineSections,
            keyPoints = keyPoints.take(8),
            mindMapBranches = mindMapBranches,
            chapterLinks = chapterLinks
        )
    }
    
    private fun parseOutlineSections(array: JSONArray?): List<OutlineSection> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            OutlineSection(
                title = obj.optString("title", ""),
                bulletPoints = parseStringArray(obj.optJSONArray("bulletPoints"))
            )
        }
    }
    
    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it, null) }
    }
    
    private fun parseMindMapBranches(array: JSONArray?): List<MindMapBranch> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            MindMapBranch(
                title = obj.optString("title", ""),
                nodes = parseStringArray(obj.optJSONArray("nodes"))
            )
        }
    }
    
    private fun parseChapterLinks(array: JSONArray?): List<ChapterLink> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            ChapterLink(
                courseName = obj.optString("courseName", ""),
                chapterLabel = obj.optString("chapterLabel", ""),
                reason = obj.optString("reason", "")
            )
        }
    }
    
    private fun parseAssignmentHintResponse(content: String): AssignmentHintResponse {
        return try {
            val json = JSONObject(content)
            AssignmentHintResponse(
                relatedConcepts = parseStringArray(json.optJSONArray("concepts")),
                formulas = parseStringArray(json.optJSONArray("formulas")),
                solutionSteps = parseStringArray(json.optJSONArray("steps")),
                chapterLinks = parseChapterLinks(json.optJSONArray("chapters")),
                relatedDiscussions = parseStringArray(json.optJSONArray("discussions"))
            )
        } catch (e: Exception) {
            AssignmentHintResponse(
                relatedConcepts = emptyList(),
                formulas = emptyList(),
                solutionSteps = listOf(content),
                chapterLinks = emptyList(),
                relatedDiscussions = emptyList()
            )
        }
    }
    
    private suspend fun executeWithRetry(request: Request, maxRetry: Int = 1): Response {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt <= maxRetry) {
            try {
                return httpClient.newCall(request).execute()
            } catch (e: Exception) {
                lastError = e
                if (attempt == maxRetry) throw e
                delay(((attempt + 1) * 1000L).coerceAtMost(4000L))
            }
            attempt++
        }
        throw lastError ?: IllegalStateException("未知错误")
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val cacheDir = File(context.cacheDir, "ai_audio").apply { if (!exists()) mkdirs() }
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(getMimeType(uri)) ?: "m4a"
            val tempFile = File.createTempFile("audio_", ".$extension", cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            tempFile
        } catch (e: Exception) {
            Log.e("AiModelService", "copyUriToTempFile error ${e.message}", e)
            null
        }
    }

    private fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
            ?: uri.path?.let { path ->
                val extension = path.substringAfterLast('.', "")
                if (extension.isNotBlank()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                } else null
            }
    }
    
}

/**
 * 知识提纲数据类
 */
data class KnowledgeOutline(
    val summary: String,
    val structuredOutline: List<OutlineSection>,
    val keyPoints: List<String>,
    val mindMapBranches: List<MindMapBranch>,
    val chapterLinks: List<ChapterLink>
)

data class OutlineSection(
    val title: String,
    val bulletPoints: List<String>
)

data class MindMapBranch(
    val title: String,
    val nodes: List<String>
)

data class ChapterLink(
    val courseName: String,
    val chapterLabel: String,
    val reason: String
)

data class AssignmentHintResponse(
    val relatedConcepts: List<String>,
    val formulas: List<String>,
    val solutionSteps: List<String>,
    val chapterLinks: List<ChapterLink>,
    val relatedDiscussions: List<String>
)

