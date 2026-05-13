@echo off
chcp 65001 >nul 2>&1
title Nebula Studio - 启动

echo ============================================
echo   Nebula Studio - 文图互转主题设计系统
echo   启动脚本
echo ============================================
echo.

:: 项目根目录
set "PROJECT_DIR=%~dp0"
set "BACKEND_DIR=%PROJECT_DIR%backend"
set "FRONTEND_DIR=%PROJECT_DIR%frontend"

:: ---------- 启动后端 ----------
echo [1/2] 启动后端服务 (Spring Boot, 端口 8080)...
cd /d "%BACKEND_DIR%"

:: 检查是否已有 jar 包
set "JAR_FILE="
for %%f in (target\*.jar) do (
    if not "%%~nxf"=="original-*" set "JAR_FILE=target\%%~nxf"
)

if defined JAR_FILE (
    echo   找到 JAR: %JAR_FILE%
    start "Nebula-Backend" java -jar %JAR_FILE% --spring.profiles.active=h2
) else (
    echo   JAR 包不存在，使用 Maven 启动...
    where mvn >nul 2>&1
    if %errorlevel%==0 (
        start "Nebula-Backend" cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=h2"
    ) else (
        echo   [错误] 未找到 mvn 命令，请安装 Maven 或先执行 mvn package 构建
        echo   也可以手动构建: cd backend ^&^& mvn package -DskipTests
        goto :frontend
    )
)

echo   后端服务启动中，等待 20 秒...
timeout /t 20 /nobreak >nul

:frontend
:: ---------- 启动前端 ----------
echo.
echo [2/2] 启动前端服务 (Vite, 端口 3000)...
cd /d "%FRONTEND_DIR%"

:: 检查 node_modules
if not exist "node_modules\" (
    echo   node_modules 不存在，执行 npm install...
    call npm install
    if %errorlevel% neq 0 (
        echo   [错误] npm install 失败
        pause
        exit /b 1
    )
)

start "Nebula-Frontend" cmd /c "npm run dev"

echo.
echo ============================================
echo   服务已启动:
echo   - 后端 API:  http://localhost:8080
echo   - 前端页面:  http://localhost:3000
echo   - H2 控制台: http://localhost:8080/h2-console
echo ============================================
echo.
echo 按任意键退出此窗口(服务将继续运行)...
pause >nul
