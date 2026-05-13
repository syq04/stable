$r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body '{"email":"admin@nebula.studio","password":"admin123"}' -UseBasicParsing
$resp = $r.Content | ConvertFrom-Json
$token = $resp.data.token

$statusR = Invoke-WebRequest -Uri 'http://localhost:8080/api/ai/status' -Method GET -Headers @{Authorization="Bearer $token"} -UseBasicParsing
$statusR.Content | Out-File -FilePath 'd:\dev\daodao\202605\project01\ai_status.txt' -Encoding utf8
