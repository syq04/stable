@echo off
chcp 65001 >nul 2>&1
title Nebula Studio - 测试

echo ============================================
echo   Nebula Studio - 接口测试
echo ============================================
echo.

set "PROJECT_DIR=%~dp0"
set "RESULT_FILE=%PROJECT_DIR%test_result.txt"

echo 测试结果 > "%RESULT_FILE%"
echo 测试时间: %date% %time% >> "%RESULT_FILE%"
echo ============================================ >> "%RESULT_FILE%"
echo.

:: ---------- 1. 后端健康检查 ----------
echo [1/4] 后端健康检查 (端口 8080)...
curl -s -o nul -w "HTTP%%{http_code}" http://localhost:8080/api/auth/login >nul 2>&1
if %errorlevel%==0 (
    echo   [通过] 后端服务正常运行
    echo [通过] 后端服务正常运行 >> "%RESULT_FILE%"
) else (
    echo   [失败] 后端服务未响应
    echo [失败] 后端服务未响应 >> "%RESULT_FILE%"
)

:: ---------- 2. 前端健康检查 ----------
echo [2/4] 前端健康检查 (端口 3000)...
curl -s -o nul -w "HTTP%%{http_code}" http://localhost:3000 >nul 2>&1
if %errorlevel%==0 (
    echo   [通过] 前端服务正常运行
    echo [通过] 前端服务正常运行 >> "%RESULT_FILE%"
) else (
    echo   [失败] 前端服务未响应
    echo [失败] 前端服务未响应 >> "%RESULT_FILE%"
)

:: ---------- 3. 登录接口测试 ----------
echo [3/4] 登录接口测试 (POST /api/auth/login)...
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@nebula.studio\",\"password\":\"admin123\"}" > "%PROJECT_DIR%login_resp.json" 2>nul
if exist "%PROJECT_DIR%login_resp.json" (
    findstr /C:"token" "%PROJECT_DIR%login_resp.json" >nul 2>&1
    if %errorlevel%==0 (
        echo   [通过] 登录接口正常
        echo [通过] 登录接口正常 >> "%RESULT_FILE%"
    ) else (
        echo   [失败] 登录接口返回异常
        echo [失败] 登录接口返回异常 >> "%RESULT_FILE%"
    )
) else (
    echo   [失败] 登录接口无响应
    echo [失败] 登录接口无响应 >> "%RESULT_FILE%"
)

:: ---------- 4. 受保护接口测试 ----------
echo [4/4] 受保护接口测试 (GET /api/user/profile)...
:: 提取 token
for /f "delims=" %%t in ('powershell -Command "(Get-Content '%PROJECT_DIR%login_resp.json' | ConvertFrom-Json).data.token" 2^>nul') do set "TOKEN=%%t"
if defined TOKEN (
    curl -s -X GET http://localhost:8080/api/user/profile -H "Authorization: Bearer %TOKEN%" > "%PROJECT_DIR%profile_resp.json" 2>nul
    if exist "%PROJECT_DIR%profile_resp.json" (
        findstr /C:"email" "%PROJECT_DIR%profile_resp.json" >nul 2>&1
        if %errorlevel%==0 (
            echo   [通过] 用户信息接口正常
            echo [通过] 用户信息接口正常 >> "%RESULT_FILE%"
        ) else (
            echo   [失败] 用户信息接口返回异常
            echo [失败] 用户信息接口返回异常 >> "%RESULT_FILE%"
        )
    )
) else (
    echo   [跳过] 无法获取 Token
    echo [跳过] 无法获取 Token >> "%RESULT_FILE%"
)

echo.
echo ============================================ >> "%RESULT_FILE%"
echo.
echo 测试完成，结果已保存到: test_result.txt
echo.
type "%RESULT_FILE%"
echo.
pause
