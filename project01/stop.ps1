# Nebula Studio - 停止脚本 (PowerShell)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ProjectDir '.pids.json'

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Nebula Studio - 停止服务" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 方式1: 通过 PID 文件停止
if (Test-Path $PidFile) {
    Write-Host "从 .pids.json 读取 PID 信息..." -ForegroundColor Yellow
    $pids = Get-Content $PidFile | ConvertFrom-Json
    if ($pids.backend -and $pids.backend -ne 0) {
        Write-Host "  停止后端进程 PID: $($pids.backend)"
        Stop-Process -Id $pids.backend -Force -ErrorAction SilentlyContinue
    }
    if ($pids.frontend -and $pids.frontend -ne 0) {
        Write-Host "  停止前端进程 PID: $($pids.frontend)"
        Stop-Process -Id $pids.frontend -Force -ErrorAction SilentlyContinue
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# 方式2: 通过端口查找并停止
Write-Host ""
Write-Host "[1/2] 停止后端服务 (端口 8080)..." -ForegroundColor Yellow
$backendConns = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
foreach ($conn in $backendConns) {
    Write-Host "  终止进程 PID: $($conn.OwningProcess)"
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}

Write-Host "[2/2] 停止前端服务 (端口 3000)..." -ForegroundColor Yellow
$frontendConns = Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
foreach ($conn in $frontendConns) {
    Write-Host "  终止进程 PID: $($conn.OwningProcess)"
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "所有服务已停止。" -ForegroundColor Green
