@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo 正在尝试打开 Android Studio...
set "AS1=%ProgramFiles%\Android\Android Studio\bin\studio64.exe"
set "AS2=%LocalAppData%\Programs\Android Studio\bin\studio64.exe"

if exist "%AS1%" (
    start "Android Studio" "%AS1%" "%cd%"
    goto end
)

if exist "%AS2%" (
    start "Android Studio" "%AS2%" "%cd%"
    goto end
)

echo [失败] 未自动找到 Android Studio。
echo 请手动打开 Android Studio，然后打开当前项目目录：
echo %cd%

:end
endlocal
