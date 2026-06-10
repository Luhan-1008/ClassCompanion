# AI模型服务配置说明

## 配置API密钥

在使用AI笔记助手功能之前，需要配置大模型API密钥。

### 方法1：在代码中直接配置（仅用于开发测试）

编辑 `app/src/main/java/com/example/myapplication/service/AiModelService.kt` 文件：

```kotlin
private val apiKey: String = "your-api-key-here" // 替换为你的API密钥
private val apiBaseUrl: String = "https://api.openai.com/v1" // 或使用其他兼容的API端点
```

### 方法2：使用环境变量或配置文件（推荐用于生产环境）

1. 在 `local.properties` 文件中添加：
```properties
OPENAI_API_KEY=your-api-key-here
OPENAI_API_BASE_URL=https://api.openai.com/v1
```

2. 修改 `AiModelService.kt` 读取配置：
```kotlin
import java.util.Properties
import java.io.FileInputStream

private val apiKey: String = run {
    val properties = Properties()
    val localProperties = File("local.properties")
    if (localProperties.exists()) {
        FileInputStream(localProperties).use { properties.load(it) }
    }
    properties.getProperty("OPENAI_API_KEY", "your-api-key-here")
}
```

## 支持的大模型

### OpenAI API
- 文本模型：`gpt-4`, `gpt-3.5-turbo`
- 视觉模型：`gpt-4-vision-preview`（用于图片识别）
- 语音模型：`whisper-1`（用于音频转文字）

### 其他兼容API
- 可以修改 `apiBaseUrl` 指向其他兼容OpenAI API格式的服务
- 例如：Claude API、本地部署的模型服务等

## 功能说明

1. **文本分析**：直接分析用户输入的文本内容
2. **图片识别**：支持上传课堂笔记图片，自动识别文字和内容
3. **音频转文字**：支持上传课堂录音，自动转换为文字后分析
4. **知识提纲生成**：自动生成结构清晰、重点突出的知识提纲

## 注意事项

- API调用会产生费用，请合理使用
- 音频文件建议使用常见格式（m4a, mp3, wav等）
- 图片文件建议使用JPEG或PNG格式
- 大文件上传可能需要较长时间，请耐心等待

