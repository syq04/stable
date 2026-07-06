@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title Nebula Studio - Environment Setup

set "ROOT=%~dp0"

echo.
echo   ================================================
echo     Nebula Studio - Environment Setup
echo   ================================================
echo.
echo   This script will check/install all dependencies
echo   for the full project stack.
echo.
echo   Stack:
echo     Frontend  (Vue 3 + Vite)  - Node.js / npm
echo     Backend   (Spring Boot 3) - Java 17 / Maven
echo     Inference (FastAPI)       - Python 3.10+ / pip
echo     Database  (H2 embedded)   - No install required
echo.
echo   Time estimate:
echo     ~10 min on clean machine (depends on network)
echo     ~2.5 GB for PyTorch GPU (first pip install)
echo.
echo   ================================================
pause

:: ============================================================================
:: Phase 1 - Prerequisites Check
:: ============================================================================
echo.
echo.
echo   ========================================
echo   [1/6] Checking Prerequisites
echo   ========================================
echo.

set "PREREQ_OK=1"

:: --- Git ---
echo   [Git]
where git >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Please install from: https://git-scm.com/download/win
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('git --version') do echo         %%i  [OK]
)

:: --- Java 17+ ---
echo   [Java]
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Please install JDK 17+ from:
    echo                    https://adoptium.net/download/
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /c:"version"') do (
        set "javaver=%%i"
    )
    echo         !javaver!
    echo !javaver! | findstr /c:"17" >nul 2>&1
    if !errorlevel! neq 0 (
        echo         WARNING: Java 17 recommended, you have another version
    ) else (
        echo         [OK]
    )
)

:: --- Maven ---
echo   [Maven]
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Please install from: https://maven.apache.org/download.cgi
    echo                    Or use Maven Wrapper: backend\mvnw.cmd
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('mvn --version 2^>^&1 ^| findstr /c:"Apache Maven"') do echo         %%i  [OK]
)

:: --- Node.js ---
echo   [Node.js]
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Please install from: https://nodejs.org/ (LTS)
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('node --version') do echo         %%i  [OK]
)

:: --- npm ---
echo   [npm]
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Usually bundled with Node.js
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('npm --version') do echo         %%i  [OK]
)

:: --- Python ---
echo   [Python]
where python >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Please install from: https://www.python.org/downloads/
    echo                    Minimum version: 3.10
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('python --version 2^>^&1') do echo         %%i  [OK]
)

:: --- pip ---
echo   [pip]
python -m pip --version >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - Usually bundled with Python
    set "PREREQ_OK=0"
) else (
    for /f "tokens=*" %%i in ('python -m pip --version 2^>^&1') do echo         %%i  [OK]
)

:: --- CUDA / GPU (optional) ---
echo   [CUDA/GPU]
where nvidia-smi >nul 2>&1
if %errorlevel% neq 0 (
    echo         NOT FOUND - GPU inference will NOT be available
    echo                   PyTorch CPU-only will be installed
) else (
    for /f "tokens=*" %%i in ('nvidia-smi --query-gpu=name --format=csv,noheader 2^>nul') do echo         %%i  [OK]
)

echo.
if "!PREREQ_OK!"=="0" (
    echo   ========================================
    echo   WARNING: Some prerequisites are missing!
    echo   Please install the missing tools above,
    echo   then re-run this script.
    echo   ========================================
    echo.
    choice /c yn /m "Continue anyway? (y/n)"
    if !errorlevel! neq 1 exit /b 1
)

:: ============================================================================
:: Phase 2 - Frontend Dependencies
:: ============================================================================
echo.
echo.
echo   ========================================
echo   [2/6] Frontend Dependencies (npm)
echo   ========================================
echo.

if exist "%ROOT%frontend\package.json" (
    cd /d "%ROOT%frontend"

    if exist "node_modules\" (
        echo         node_modules\ already exists, skipping.
        echo         Run "npm install" manually if needed.
    ) else (
        echo         Running npm install...
        call npm install
        if !errorlevel! neq 0 (
            echo         ERROR: npm install failed
            echo         Try running manually: cd frontend ^&^& npm install
        ) else (
            echo         [OK] Frontend dependencies installed
        )
    )
    echo.
    echo         Running npm run build...
    call npm run build
    if !errorlevel! neq 0 (
        echo         WARNING: Build had issues (non-fatal for dev)
    ) else (
        echo         [OK] Frontend built to dist\
    )
) else (
    echo         SKIP: frontend\package.json not found
)

:: ============================================================================
:: Phase 3 - Inference Service Dependencies (Python)
:: ============================================================================
echo.
echo.
echo   ========================================
echo   [3/6] Inference Service (Python / pip)
echo   ========================================
echo.

if exist "%ROOT%inference-service\requirements.txt" (
    cd /d "%ROOT%inference-service"

    :: Check if CUDA is available
    where nvidia-smi >nul 2>&1
    if %errorlevel% equ 0 (
        set "HAS_GPU=1"
    ) else (
        set "HAS_GPU=0"
    )

    if "!HAS_GPU!"=="1" (
        echo         GPU detected. Installing PyTorch with CUDA 12.4...
        echo         (2.5 GB download, may take several minutes)
        python -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124
    ) else (
        echo         No GPU detected. Installing PyTorch CPU-only...
        echo         (may take several minutes)
        python -m pip install torch torchvision torchaudio
    )

    if !errorlevel! neq 0 (
        echo         WARNING: PyTorch install failed. Retrying once...
        if "!HAS_GPU!"=="1" (
            python -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124
        ) else (
            python -m pip install torch torchvision torchaudio
        )
        if !errorlevel! neq 0 (
            echo         ERROR: PyTorch install still failed
        )
    )

    echo.
    echo         Installing Python dependencies from requirements.txt...
    python -m pip install -r requirements.txt
    if !errorlevel! neq 0 (
        echo         WARNING: Some packages may have failed to install
    ) else (
        echo         [OK] Python dependencies installed
    )
) else (
    echo         SKIP: requirements.txt not found
)

:: ============================================================================
:: Phase 4 - Backend Build (Maven)
:: ============================================================================
echo.
echo.
echo   ========================================
echo   [4/6] Backend Build (Maven)
echo   ========================================
echo.

if exist "%ROOT%backend\pom.xml" (
    cd /d "%ROOT%backend"

    where mvn >nul 2>&1
    if %errorlevel% neq 0 (
        echo         Maven not found. Checking for Maven Wrapper...
        if exist "mvnw.cmd" (
            echo         Using Maven Wrapper (mvnw.cmd)
            set "MVN_CMD=mvnw.cmd"
        ) else (
            echo         ERROR: Maven is required for backend build
            echo         Install from: https://maven.apache.org/download.cgi
            goto :phase5
        )
    ) else (
        set "MVN_CMD=mvn"
    )

    echo         Running Maven clean package (skip tests)...
    echo         (downloads ~100 MB of dependencies, may take 5-10 min)
    echo.

    call !MVN_CMD! clean package -DskipTests
    if !errorlevel! neq 0 (
        echo.
        echo         ERROR: Maven build failed
        echo         Check error above and try manually:
        echo           cd backend ^&^& mvn clean package -DskipTests
    ) else (
        echo.
        echo         [OK] Backend built successfully
        echo         JAR location: backend\target\
        dir /b target\*.jar 2>nul
    )
) else (
    echo         SKIP: backend\pom.xml not found
)

:phase5
:: ============================================================================
:: Phase 5 - Model Files Check
:: ============================================================================
echo.
echo.
echo   ========================================
echo   [5/6] Model Files Check
echo   ========================================
echo.

set "MODEL_DIR=%ROOT%sd-models"
if exist "%MODEL_DIR%\" (
    set "MODEL_COUNT=0"
    for %%f in ("%MODEL_DIR%\*.safetensors") do set /a MODEL_COUNT+=1

    if !MODEL_COUNT! gtr 0 (
        echo         Found !MODEL_COUNT! .safetensors file(s):
        for %%f in ("%MODEL_DIR%\*.safetensors") do echo           %%~nxf
    ) else (
        echo         WARNING: No .safetensors files found in sd-models\
        echo         Please download a Stable Diffusion model:
        echo           https://huggingface.co/runwayml/stable-diffusion-v1-5
        echo         Place the .safetensors file in: %MODEL_DIR%
    )
) else (
    echo         WARNING: sd-models\ directory not found
    echo         Creating directory...
    mkdir "%MODEL_DIR%" 2>nul
    echo         Please download a Stable Diffusion model and place it here:
    echo           %MODEL_DIR%
)

:: ============================================================================
:: Phase 6 - Verification & Summary
:: ============================================================================
echo.
echo.
echo   ========================================
echo   [6/6] Verification ^& Summary
echo   ========================================
echo.

set "ALL_OK=1"

:: Verify frontend
echo   [Frontend]
if exist "%ROOT%frontend\node_modules\" (
    echo         node_modules  [OK]
) else (
    echo         node_modules  [MISSING]
    set "ALL_OK=0"
)
if exist "%ROOT%frontend\dist\" (
    echo         dist          [OK]
) else (
    echo         dist          [MISSING]
)

:: Verify inference
echo   [Inference]
python -c "import torch; print('        PyTorch', torch.__version__, '[OK]')" 2>nul || (
    echo         PyTorch       [MISSING]
    set "ALL_OK=0"
)
python -c "import diffusers; print('        Diffusers', diffusers.__version__, '[OK]')" 2>nul || (
    echo         Diffusers     [MISSING]
    set "ALL_OK=0"
)

:: Verify backend
echo   [Backend]
for %%f in ("%ROOT%backend\target\*.jar") do (
    if not "%%~nxf"=="original-*" (
        echo         JAR: %%~nxf  [OK]
        goto :jar_found
    )
)
echo         JAR           [MISSING]
set "ALL_OK=0"
:jar_found

:: ============================================================================
:: Final summary
:: ============================================================================
echo.
echo   ================================================
if "!ALL_OK!"=="1" (
    echo     SETUP COMPLETE - All dependencies ready!
    echo   ================================================
    echo.
    echo     You can now start the services:
    echo       start.bat      - Start all 3 services
    echo       dev.bat        - Developer panel
    echo.
    echo     Or run individually:
    echo       inference-service\: python main.py
    echo       backend\:          mvn spring-boot:run -Dspring-boot.run.profiles=h2
    echo       frontend\:         npm run dev
    echo.
    echo     Login: admin@nebula.com / admin123
) else (
    echo     SETUP COMPLETE with warnings (see above)
    echo   ================================================
    echo.
    echo     Some components may not be fully ready.
    echo     Review the output above and fix missing items.
    echo     Then run: start.bat
)

echo   ================================================
echo.
pause
exit /b 0
