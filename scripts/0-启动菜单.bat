@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0.."

title ClassCompanion 启动菜单

:menu
cls
echo ========================================
echo         ClassCompanion 启动菜单
echo ========================================
echo.
echo [1] 检查开发环境
echo [2] MySQL 数据库工具
echo [3] 配置安卓端后端地址
echo [4] 启动后端
echo [5] 检查后端状态
echo [6] 打开 Android Studio
echo [7] 查看启动说明
echo [8] 查看通知日志
echo [9] Gradle 命令入口
echo [0] 退出
echo.
set /p choice=请输入选项编号: 

if "%choice%"=="1" call "%~dp0\1-检查环境.bat"
if "%choice%"=="2" call "%~dp0\2-MySQL数据库工具.bat"
if "%choice%"=="3" call "%~dp0\3-配置安卓后端地址.bat"
if "%choice%"=="4" call "%~dp0\4-启动后端.bat"
if "%choice%"=="5" call "%~dp0\5-检查后端状态.bat"
if "%choice%"=="6" call "%~dp0\6-打开AndroidStudio.bat"
if "%choice%"=="7" call "%~dp0\7-查看启动说明.bat"
if "%choice%"=="8" call "%~dp0\8-查看通知日志.bat"
if "%choice%"=="9" call "%~dp0\9-Gradle命令入口.bat"
if "%choice%"=="0" goto end

echo.
echo 按任意键返回菜单...
pause >nul
goto menu

:end
endlocal
