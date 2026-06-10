# BUAA课程伴侣（航侣 App）
这是一个前后端分离的 Android 项目。
- `app/`：Android 客户端，负责界面、本地存储、调用后端接口。
- `backend/`：Spring Boot 后端，负责登录注册、业务接口、连接 MySQL。
## 项目启动顺序
1. 先处理 MySQL 服务，建议运行 `scripts/2-MySQL数据库工具.bat`。
2. 运行 `scripts/1-检查环境.bat` 检查开发环境。
3. 在数据库工具菜单中启动 `mysql80`，确认数据库 `android` 存在，必要时导入 `database_schema.sql`。
4. 运行 `scripts/3-配置安卓后端地址.bat` 配置 `API_BASE_URL`。
5. 运行 `scripts/4-启动后端.bat` 启动后端服务。
6. 运行 `scripts/5-检查后端状态.bat` 检查后端是否正常。
7. 运行 `scripts/6-打开AndroidStudio.bat` 打开项目。
8. 在 Android Studio 中运行 `app` 模块。
## 地址配置说明
- Android 模拟器：使用 `http://10.0.2.2:8080/`
- 真机或局域网设备：使用你电脑的局域网 IP，例如 `http://192.168.1.10:8080/`
## 常用脚本
- `scripts/一键启动工具箱.bat`：打开启动菜单。
- `scripts/1-检查环境.bat`：检查 Java、MySQL、mysql80 服务、3306 端口和关键文件。
- `scripts/2-MySQL数据库工具.bat`：打开 MySQL 数据库工具菜单，支持检查 3306 端口、启动/停止/删除 `mysql80`、登录 MySQL、创建 `android` 数据库并导入 `database_schema.sql`。
- `scripts/3-配置安卓后端地址.bat`：自动修改 `app/build.gradle.kts` 中的 `API_BASE_URL`。
- `scripts/4-启动后端.bat`：启动 Spring Boot 后端。
- `scripts/5-检查后端状态.bat`：检查 8080 端口和健康检查接口。
- `scripts/6-打开AndroidStudio.bat`：尝试自动打开 Android Studio。
- `scripts/7-查看启动说明.bat`：显示简要启动说明。
- `scripts/8-查看通知日志.bat`：查看通知相关的 ADB 日志。
- `scripts/9-Gradle命令入口.bat`：统一从脚本目录调用根目录 `gradlew.bat`。
## 你常用的 MySQL 处理流程
1. 如果 3306 端口被占用，先在 `scripts/2-MySQL数据库工具.bat` 中选择“检查 3306 端口占用”，根据 PID 去任务管理器结束对应进程。
2. 如果 `mysql80` 服务名冲突，可以在数据库工具菜单里先停止，再删除 `mysql80` 服务；这一步建议用管理员权限运行。
3. 需要启动数据库时，在数据库工具菜单里选择“启动 mysql80 服务”。
4. 需要手动进入 MySQL 时，在数据库工具菜单里选择“登录 MySQL（mysql -u root -p）”。
5. 如果后端报 `Access denied for user 'root'@'localhost'`，说明 `backend/src/main/resources/application.yml` 中的用户名或密码与你本机 MySQL 实际账号不一致，需要改成正确值。
## 运行验证
启动完成后，建议先注册一个新账号，再登录一次。如果换一个模拟器后仍能用同一账号重新登录，说明客户端已经成功连接到后端和 MySQL。
