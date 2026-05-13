import urllib.request, json, sys, time
time.sleep(2)
try:
    d = json.dumps({'email':'admin@nebula.studio','password':'admin123'}).encode()
    r = urllib.request.Request('http://localhost:8080/api/auth/login', data=d, headers={'Content-Type':'application/json'})
    resp = json.loads(urllib.request.urlopen(r, timeout=10).read())
    t = resp['data']['token']
    r2 = urllib.request.Request('http://localhost:8080/api/ai/status', headers={'Authorization':'Bearer '+t})
    s = json.loads(urllib.request.urlopen(r2, timeout=10).read())
    with open('d:/dev/daodao/202605/project01/backend/result.txt','w',encoding='utf-8') as f:
        f.write(json.dumps(s,indent=2,ensure_ascii=False))
except Exception as e:
    with open('d:/dev/daodao/202605/project01/backend/result.txt','w',encoding='utf-8') as f:
        f.write(f"ERROR:{e}")
