@echo off
chcp 65001 >nul 2>&1
title Nebula Studio - 管理面板

:menu
echo.
echo ============================================
echo   Nebula Studio - 文图互转主题设计系统
echo   管理面板
echo ============================================
echo.
echo   1. 启动项目 (后端 + 前端)
echo   2. 停止项目
echo   3. 重启项目
echo   4. 构建项目
echo   5. 运行测试
echo   6. 查看后端日志
echo   7. 查看服务状态
echo   0. 退出
echo.
set /p choice=请选择操作:

if "%choice%"=="1" goto start
if "%choice%"=="2" goto stop
if "%choice%"=="3" goto restart
if "%choice%"=="4" goto build
if "%choice%"=="5" goto test
if "%choice%"=="6" goto logs
if "%choice%"=="7" goto status
if "%choice%"=="0" exit /b 0
echo 无效选择
goto menu

:start
call "%~dp0start.bat"
goto menu

:stop
call "%~dp0stop.bat"
goto menu

:restart
echo 正在停止服务...
call "%~dp0stop.bat"
timeout /t 3 /nobreak >nul
echo 正在启动服务...
call "%~dp0start.bat"
goto menu

:build
call "%~dp0build.bat"
goto menu

:test
call "%~dp0test.bat"
goto menu

:logs
echo.
echo ========== 后端日志 (最后 50 行) ==========
if exist "%~dp0backend_stdout.log" (
    powershell -Command "Get-Content '%~dp0backend_stdout.log' -Tail 50"
) else (
    echo   无后端日志文件
)
echo.
pause
goto menu

:status
echo.
echo ========== 服务状态 ==========
echo.
echo [后端 - 端口 8080]
netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo   状态: 运行中
) else (
    echo   状态: 未运行
)
echo.
echo [前端 - 端口 3000]
netstat -ano | findstr ":3000 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo   状态: 运行中
) else (
    echo   状态: 未运行
)
echo.
pause
goto menu
