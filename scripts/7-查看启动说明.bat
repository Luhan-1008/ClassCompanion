@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo ========================================
echo              启动说明
echo ========================================
echo.
echo 推荐启动顺序：
echo.
echo 1. 先运行 scripts\1-检查环境.bat
echo 2. 第一次使用先运行 scripts\2-MySQL数据库工具.bat
echo 3. 运行 scripts\3-配置安卓后端地址.bat
echo    - 模拟器选 10.0.2.2
echo    - 真机选你电脑的局域网 IP
echo 4. 运行 scripts\4-启动后端.bat
echo 5. 运行 scripts\5-检查后端状态.bat
echo 6. 运行 scripts\6-打开AndroidStudio.bat
echo 7. 在 Android Studio 中运行 app 模块
echo.
echo 验证方式：
echo - 先注册账号
echo - 再登录
echo - 换一个模拟器再次登录，验证账号是否保存在 MySQL
echo.
echo 常见问题：
echo - 后端起不来：先检查 MySQL 是否启动
echo - App 连不上后端：确认 API_BASE_URL 是否正确
echo - 模拟器无法访问 localhost：请使用 10.0.2.2
echo.
endlocal
