@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title Nebula Studio - Stop All

set "ROOT=%~dp0"

echo ============================================
echo   Nebula Studio - Stop All Services
echo ============================================
echo.

echo [*] Inference Service (5000)...
call :kill_port 5000

echo [*] Backend (8080)...
call :kill_port 8080
taskkill /FI "IMAGENAME eq java.exe" /F >nul 2>&1
del /f "%ROOT%backend\data\nebula_studio.lock.db" 2>nul

echo [*] Frontend (3000)...
call :kill_port 3000

echo [*] Frontend alt (3001)...
call :kill_port 3001

echo [*] Window title cleanup...
taskkill /FI "WINDOWTITLE eq InferenceService*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Nebula-Backend*"   /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Nebula-Frontend*"  /F >nul 2>&1

echo.
echo ============================================
echo   All services stopped.
echo ============================================
timeout /t 2 /nobreak >nul 2>&1
exit /b 0

:kill_port
set "port=%~1"
set "killed=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":%port% " ^| findstr /c:"LISTENING"') do (
    echo   Killing PID %%a on port %port%
    taskkill /f /pid %%a >nul 2>&1
    if not errorlevel 1 set /a killed+=1
)
if not "!killed!"=="0" (
    echo   Killed !killed! processes
) else (
    echo   No process found
)
exit /b 0
