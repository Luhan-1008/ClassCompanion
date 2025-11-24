# AI笔记助手开发测试指南

## 第一步：获取OpenAI API密钥

1. 访问 [OpenAI官网](https://platform.openai.com/)
2. 注册/登录账号
3. 进入 [API Keys页面](https://platform.openai.com/api-keys)
4. 点击 "Create new secret key" 创建新的API密钥
5. **重要**：复制并保存好这个密钥（只显示一次）

## 第二步：配置API密钥

### 方法1：直接在代码中配置（最简单，适合开发测试）

1. 打开文件：`app/src/main/java/com/example/myapplication/service/AiModelService.kt`

2. 找到第26-27行，修改为：
```kotlin
private val apiKey: String = "sk-your-actual-api-key-here" // 替换为你的实际API密钥
private val apiBaseUrl: String = "https://api.openai.com/v1" // OpenAI API端点
```

3. 将 `"sk-your-actual-api-key-here"` 替换为你刚才复制的API密钥

### 方法2：使用环境变量（更安全，但需要配置）

如果你使用Android Studio，可以在运行配置中添加环境变量。

## 第三步：检查依赖

确保 `app/build.gradle.kts` 文件中已添加OkHttp依赖（应该已经添加了）：

```kotlin
// OkHttp (for AI model service)
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

如果没有，请添加后点击 "Sync Now" 同步项目。

## 第四步：测试功能

### 测试步骤：

1. **运行应用**
   - 在Android Studio中点击运行按钮
   - 等待应用启动

2. **进入AI笔记助手**
   - 点击底部导航栏的"我的"
   - 找到"AI笔记助手"并点击

3. **测试文本分析**
   - 在文本框中输入一些课堂笔记内容，例如：
     ```
     今天学习了线性代数中的矩阵运算。矩阵乘法需要满足第一个矩阵的列数等于第二个矩阵的行数。
     矩阵的转置是将行和列互换。单位矩阵是对角线上全为1，其他位置全为0的方阵。
     ```
   - 点击"生成AI笔记"按钮
   - 等待几秒钟，应该会看到生成的知识提纲

4. **测试图片识别**
   - 点击"添加图片"按钮
   - 选择一张包含文字的图片（可以是课堂PPT截图、笔记照片等）
   - 点击"生成AI笔记"按钮
   - 等待处理（可能需要更长时间）

5. **测试音频转文字**
   - 点击"导入录音"按钮
   - 选择一个音频文件（支持m4a、mp3、wav等格式）
   - 点击"生成AI笔记"按钮
   - 等待处理（音频转文字可能需要较长时间）

## 第五步：查看结果

成功生成后，你会看到以下内容：

- **总结**：2-3句话概括主要内容
- **结构化大纲**：按主题分层次组织的内容
- **关键知识点**：提取的重要概念
- **思维导图节点**：主要概念及其关联
- **章节关联建议**：相关的课程章节

## 常见问题排查

### 问题1：提示"API调用失败"
- **原因**：API密钥配置错误或无效
- **解决**：检查API密钥是否正确，确保没有多余的空格
- **检查**：确认API密钥以 `sk-` 开头

### 问题2：提示"网络错误"或"连接超时"
- **原因**：网络连接问题或API服务不可用
- **解决**：
  - 检查网络连接
  - 确认设备可以访问 `api.openai.com`
  - 如果在中国大陆，可能需要使用VPN或代理

### 问题3：提示"余额不足"
- **原因**：OpenAI账户没有足够的余额
- **解决**：
  - 访问 [OpenAI Billing页面](https://platform.openai.com/account/billing)
  - 添加支付方式并充值
  - 注意：API调用会产生费用，请查看 [定价页面](https://openai.com/pricing)

### 问题4：图片识别不工作
- **原因**：可能使用了不支持视觉模型的API密钥
- **解决**：确保使用GPT-4 Vision模型，检查代码中模型名称是否为 `gpt-4-vision-preview`

### 问题5：音频转文字失败
- **原因**：音频格式不支持或文件过大
- **解决**：
  - 确保音频格式为 m4a、mp3、wav、webm 等常见格式
  - 文件大小建议不超过25MB
  - 音频时长建议不超过60分钟

## 费用说明

OpenAI API按使用量收费：

- **GPT-4**：约 $0.03 / 1K tokens（输入），$0.06 / 1K tokens（输出）
- **GPT-4 Vision**：约 $0.01 / 1K tokens（输入），图片按分辨率收费
- **Whisper**：约 $0.006 / 分钟

**建议**：
- 开发测试时使用较短的文本
- 使用较小的图片和音频文件
- 定期检查API使用量和费用

## 测试建议

1. **先用文本测试**：最简单，费用最低
2. **再用图片测试**：验证视觉模型是否正常工作
3. **最后测试音频**：最复杂，费用较高

## 下一步

如果测试成功，你可以：
- 优化提示词以获得更好的结果
- 调整模型参数（temperature、max_tokens等）
- 添加错误处理和重试机制
- 实现结果缓存以减少API调用

## 需要帮助？

如果遇到问题，可以：
1. 查看Android Studio的Logcat日志
2. 检查网络请求是否成功
3. 查看OpenAI Dashboard中的API使用日志

