@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title Nebula Studio - Start All

set "ROOT=%~dp0"

echo.
echo   =============================================
echo     Nebula Studio - Start All Services
echo   =============================================
echo.

:: ============================================================
:: Phase 1 - Cleanup
:: ============================================================
echo   [1/5] Stopping old instances...

call :kill_port 5000
call :kill_port 8080
call :kill_port 3000
call :kill_port 3001
del /f "%ROOT%backend\data\nebula_studio.lock.db" 2>nul
echo         Done.

:: ============================================================
:: Phase 2 - Inference Service (5000)
:: ============================================================
echo.
echo   [2/5] Inference Service ^(5000^)

if not exist "%ROOT%inference-service\main.py" (
    echo         SKIP: inference-service\main.py not found
    goto :backend
)

cd /d "%ROOT%inference-service"

echo         Checking dependencies (torch ~2.5GB, one-time download)...
call pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124
if !errorlevel! neq 0 (
    echo         WARN: torch install failed, retrying once...
    call pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124
)
call pip install -r requirements.txt

    echo         Launching...
start "InferenceService" /D "%ROOT%inference-service" cmd /c "set CUDA_LAUNCH_BLOCKING=1 && python main.py"

echo         Loading model to GPU (7.7 GB, may take 2-5 min)...
call :wait "http://127.0.0.1:5000/health" 300
if !errorlevel! equ 0 (
    echo         READY
) else (
    echo         TIMEOUT - check http://127.0.0.1:5000/health
)

:: ============================================================
:: Phase 3 - Backend (8080)
:: ============================================================
:backend
echo.
echo   [3/5] Backend ^(8080^)

if not exist "%ROOT%backend\pom.xml" (
    echo         SKIP: backend\pom.xml not found
    goto :frontend
)

cd /d "%ROOT%backend"

set "JAR="
for %%f in (target\*.jar) do if not "%%~nxf"=="original-*" set "JAR=%%f"

if defined JAR (
    echo         Starting from JAR: !JAR!...
    start "Nebula-Backend" /D "%ROOT%backend" java -jar "%ROOT%backend\!JAR!" --spring.profiles.active=h2
) else (
    echo         No JAR found, starting from Maven ^(slower^)...
    start "Nebula-Backend" /D "%ROOT%backend" cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=h2"
)

echo         Waiting (Spring Boot startup, ~30-90s)...
call :wait "http://localhost:8080" 180
if !errorlevel! equ 0 (
    echo         READY
) else (
    echo         TIMEOUT - check http://localhost:8080
)

:: ============================================================
:: Phase 4 - Frontend (3000)
:: ============================================================
:frontend
echo.
echo   [4/5] Frontend ^(3000^)

if not exist "%ROOT%frontend\package.json" (
    echo         SKIP: frontend\package.json not found
    goto :done
)

if not exist "%ROOT%frontend\node_modules\" (
    echo         Installing npm dependencies...
    cd /d "%ROOT%frontend"
    call npm install
)

echo         Launching...
start "Nebula-Frontend" /D "%ROOT%frontend" cmd /c "npm run dev"

echo         Waiting...
call :wait "http://localhost:3000" 45
if !errorlevel! equ 0 (
    echo         READY
) else (
    echo         TIMEOUT - check http://localhost:3000
)

:: ============================================================
:: Phase 5 - Summary
:: ============================================================
:done
echo.
echo   =============================================
echo     Inference      http://127.0.0.1:5000/health
echo     Backend API    http://localhost:8080
echo     Frontend       http://localhost:3000
echo.
echo     Login:         admin@nebula.com / admin123
echo   =============================================
echo.
echo   All done. Press any key to close.
pause >nul
exit /b 0

:: ============================================================
:: Functions
:: ============================================================

:wait
:: args: URL, timeout_seconds
:: returns: 0 if URL responds within timeout, 1 otherwise
setlocal
set "url=%~1"
set "max=%~2"
set "elapsed=0"
:wait_poll
curl -s --connect-timeout 3 --max-time 5 -o nul "%url%" 2>nul
if !errorlevel! equ 0 exit /b 0
<nul set /p ="."
timeout /t 3 /nobreak >nul
set /a elapsed+=3
if !elapsed! lss !max! goto wait_poll
echo.
exit /b 1

:kill_port
:: args: port_number
setlocal
set "port=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":%port% " ^| findstr /c:"LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)
exit /b 0
