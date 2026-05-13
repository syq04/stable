import urllib.request, json, os
os.chdir(os.path.dirname(os.path.abspath(__file__)))
try:
    d = json.dumps({'email':'admin@nebula.studio','password':'admin123'}).encode()
    r = urllib.request.Request('http://localhost:8080/api/auth/login', data=d, headers={'Content-Type':'application/json'})
    resp = json.loads(urllib.request.urlopen(r).read())
    t = resp['data']['token']
    r2 = urllib.request.Request('http://localhost:8080/api/ai/status', headers={'Authorization':'Bearer '+t})
    s = json.loads(urllib.request.urlopen(r2).read())
    with open('result.txt','w',encoding='utf-8') as f:
        f.write(json.dumps(s,indent=2,ensure_ascii=False))
except Exception as e:
    with open('result.txt','w',encoding='utf-8') as f:
        f.write(f"ERROR:{e}")
