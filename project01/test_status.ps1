$ErrorActionPreference = 'Stop'
try {
    $body = @{email='admin@nebula.studio';password='admin123'} | ConvertTo-Json
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body
    $token = $r.data.token
    $s = Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/status' -Headers @{Authorization="Bearer $token"}
    $s | ConvertTo-Json -Depth 10 | Set-Content -Path 'd:\dev\daodao\202605\project01\ai_status.txt' -Encoding UTF8
} catch {
    $_.Exception.Message | Set-Content -Path 'd:\dev\daodao\202605\project01\error.txt' -Encoding UTF8
}
