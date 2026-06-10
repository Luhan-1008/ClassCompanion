# 邀请码功能IP配置方案

## 当前问题
连接失败：`failed to connect to /10.0.2.2 (port 8080)`
- 你的手机IP：10.192.116.187
- 你的电脑IP（WLAN）：10.193.78.203

## 推荐配置方案（按优先级）

### 方案1：使用当前电脑IP（推荐）
**文件：`app/build.gradle.kts` 第24行**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.193.78.203:8080/\"")
```
**优点：** 根据ipconfig结果，这是你当前电脑的IP地址

### 方案2：使用之前配置的IP
**文件：`app/build.gradle.kts` 第24行**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.192.89.16:8080/\"")
```
**注意：** 确保这个IP地址是你的电脑IP

### 方案3：如果使用Android模拟器
**文件：`app/build.gradle.kts` 第24行**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
```
**注意：** 仅适用于Android模拟器，不适用于真机

## 快速修复步骤

1. **打开文件**：`app/build.gradle.kts`
2. **找到第24行**，修改为方案1的配置
3. **点击 "Sync Now"** 同步项目
4. **重新编译安装**到手机

## 验证配置

修改后，在手机上用浏览器访问：
```
http://10.193.78.203:8080/api/health
```
应该返回：`{"status":"ok"}`

如果浏览器能访问，应用就能连接。

## 如果还是连接失败

1. **检查后端服务器是否运行**
   ```bash
   cd backend
   ./gradlew bootRun
   ```

2. **检查防火墙**
   - Windows防火墙可能阻止了8080端口
   - 临时关闭防火墙测试，或添加8080端口例外

3. **确认手机和电脑在同一WiFi**
   - 手机IP：10.192.116.187
   - 电脑IP：10.193.78.203
   - 如果不在同一网段，可能无法连接

4. **尝试其他IP地址**
   - 查看所有网络适配器的IP
   - 尝试使用其他网卡的IP地址

