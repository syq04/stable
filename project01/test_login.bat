@echo off
powershell -ExecutionPolicy Bypass -Command "(Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body '{\"email\":\"admin@nebula.studio\",\"password\":\"admin123\"}').data.token | Set-Content -Path 'd:\dev\daodao\202605\project01\token.txt' -Encoding UTF8"
