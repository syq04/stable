@echo off
chcp 65001 >nul 2>&1
title Nebula Studio - 停止

echo ============================================
echo   Nebula Studio - 停止服务
echo ============================================
echo.

:: 停止后端 (端口 8080)
echo [1/2] 停止后端服务 (端口 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    echo   终止进程 PID: %%a
    taskkill /PID %%a /F >nul 2>&1
)

:: 停止前端 (端口 3000)
echo [2/2] 停止前端服务 (端口 3000)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000 " ^| findstr "LISTENING"') do (
    echo   终止进程 PID: %%a
    taskkill /PID %%a /F >nul 2>&1
)

:: 也尝试通过窗口标题关闭
taskkill /FI "WINDOWTITLE eq Nebula-Backend*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Nebula-Frontend*" /F >nul 2>&1

echo.
echo 所有服务已停止。
pause
