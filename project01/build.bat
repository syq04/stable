@echo off
chcp 65001 >nul 2>&1
title Nebula Studio - 构建

echo ============================================
echo   Nebula Studio - 项目构建
echo ============================================
echo.

set "PROJECT_DIR=%~dp0"
set "BACKEND_DIR=%PROJECT_DIR%backend"
set "FRONTEND_DIR=%PROJECT_DIR%frontend"

:: ---------- 构建后端 ----------
echo [1/2] 构建后端 (Maven)...
cd /d "%BACKEND_DIR%"

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo   [错误] 未找到 mvn 命令，请安装 Maven
    pause
    exit /b 1
)

call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo   [错误] 后端构建失败
    pause
    exit /b 1
)
echo   [完成] 后端构建成功

:: ---------- 构建前端 ----------
echo.
echo [2/2] 构建前端 (Vite)...
cd /d "%FRONTEND_DIR%"

if not exist "node_modules\" (
    echo   执行 npm install...
    call npm install
    if %errorlevel% neq 0 (
        echo   [错误] npm install 失败
        pause
        exit /b 1
    )
)

call npm run build
if %errorlevel% neq 0 (
    echo   [错误] 前端构建失败
    pause
    exit /b 1
)
echo   [完成] 前端构建成功

echo.
echo ============================================
echo   构建完成!
echo   - 后端 JAR: backend\target\*.jar
echo   - 前端产物: frontend\dist\
echo ============================================
pause
