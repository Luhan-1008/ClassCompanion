@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo ========================================
echo           启动后端服务
echo ========================================
echo.
echo 启动前请确认：
echo 1. MySQL 已启动
echo 2. 数据库 android 已创建
echo 3. 用户名密码与 backend\src\main\resources\application.yml 一致
echo.
echo 正在启动后端，请等待看到 Started Application ...
echo 按 Ctrl+C 可以停止后端
echo.
cd backend
call gradlew.bat bootRun

endlocal
