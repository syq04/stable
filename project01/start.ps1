# Nebula Studio - 启动脚本 (PowerShell)
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir  = Join-Path $ProjectDir 'backend'
$FrontendDir = Join-Path $ProjectDir 'frontend'

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Nebula Studio - 文图互转主题设计系统" -ForegroundColor Cyan
Write-Host "  启动脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ---------- 启动后端 ----------
Write-Host "[1/2] 启动后端服务 (Spring Boot, 端口 8080)..." -ForegroundColor Yellow

$jarFile = Get-ChildItem -Path (Join-Path $BackendDir 'target') -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike 'original-*' } |
    Select-Object -First 1

if ($jarFile) {
    Write-Host "  找到 JAR: $($jarFile.FullName)"
    Start-Process -FilePath 'java' -ArgumentList "-jar", $jarFile.FullName, '--spring.profiles.active=h2' `
        -WindowStyle Normal -PassThru | ForEach-Object { $script:BackendPid = $_.Id }
} else {
    Write-Host "  JAR 包不存在，使用 Maven 启动..."
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run', '-Dspring-boot.run.profiles=h2' `
            -WorkingDirectory $BackendDir -WindowStyle Normal -PassThru |
            ForEach-Object { $script:BackendPid = $_.Id }
    } else {
        Write-Host "  [错误] 未找到 mvn 命令，请安装 Maven 或先执行 mvn package 构建" -ForegroundColor Red
    }
}

Write-Host "  后端服务启动中，等待 10 秒..."
Start-Sleep -Seconds 10

# ---------- 启动前端 ----------
Write-Host ""
Write-Host "[2/2] 启动前端服务 (Vite, 端口 3000)..." -ForegroundColor Yellow

if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
    Write-Host "  node_modules 不存在，执行 npm install..."
    Push-Location $FrontendDir
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [错误] npm install 失败" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Pop-Location
}

Start-Process -FilePath 'npm' -ArgumentList 'run', 'dev' -WorkingDirectory $FrontendDir `
    -WindowStyle Normal -PassThru | ForEach-Object { $script:FrontendPid = $_.Id }

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  服务已启动:" -ForegroundColor Green
Write-Host "  - 后端 API:  http://localhost:8080" -ForegroundColor White
Write-Host "  - 前端页面:  http://localhost:3000" -ForegroundColor White
Write-Host "  - H2 控制台: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Green

# 保存 PID 到文件，方便停止
$pidInfo = @{
    backend  = if ($script:BackendPid) { $script:BackendPid } else { 0 }
    frontend = if ($script:FrontendPid) { $script:FrontendPid } else { 0 }
}
$pidInfo | ConvertTo-Json | Set-Content -Path (Join-Path $ProjectDir '.pids.json') -Encoding UTF8
Write-Host "  PID 信息已保存到 .pids.json"
