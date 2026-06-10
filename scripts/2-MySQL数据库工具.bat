@echo off

chcp 65001 >nul

setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0.."

title MySQL 数据库工具



set "MYSQL_EXE=D:\BtSoft\mysql\MySQL5.5\bin\mysql.exe"

set "MYSQLD_EXE=D:\BtSoft\mysql\MySQL5.5\bin\mysqld.exe"

set "MYSQL_INI=D:\BtSoft\mysql\MySQL5.5\my.ini"



:menu

cls

echo ========================================

echo         MySQL 数据库工具

echo ========================================

echo.

echo 目标数据库: android

echo 默认服务名: mysql80

echo.

echo [1] 检查 3306 端口占用

echo [2] 启动 MySQL 服务

echo [3] 停止 MySQL 服务

echo [4] 删除 MySQL 服务（需管理员）

echo [5] 登录 MySQL（root）

echo [6] 创建 android 数据库并导入 database_schema.sql

echo [0] 返回

echo.

set /p choice=请输入选项编号: 

if "%choice%"=="1" goto check_port

if "%choice%"=="2" goto start_service

if "%choice%"=="3" goto stop_service

if "%choice%"=="4" goto delete_service

if "%choice%"=="5" goto login_mysql

if "%choice%"=="6" goto init_db

if "%choice%"=="0" goto end

echo.

echo [提示] 无效选项

pause

goto menu



:detect_service

set "MYSQL_SERVICE="

for %%S in (mysql80 MySQL80 mysql) do (

    sc.exe query "%%S" >nul 2>nul

    if not errorlevel 1 if not defined MYSQL_SERVICE set "MYSQL_SERVICE=%%S"

)

exit /b 0



:resolve_mysql_exe

if exist "%MYSQL_EXE%" exit /b 0

for %%I in (mysql.exe) do set "FOUND_MYSQL=%%~$PATH:I"

if defined FOUND_MYSQL set "MYSQL_EXE=%FOUND_MYSQL%"

exit /b 0



:check_port

cls

echo ========================================

echo           检查 3306 端口占用

echo ========================================

echo.

set "FOUND="

for /f "tokens=5" %%i in ('netstat -ano ^| findstr ":3306" ^| findstr "LISTENING"') do (

    set "FOUND=1"

    echo [信息] 3306 端口正在被 PID %%i 占用

)

if not defined FOUND (

    echo [成功] 当前没有检测到 3306 端口监听

) else (

    echo.

    echo 你可以打开任务管理器，根据 PID 查看对应进程。

)

echo.

pause

goto menu



:start_service

cls

echo ========================================

echo          启动 MySQL 服务

echo ========================================

echo.

echo [提示] 建议使用管理员权限运行此脚本

call :detect_service

if not defined MYSQL_SERVICE (

    echo [失败] 未找到可用的 MySQL Windows 服务

    echo        已尝试识别：mysql80、MySQL80、mysql

    echo.

    pause

    goto menu

)

echo [信息] 已识别服务名：%MYSQL_SERVICE%

echo [信息] 正在执行：net start %MYSQL_SERVICE%

echo.

net.exe start "%MYSQL_SERVICE%"

set "START_ERROR=%errorlevel%"

echo.

if not "%START_ERROR%"=="0" (

    echo [失败] 启动服务失败，错误码：%START_ERROR%

    echo.

    echo [信息] 当前服务配置如下：

    sc.exe qc "%MYSQL_SERVICE%"

) else (

    echo [成功] MySQL 服务已启动

)

echo.

pause

goto menu



:stop_service

cls

echo ========================================

echo          停止 MySQL 服务

echo ========================================

echo.

echo [提示] 建议使用管理员权限运行此脚本

call :detect_service

if not defined MYSQL_SERVICE (

    echo [失败] 未找到可用的 MySQL Windows 服务

    echo.

    pause

    goto menu

)

echo [信息] 已识别服务名：%MYSQL_SERVICE%

net.exe stop "%MYSQL_SERVICE%"

echo.

pause

goto menu



:delete_service

cls

echo ========================================

echo          删除 MySQL 服务

echo ========================================

echo.

echo [警告] 该操作会删除识别到的 MySQL Windows 服务

echo [提示] 请先确认它确实是冲突或无效服务，并使用管理员权限运行

call :detect_service

if not defined MYSQL_SERVICE (

    echo [失败] 未找到可用的 MySQL Windows 服务，无需删除

    echo.

    pause

    goto menu

)

echo [信息] 已识别服务名：%MYSQL_SERVICE%

choice /c YN /m "确认继续删除 %MYSQL_SERVICE% 服务吗"

if errorlevel 2 goto menu

net.exe stop "%MYSQL_SERVICE%"

sc.exe delete "%MYSQL_SERVICE%"

echo.

pause

goto menu



:login_mysql

cls

echo ========================================

echo         登录 MySQL

echo ========================================

echo.

call :resolve_mysql_exe

if not exist "%MYSQL_EXE%" (

    echo [失败] 未找到 mysql.exe

    echo        默认路径：D:\BtSoft\mysql\MySQL5.5\bin\mysql.exe

    echo        也没有在 PATH 中找到 mysql.exe

    echo.

    pause

    goto menu

)

echo [信息] 正在使用：%MYSQL_EXE%

echo.

"%MYSQL_EXE%" -u root -p

echo.

pause

goto menu



:init_db

cls

echo ========================================

echo    创建 android 数据库并导入表结构

echo ========================================

echo.

call :resolve_mysql_exe

if not exist "%MYSQL_EXE%" (

    echo [失败] 未找到 mysql.exe

    echo.

    pause

    goto menu

)

if not exist "database_schema.sql" (

    echo [失败] 未找到 database_schema.sql

    echo.

    pause

    goto menu

)

set /p db_user=请输入 MySQL 用户名（默认 root）: 

if "%db_user%"=="" set "db_user=root"

set /p db_pass=请输入 MySQL 密码: 

set /p db_host=请输入 MySQL 主机（默认 localhost）: 

if "%db_host%"=="" set "db_host=localhost"

set /p db_port=请输入 MySQL 端口（默认 3306）: 

if "%db_port%"=="" set "db_port=3306"

echo.

echo [信息] 正在创建数据库 android...

"%MYSQL_EXE%" -h "%db_host%" -P "%db_port%" -u "%db_user%" -p%db_pass% -e "CREATE DATABASE IF NOT EXISTS android DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if errorlevel 1 (

    echo.

    echo [失败] 创建数据库失败，请检查用户名、密码、主机和端口

    echo.

    pause

    goto menu

)

echo [信息] 正在导入 database_schema.sql...

"%MYSQL_EXE%" -h "%db_host%" -P "%db_port%" -u "%db_user%" -p%db_pass% android < database_schema.sql

if errorlevel 1 (

    echo.

    echo [失败] 导入表结构失败

) else (

    echo.

    echo [成功] android 数据库初始化完成

)

echo.

pause

goto menu



:end

endlocal

exit /b 0

