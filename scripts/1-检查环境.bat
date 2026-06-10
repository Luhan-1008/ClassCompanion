@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0.."
echo ========================================
echo            检查开发环境
echo ========================================
echo.
echo [1/6] 检查 Java...
where java >nul 2>nul
if %errorlevel%==0 (
    java -version
) else (
    echo [警告] 未找到 java，请安装 JDK 17
)
echo.
echo [2/6] 检查 Gradle Wrapper...
if exist "backend\gradlew.bat" (
    echo [成功] 已找到 backend\gradlew.bat
) else (
    echo [失败] 未找到 backend\gradlew.bat
)
echo.
echo [3/6] 检查 MySQL 客户端...
where mysql >nul 2>nul
if %errorlevel%==0 (
    mysql --version
) else (
    echo [提示] 未找到 mysql 命令行客户端
    echo        如果你用 Navicat / MySQL Workbench，也可以正常使用
)
echo.
echo [4/6] 检查 MySQL 服务 mysql80...
sc query mysql80 >nul 2>nul
if %errorlevel%==0 (
    for /f "tokens=3" %%i in ('sc query mysql80 ^| findstr /I "STATE"') do set MYSQL80_STATE=%%i
    echo [信息] mysql80 服务存在，当前状态：!MYSQL80_STATE!
) else (
    echo [提示] 未检测到 mysql80 服务
)
echo.
echo [5/6] 检查 3306 端口占用...
set PORT_FOUND=
for /f "tokens=5" %%i in ('netstat -ano ^| findstr ":3306" ^| findstr "LISTENING"') do (
    set PORT_FOUND=1
    echo [信息] 3306 端口正在被 PID %%i 占用
)
if not defined PORT_FOUND (
    echo [提示] 当前没有检测到 3306 端口监听
)
echo.
echo [6/6] 检查项目关键文件...
if exist "backend\src\main\resources\application.yml" (
    echo [成功] 已找到 backend 配置文件
) else (
    echo [失败] 未找到 backend 配置文件
)
if exist "app\build.gradle.kts" (
    echo [成功] 已找到 app 构建文件
) else (
    echo [失败] 未找到 app 构建文件
)
if exist "database_schema.sql" (
    echo [成功] 已找到数据库初始化脚本
) else (
    echo [失败] 未找到 database_schema.sql
)
echo.
echo 检查完成。
endlocal
