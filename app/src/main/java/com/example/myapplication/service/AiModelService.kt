package com.example.myapplication.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 大模型服务类
 * 支持调用OpenAI API或其他兼容的大模型API
 */
class AiModelService(private val context: Context) {
    
    // 这里需要配置你的API密钥和端点
    // 可以在local.properties中配置，或使用环境变量
    // 注意：在生产环境中，应该从安全的地方读取API密钥，不要硬编码
    private val apiKey: String = System.getenv("OPENAI_API_KEY") 
        ?: "your-api-key-here" // 需要替换为实际的API密钥，或设置环境变量
    private val apiBaseUrl: String = System.getenv("OPENAI_API_BASE_URL")
        ?: "https://api.openai.com/v1" // OpenAI API端点，或使用环境变量
    
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
            val client = OkHttpClient()
            
            // 构建提示词
            val systemPrompt = """你是一位专业的教学助手，擅长将课堂内容整理成结构清晰、重点突出的知识提纲。
请根据提供的课堂内容（包括文本、图片和录音转文字），生成以下格式的知识提纲：

1. 总结：用2-3句话概括主要内容
2. 结构化大纲：按主题分层次组织，每个主题包含3-5个要点
3. 关键知识点：提取5-8个最重要的概念或公式
4. 思维导图节点：识别主要概念及其关联
5. 章节关联建议：如果内容涉及特定课程章节，请标注

请确保提纲结构清晰、重点突出、便于复习。"""
            
            val userContent = buildString {
                if (textContent.isNotBlank()) {
                    append("文本内容：\n$textContent\n\n")
                }
                if (audioTranscript != null && audioTranscript.isNotBlank()) {
                    append("录音转文字：\n$audioTranscript\n\n")
                }
                if (imageBase64 != null) {
                    append("已上传图片，请识别图片中的文字和内容。\n\n")
                }
                append("请生成结构化的知识提纲。")
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
                put("model", if (imageBase64 != null) "gpt-4-vision-preview" else "gpt-4")
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 2000)
            }
            
            val request = Request.Builder()
                .url("$apiBaseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API调用失败: ${response.code} ${response.message}")
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")
            
            // 解析大模型返回的内容为结构化格式
            val outline = parseModelResponse(content)
            
            Result.success(outline)
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
     * 语音转文字（使用OpenAI Whisper API）
     */
    suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            
            if (inputStream == null) {
                return@withContext Result.failure(Exception("无法读取音频文件"))
            }
            
            val audioBytes = inputStream.readBytes()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "audio.m4a",
                    RequestBody.create("audio/m4a".toMediaTypeOrNull(), audioBytes)
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "zh")
                .build()
            
            val request = Request.Builder()
                .url("$apiBaseUrl/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("语音识别失败: ${response.code} ${response.message}")
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val text = jsonResponse.getString("text")
            
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解析大模型返回的内容为结构化格式
     */
    private fun parseModelResponse(content: String): KnowledgeOutline {
        // 尝试解析JSON格式的响应
        return try {
            val json = JSONObject(content)
            KnowledgeOutline(
                summary = json.optString("summary", ""),
                structuredOutline = parseOutlineSections(json.optJSONArray("structuredOutline")),
                keyPoints = parseStringArray(json.optJSONArray("keyPoints")),
                mindMapBranches = parseMindMapBranches(json.optJSONArray("mindMapBranches")),
                chapterLinks = parseChapterLinks(json.optJSONArray("chapterLinks"))
            )
        } catch (e: Exception) {
            // 如果不是JSON格式，尝试从文本中提取
            parseTextResponse(content)
        }
    }
    
    private fun parseTextResponse(text: String): KnowledgeOutline {
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
        
        return KnowledgeOutline(
            summary = summary.ifBlank { "已生成知识提纲" },
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

