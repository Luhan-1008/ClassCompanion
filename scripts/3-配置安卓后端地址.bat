@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0.."

set "TARGET_FILE=app\build.gradle.kts"
if not exist "%TARGET_FILE%" (
    echo [失败] 未找到 %TARGET_FILE%
    goto end
)

echo ========================================
echo       配置安卓端后端地址(API_BASE_URL)
echo ========================================
echo.
echo 请选择运行方式：
echo [1] Android 模拟器（推荐，使用 10.0.2.2）
echo [2] 真机 / 局域网设备（输入电脑 IP）
echo [3] 仅查看当前配置
echo.
set /p MODE=请输入选项编号: 

if "%MODE%"=="3" goto show

if "%MODE%"=="1" (
    set "NEW_URL=http://10.0.2.2:8080/"
    goto update
)

if "%MODE%"=="2" (
    set /p LAN_IP=请输入你电脑的局域网 IP（如 192.168.1.10）: 
    if "%LAN_IP%"=="" (
        echo [失败] 你没有输入 IP
        goto end
    )
    set "NEW_URL=http://%LAN_IP%:8080/"
    goto update
)

echo [失败] 无效选项
goto end

:update
powershell -NoProfile -ExecutionPolicy Bypass -Command "$path='app\\build.gradle.kts'; $content=Get-Content -Raw -Encoding UTF8 $path; $pattern='buildConfigField\(""String"",\s*""API_BASE_URL"",\s*""[^""]*""\)'; $replacement='buildConfigField(""String"", ""API_BASE_URL"", ""\"' + $env:NEW_URL + '\""")'; $updated=[regex]::Replace($content,$pattern,$replacement,1); if($updated -eq $content){ Write-Error '未找到 API_BASE_URL 配置'; exit 1 } ; Set-Content -Encoding UTF8 $path $updated;"
if not %errorlevel%==0 (
    echo [失败] 更新 API_BASE_URL 失败
    goto end
)
echo.
echo [成功] 已将 API_BASE_URL 设置为 %NEW_URL%
goto show

:show
echo.
echo 当前 API_BASE_URL 配置如下：
powershell -NoProfile -ExecutionPolicy Bypass -Command "Select-String -Path 'app\\build.gradle.kts' -Pattern 'API_BASE_URL' | ForEach-Object { $_.Line }"

:end
endlocal
