# Nebula Studio - 接口测试脚本 (PowerShell)
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResultFile = Join-Path $ProjectDir 'test_result.txt'
$BackendUrl = 'http://localhost:8080'
$FrontendUrl = 'http://localhost:3000'

$pass = 0
$fail = 0
$skip = 0

function Write-Result($name, $status, $detail = '') {
    $icon = switch ($status) {
        'pass' { '[通过]'; $script:pass++ }
        'fail' { '[失败]'; $script:fail++ }
        'skip' { '[跳过]'; $script:skip++ }
    }
    $color = switch ($status) { 'pass' { 'Green' } 'fail' { 'Red' } 'skip' { 'Yellow' } }
    $msg = "$icon $name"
    if ($detail) { $msg += " - $detail" }
    Write-Host "  $msg" -ForegroundColor $color
    Add-Content -Path $ResultFile -Value $msg -Encoding UTF8
}

# 初始化结果文件
@"
============================================
Nebula Studio - 接口测试报告
测试时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
============================================
"@ | Set-Content -Path $ResultFile -Encoding UTF8

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Nebula Studio - 接口测试" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ---------- 1. 后端健康检查 ----------
Write-Host "[1/6] 后端健康检查..." -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "$BackendUrl/api/auth/login" -Method GET -UseBasicParsing -TimeoutSec 5
    Write-Result '后端服务运行' 'pass' "HTTP $($r.StatusCode)"
} catch {
    Write-Result '后端服务运行' 'fail' $_.Exception.Message
}

# ---------- 2. 前端健康检查 ----------
Write-Host "[2/6] 前端健康检查..." -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri $FrontendUrl -UseBasicParsing -TimeoutSec 5
    Write-Result '前端服务运行' 'pass' "HTTP $($r.StatusCode)"
} catch {
    Write-Result '前端服务运行' 'fail' $_.Exception.Message
}

# ---------- 3. 注册接口测试 ----------
Write-Host "[3/6] 注册接口测试..." -ForegroundColor Yellow
try {
    $regBody = @{
        email    = "test_$(Get-Random)@nebula.studio"
        password = 'Test123456'
        username = "testuser_$(Get-Random)"
    } | ConvertTo-Json
    $regResp = Invoke-RestMethod -Uri "$BackendUrl/api/auth/register" -Method POST -ContentType 'application/json' -Body $regBody
    if ($regResp.code -eq 200 -or $regResp.code -eq 0) {
        Write-Result '注册接口' 'pass'
    } else {
        Write-Result '注册接口' 'fail' "code=$($regResp.code), msg=$($regResp.message)"
    }
} catch {
    Write-Result '注册接口' 'fail' $_.Exception.Message
}

# ---------- 4. 登录接口测试 ----------
Write-Host "[4/6] 登录接口测试..." -ForegroundColor Yellow
$script:Token = $null
try {
    $loginBody = @{ email = 'admin@nebula.studio'; password = 'admin123' } | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Uri "$BackendUrl/api/auth/login" -Method POST -ContentType 'application/json' -Body $loginBody
    if ($loginResp.data.token) {
        $script:Token = $loginResp.data.token
        Write-Result '登录接口' 'pass' "Token 获取成功"
    } else {
        Write-Result '登录接口' 'fail' '未返回 Token'
    }
} catch {
    Write-Result '登录接口' 'fail' $_.Exception.Message
}

# ---------- 5. 用户信息接口 ----------
Write-Host "[5/6] 用户信息接口测试..." -ForegroundColor Yellow
if ($script:Token) {
    try {
        $profileResp = Invoke-RestMethod -Uri "$BackendUrl/api/user/profile" -Headers @{ Authorization = "Bearer $($script:Token)" }
        Write-Result '用户信息接口' 'pass' "用户: $($profileResp.data.username // $profileResp.data.email)"
    } catch {
        Write-Result '用户信息接口' 'fail' $_.Exception.Message
    }
} else {
    Write-Result '用户信息接口' 'skip' '无 Token'
}

# ---------- 6. 风格列表接口 ----------
Write-Host "[6/6] 风格列表接口测试..." -ForegroundColor Yellow
if ($script:Token) {
    try {
        $styleResp = Invoke-RestMethod -Uri "$BackendUrl/api/styles/active" -Headers @{ Authorization = "Bearer $($script:Token)" }
        Write-Result '风格列表接口' 'pass' "数据返回正常"
    } catch {
        Write-Result '风格列表接口' 'fail' $_.Exception.Message
    }
} else {
    Write-Result '风格列表接口' 'skip' '无 Token'
}

# ---------- 汇总 ----------
$summary = @"

============================================
测试汇总: 通过=$pass, 失败=$fail, 跳过=$skip
============================================
"@
Add-Content -Path $ResultFile -Value $summary -Encoding UTF8

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  测试汇总: 通过=$pass, 失败=$fail, 跳过=$skip" -ForegroundColor Green
Write-Host "  结果已保存到: $ResultFile" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
