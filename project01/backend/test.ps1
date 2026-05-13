try {
    $body = @{email='admin@nebula.studio';password='admin123'} | ConvertTo-Json
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body
    $token = $r.data.token
    "TOKEN:$token" | Out-File '.\result.txt' -Encoding utf8
    $s = Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/status' -Headers @{Authorization="Bearer $token"}
    $s | ConvertTo-Json -Depth 10 | Out-File '.\result.txt' -Append -Encoding utf8
} catch {
    "ERROR:$($_.Exception.Message)" | Out-File '.\result.txt' -Encoding utf8
}
