@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo ========================================
echo          检查后端服务状态
echo ========================================
echo.

echo [1/2] 检查 8080 端口是否监听...
netstat -an | findstr ":8080" | findstr "LISTENING"
if %errorlevel%==0 (
    echo [成功] 检测到 8080 正在监听
) else (
    echo [失败] 未检测到 8080 监听
)
echo.

echo [2/2] 测试健康检查地址...
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r=Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/health -TimeoutSec 5; Write-Host '[成功] /api/health 可访问'; Write-Host ('状态码: ' + $r.StatusCode) } catch { Write-Host '[失败] 无法访问 http://localhost:8080/api/health' }"

endlocal
