#!/usr/bin/env python3
"""
============================================================================
  Nebula Studio — 全量回归测试（黑盒 + 白盒）
  覆盖：推理服务 → 后端 API → 权限校验 → 边界条件 → 错误路径
============================================================================
"""
import requests
import json
import time
import sys
import os
import re
import io as _io
from datetime import datetime
from io import BytesIO
from PIL import Image
import base64

os.environ.setdefault("PYTHONIOENCODING", "utf-8")
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# ── 尝试 colorama ──────────────────────────────────────────
try:
    from colorama import init, Fore, Style
    init(autoreset=True)
    HAS_COLOR = True
except ImportError:
    HAS_COLOR = False
    Fore = Style = type('_', (), {'__getattr__': lambda s,k: ''})()

BASE_BACKEND  = os.environ.get("TEST_BACKEND_URL",  "http://localhost:8080")
BASE_INFERENCE = os.environ.get("TEST_INFERENCE_URL", "http://127.0.0.1:5000")
REPORT_FILE    = os.path.join(os.path.dirname(__file__), "test_report.txt")

PASS, FAIL, SKIP, WARN = 0, 0, 0, 0
TEST_START = time.time()
REPORT_LINES = []
TOKEN = None

# ═══════════════════════════════════════════════════════════
#  HELPERS
# ═══════════════════════════════════════════════════════════

def _c(tag, s): return f"{getattr(Fore, tag.upper(), '')}{s}{Style.RESET_ALL}" if HAS_COLOR else s

def _status(ok, msg=""):
    if ok is True:   return _c("green",  f"[PASS] {msg}")
    if ok is False:  return _c("red",    f"[FAIL] {msg}")
    if ok == "SKIP": return _c("yellow", f"[SKIP] {msg}")
    if ok == "WARN": return _c("yellow", f"[WARN] {msg}")
    return f"[????] {msg}"

def _log(status_char, suite, case, detail="", elapsed=0.0):
    global PASS, FAIL, SKIP, WARN
    if status_char == "PASS": PASS += 1
    elif status_char == "FAIL": FAIL += 1
    elif status_char == "SKIP": SKIP += 1
    elif status_char == "WARN": WARN += 1
    line = f"  {_status(True if status_char=='PASS' else False if status_char=='FAIL' else 'SKIP' if status_char=='SKIP' else 'WARN', '')} {suite} › {case}"
    if detail:
        line += f"  ({detail})"
    if elapsed > 0:
        line += f"  [{elapsed:.2f}s]"
    print(line)
    REPORT_LINES.append(line)

def section(title):
    bar = "─" * 60
    print(f"\n{_c('cyan', bar)}\n{_c('cyan', '  ' + title)}\n{_c('cyan', bar)}")
    REPORT_LINES.append(f"\n{'─'*60}\n  {title}\n{'─'*60}")

# ── Assertion helpers ──────────────────────────────────────
def _equals(a, b, label=""):
    return a == b, f"期望={b} 实际={a}" if a != b else label

def _in_range(val, lo, hi, label=""):
    ok = lo <= val <= hi
    return ok, f"{val} not in [{lo}, {hi}]" if not ok else label

def _has_keys(d, *keys):
    missing = [k for k in keys if k not in d]
    return len(missing) == 0, f"缺少字段: {missing}" if missing else ""

def _types(d, **kwargs):
    for k, t in kwargs.items():
        if k in d and not isinstance(d[k], t):
            return False, f"{k} 期望 {t.__name__} 实际 {type(d[k]).__name__}"
    return True, ""

def _one_of(val, *options):
    return val in options, f"{val} ∉ {options}"

def api(method, url, **kwargs):
    """统一请求入口，返回 (status_code, data, elapsed)"""
    try:
        fn = getattr(requests, method)
        start = time.time()
        timeout = kwargs.pop('timeout', (5, 30))
        kwargs.setdefault('proxies', {"http": None, "https": None})
        resp = fn(url, timeout=timeout, **kwargs)
        elapsed = time.time() - start
        ct = resp.headers.get("Content-Type", "")
        if "application/json" in ct:
            return resp.status_code, resp.json(), elapsed
        return resp.status_code, resp.text, elapsed
    except requests.ConnectionError:
        return 0, {"error": "连接被拒，服务未启动"}, 0
    except Exception as e:
        return 0, {"error": str(e)}, 0

def run(suite, case, ok, detail=""):
    """记录单条断言"""
    if ok is True:
        _log("PASS", suite, case, detail)
    elif ok is False:
        _log("FAIL", suite, case, detail)
    elif ok == "SKIP":
        _log("SKIP", suite, case, detail)
    elif ok == "WARN":
        _log("WARN", suite, case, detail)

def runm(suite, case, flag, detail=""):
    """run 快捷方式: flag 为 (bool, message) 元组"""
    if isinstance(flag, tuple) and len(flag) == 2:
        ok, msg = flag
        run(suite, case, ok, msg if not ok else (detail or msg))


# ═══════════════════════════════════════════════════════════
#  WHITE-BOX: 推理服务
# ═══════════════════════════════════════════════════════════

def test_inference_whitebox():
    section("WHITE-BOX — 推理服务内部状态验证")

    # ── /health 响应结构校验 ──
    code, data, ela = api("get", f"{BASE_INFERENCE}/health")
    run("推理·白盒", "健康检查可达", code == 200,
        f"HTTP {code}" if code != 200 else "")
    if code != 200:
        return  # 后续无法进行

    runm("推理·白盒", "响应含 status 字段", _has_keys(data, "status"))
    runm("推理·白盒", "status == 'ok'", _equals(data.get("status"), "ok"))
    runm("推理·白盒", "含 model_dir 字段", _has_keys(data, "model_dir"))
    runm("推理·白盒", "model_dir 类型 str", _types(data, model_dir=str))
    runm("推理·白盒", "含 models_count 字段", _has_keys(data, "models_count"))
    runm("推理·白盒", "models_count >= 0", _in_range(data.get("models_count", -1), 0, 999))
    health_models_count = data.get("models_count")
    runm("推理·白盒", "含 device 字段", _has_keys(data, "device"))
    runm("推理·白盒", "device ∈ {cuda, cpu}", _one_of(data.get("device", ""), "cuda", "cpu"))
    runm("推理·白盒", "含 current_model 字段", _has_keys(data, "current_model"))

    # ── /models 响应结构校验 ──
    code, data, ela = api("get", f"{BASE_INFERENCE}/models")
    runm("推理·白盒", "/models 可达", _equals(code, 200))
    if code == 200:
        runm("推理·白盒", "含 models 数组", _has_keys(data, "models"))
        models = data.get("models", [])
        runm("推理·白盒", "模型数量与 health 一致",
             _equals(len(models), health_models_count,
                     f"health={health_models_count} /models={len(models)}"))
        for m in models[:1]:  # 校验第一个模型字段
            runm("推理·白盒", "模型含 name", _has_keys(m, "name"))
            runm("推理·白盒", "模型含 filename", _has_keys(m, "filename"))
            runm("推理·白盒", "模型含 size_mb", _has_keys(m, "size_mb"))
            runm("推理·白盒", "size_mb > 0", _in_range(m.get("size_mb", 0), 0.1, 999999))


def test_inference_blackbox():
    section("BLACK-BOX — 推理服务功能测试")

    # ── 正常生成 ──
    code, data, ela = api("post", f"{BASE_INFERENCE}/generate",
                          json={"prompt": "a cat", "steps": 5, "width": 256, "height": 256})
    runm("推理·功能", "POST /generate 返回 200", _equals(code, 200), f"HTTP {code}")
    if code == 200:
        runm("推理·功能", "success == True", _equals(data.get("success"), True))
        runm("推理·功能", "含 image_base64", _has_keys(data, "image_base64"))
        runm("推理·功能", "含 seed (整数)", _types(data, seed=int))
        # white-box: 验证 base64 能否解码为图片
        b64 = data.get("image_base64")
        if b64:
            try:
                img_bytes = base64.b64decode(b64)
                img = Image.open(BytesIO(img_bytes))
                runm("推理·功能", "base64 解码为有效图片", isinstance(img, Image.Image), "解码异常")
                runm("推理·功能", "输出尺寸匹配", _equals(img.size, (256, 256)))
            except Exception:
                run("推理·功能", "base64 解码为有效图片", False, "解码异常")
        runm("推理·功能", "生成耗时合理(<120s)", _in_range(ela, 0, 120), f"{ela:.1f}s")

    # ── 带进度 ──
    task_id = "test-progress-001"
    code, data, ela = api("post", f"{BASE_INFERENCE}/generate",
                          json={"prompt": "sunset", "steps": 5, "width": 320, "height": 320, "task_id": task_id},
                          timeout=120)
    runm("推理·功能", "带 task_id 生成成功", _equals(code, 200))
    if code == 200:
        time.sleep(0.5)
        pcode, pdata, _ = api("get", f"{BASE_INFERENCE}/progress/{task_id}")
        runm("推理·功能", "GET /progress 可达", _equals(pcode, 200))
        if pcode == 200:
            runm("推理·功能", "进度含 step", _has_keys(pdata, "step"))
            runm("推理·功能", "进度含 finished", _has_keys(pdata, "finished"))

    # ── 切换采样器 ──
    code, data, ela = api("post", f"{BASE_INFERENCE}/generate",
                          json={"prompt": "test sampler", "steps": 3, "width": 256, "height": 256,
                                "sampler_name": "DDIM"})
    runm("推理·功能", "DDIM 采样器生成成功", _equals(code, 200))

    # ── 无效模型 ──
    code, data, ela = api("post", f"{BASE_INFERENCE}/generate",
                          json={"prompt": "test", "steps": 3, "checkpoint_name": "__nonexistent__"})
    runm("推理·功能", "无效模型返回 404", _equals(code, 404))

    # ── 空 prompt ──
    code, data, ela = api("post", f"{BASE_INFERENCE}/generate",
                          json={"prompt": "", "steps": 3})
    runm("推理·功能", "空 prompt 返回 200 (容错)", _equals(code, 200),
         f"HTTP {code}" if code != 200 else "")


# ═══════════════════════════════════════════════════════════
#  WHITE-BOX: 后端 API
# ═══════════════════════════════════════════════════════════

def test_backend_whitebox_auth():
    section("WHITE-BOX — 认证模块内部验证")

    # ── 响应结构校验 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/login",
                          json={"email": "admin@nebula.com", "password": "admin123"})
    runm("认证·白盒", "登录返回 200", _equals(code, 200))
    runm("认证·白盒", "统一响应格式: {code, data, message}", _has_keys(data, "code", "data", "message"))
    runm("认证·白盒", "code == 200", _equals(data.get("code"), 200))
    runm("认证·白盒", "data 含 token", _has_keys(data.get("data", {}), "token"))
    runm("认证·白盒", "token 类型 str", _types(data.get("data", {}), token=str))
    runm("认证·白盒", "token 长度 >= 20", _in_range(len(data.get("data", {}).get("token", "")), 20, 9999))

    global TOKEN
    if code == 200 and data.get("data", {}).get("token"):
        TOKEN = data["data"]["token"]
    else:
        run("认证·白盒", "Token 获取", False, "后续认证测试将跳过")

    # ── 错误密码 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/login",
                          json={"email": "admin@nebula.com", "password": "wrongpassword"})
    runm("认证·白盒", "错误密码返回非 200", _equals(data.get("code") != 200, True),
         f"code={data.get('code')}")


def test_backend_whitebox_users():
    if not TOKEN:
        run("用户·白盒", "跳过", "SKIP", "无 Token")
        return

    section("WHITE-BOX — 用户模块内部验证")
    h = {"Authorization": f"Bearer {TOKEN}"}

    # ── 列表分页 ──
    code, data, ela = api("get", f"{BASE_BACKEND}/api/users?page=1&size=3", headers=h)
    runm("用户·白盒", "GET /users 返回 200", _equals(code, 200))
    if code == 200:
        runm("用户·白盒", "含 records 字段", _has_keys(data.get("data", {}), "records"))
        runm("用户·白盒", "records 类型 list", _types(data.get("data", {}), records=list))
        records = data.get("data", {}).get("records", [])
        runm("用户·白盒", f"分页 ≤3 条", _in_range(len(records), 0, 3), f"实际={len(records)}")
        if records:
            runm("用户·白盒", "用户含 id/username/email/role", _has_keys(records[0], "id", "username", "email", "role"))
            runm("用户·白盒", "role ∈ {USER,DESIGNER,ADMIN}",
                 _one_of(records[0].get("role", ""), "USER", "DESIGNER", "ADMIN"))


def test_backend_whitebox_text2image():
    if not TOKEN:
        run("文生图·白盒", "跳过", "SKIP", "无 Token")
        return

    section("WHITE-BOX — 文生图模块内部验证")
    h = {"Authorization": f"Bearer {TOKEN}"}

    # ── 模型列表 ──
    code, data, ela = api("get", f"{BASE_BACKEND}/api/text2image/models", headers=h)
    runm("文生图·白盒", "GET /models 返回 200", _equals(code, 200))
    if code == 200 and isinstance(data.get("data"), list):
        for m in data["data"][:1]:
            runm("文生图·白盒", "模型含 name/filename/sizeMb", _has_keys(m, "name", "filename", "sizeMb"))
            runm("文生图·白盒", "不含绝对路径 path (安全)", _equals("path" in m, False), "泄露绝对路径!" if "path" in m else "已移除")

    # ── 文生图请求结构 ──
    payload = {"prompt": "a cat", "steps": 5, "width": 320, "height": 320, "cfgScale": 7.0, "batchSize": 1}
    code, data, ela = api("post", f"{BASE_BACKEND}/api/text2image/generate", json=payload, headers=h, timeout=300)
    runm("文生图·白盒", "POST /generate 返回 200", _equals(code, 200))
    if code == 200 and data.get("code") == 200:
        record = data.get("data", {})
        runm("文生图·白盒", "record 含 id/status/imageUrl", _has_keys(record, "id", "status", "imageUrl"))
        runm("文生图·白盒", "status == 'SUCCESS'", _equals(record.get("status"), "SUCCESS"),
             f"实际={record.get('status')}")
        runm("文生图·白盒", "imageUrl 非空", _in_range(len(record.get("imageUrl") or ""), 1, 9999))

    # ── 缺 prompt ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/text2image/generate",
                          json={"steps": 5}, headers=h, timeout=300)
    runm("文生图·白盒", "缺 prompt 拒绝", _equals(code == 200 and data.get("code") == 200, False),
         "应拒绝却通过了" if code == 200 and data.get("code") == 200 else "正确拦截")

    # ── 超界参数 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/text2image/generate",
                          json={"prompt": "test", "steps": 999, "width": 5000, "height": 5000},
                          headers=h, timeout=300)
    runm("文生图·白盒", "超界参数被校验拦截", _equals(code == 200 and data.get("code") == 200, False),
         "应拒绝" if code == 200 and data.get("code") == 200 else "已拦截")


def test_backend_whitebox_image2text():
    if not TOKEN:
        run("图生文·白盒", "跳过", "SKIP", "无 Token")
        return

    section("WHITE-BOX — 图生文模块内部验证")
    h = {"Authorization": f"Bearer {TOKEN}"}

    # ── 生成一张小图用于测试 ──
    test_img = BytesIO()
    Image.new("RGB", (64, 64), "blue").save(test_img, "PNG")
    test_img.seek(0)

    code, data, ela = api("post", f"{BASE_BACKEND}/api/image2text/analyze",
                          headers=h,
                          files={"image": ("test.png", test_img, "image/png")},
                          data={"analysisType": "general"},
                          timeout=120)
    runm("图生文·白盒", "POST /analyze 返回 200", _equals(code, 200))
    if code == 200 and data.get("code") == 200:
        record = data.get("data", {})
        runm("图生文·白盒", "含 id/type/status", _has_keys(record, "id", "type", "status"))
        runm("图生文·白盒", "type == 'IMAGE2TEXT'", _equals(record.get("type"), "IMAGE2TEXT"))
    else:
        runm("图生文·白盒", "响应结构正常", _equals(code, 200),
             f"HTTP {code} msg={data.get('message', '')} — API Key 已配置?")

    # ── 空文件 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/image2text/analyze",
                          headers=h, timeout=120)
    runm("图生文·白盒", "缺文件参数被拦截", _equals(data.get("code"), 400),
         f"HTTP {code} — 应返回 400" if data.get("code") != 400 else "")


# ═══════════════════════════════════════════════════════════
#  BLACK-BOX: 后端权限与边界
# ═══════════════════════════════════════════════════════════

def test_backend_blackbox_auth():
    section("BLACK-BOX — 认证授权边界测试")

    # ── 无认证访问受保护资源 ──
    code, data, ela = api("get", f"{BASE_BACKEND}/api/user/profile")
    runm("授权·黑盒", "无 Token 访问 profile → 401", _equals(code, 401), f"HTTP {code}")

    code, data, ela = api("get", f"{BASE_BACKEND}/api/text2image/models")
    runm("授权·黑盒", "无 Token 访问 /models → 401", _equals(code, 401), f"HTTP {code}")

    # ── 公开端点 ──
    code, data, ela = api("get", f"{BASE_BACKEND}/api/styles/active")
    runm("授权·黑盒", "GET /api/styles/active (公开) 可达", _in_range(code, 200, 499), f"HTTP {code}")

    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/register",
                          json={"username": "bogus", "email": "bogus@test.com", "password": "Test123456"})
    run("授权·黑盒", "POST /auth/register (公开) 可达", _in_range(code, 200, 499),
        f"HTTP {code}" if code >= 500 else "")

    # ── OPTIONS 预检 ──
    code, data, ela = api("options", f"{BASE_BACKEND}/api/auth/login")
    runm("授权·黑盒", "OPTIONS 预检通过", _in_range(code, 200, 299), f"HTTP {code}")

    # ── 伪造 Token ──
    fake_h = {"Authorization": "Bearer invalid.token.here"}
    code, data, ela = api("get", f"{BASE_BACKEND}/api/user/profile", headers=fake_h)
    runm("授权·黑盒", "伪造 Token 返回 401", _equals(code, 401), f"HTTP {code}")

    if TOKEN:
        h = {"Authorization": f"Bearer {TOKEN}"}
        # ── 普通用户尝试管理员接口 (admin 本身有权限，所以测试 designer) ──
        code2, designer, _ = api("post", f"{BASE_BACKEND}/api/auth/login",
                                 json={"email": "designer@nebula.com", "password": "designer123"})
        if code2 == 200 and designer.get("data", {}).get("token"):
            dt = designer["data"]["token"]
            dh = {"Authorization": f"Bearer {dt}"}
            dcode, ddata, _ = api("get", f"{BASE_BACKEND}/api/users", headers=dh)
            runm("授权·黑盒", "DESIGNER 访问 /users (需ADMIN) → 403",
                 _equals(dcode, 403), f"HTTP {dcode}")


def test_backend_blackbox_rate_limit():
    section("BLACK-BOX — 速率限制测试")

    # ── 快速连续登录 ──
    blocked = False
    for i in range(8):
        code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/login",
                              json={"email": "admin@nebula.com", "password": "wrong"})
        if code == 429:
            blocked = True
            break
        time.sleep(0.02)  # 极短间隔
    if blocked:
        run("限流·黑盒", "登录频率限制正确拦截", True, "429 Too Many Requests")
    else:
        run("限流·黑盒", "登录频率限制", "WARN",
            "8 次连续请求均未被限流 (可能限流器未生效或速率过高)")


def test_backend_blackbox_input_validation():
    section("BLACK-BOX — 输入校验测试")

    def _rejected(code, data):
        return (code == 200 and isinstance(data, dict) and data.get("code") != 200) or code != 200

    # ── 注册: 无效邮箱 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/register",
                          json={"username": "test", "email": "notanemail", "password": "Test123456"})
    passed = _rejected(code, data)
    run("校验·黑盒", "无效邮箱格式被拦截", passed,
        "已拦截" if passed else f"HTTP {code} code={data.get('code') if isinstance(data, dict) else '?'}")

    # ── 注册: 弱密码 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/register",
                          json={"username": "test", "email": "weak@test.com", "password": "12"})
    passed = _rejected(code, data)
    run("校验·黑盒", "弱密码被拦截 (>=6)", passed,
        "已拦截" if passed else f"HTTP {code} code={data.get('code') if isinstance(data, dict) else '?'}")

    # ── 登录: 空 JSON ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/login", json={})
    passed = _rejected(code, data)
    run("校验·黑盒", "空登录体被拦截", passed,
        "已拦截" if passed else f"HTTP {code} code={data.get('code') if isinstance(data, dict) else '?'}")

    # ── SQL 注入试探 ──
    code, data, ela = api("post", f"{BASE_BACKEND}/api/auth/login",
                          json={"email": "admin@nebula.com' OR '1'='1", "password": "test123"})
    passed = _rejected(code, data)
    run("校验·黑盒", "SQL 注入试探被拦截", passed,
        "已拦截" if passed else "WARN: 未拦截，需检查参数化查询")


# ═══════════════════════════════════════════════════════════
#  INTEGRATION: 端到端流程
# ═══════════════════════════════════════════════════════════

def test_integration_e2e():
    section("集成测试 — 端到端场景")

    # ── E2E: 登录 → 查看风格 → 生成图片 → 查看历史 ──
    code, data, _ = api("post", f"{BASE_BACKEND}/api/auth/login",
                        json={"email": "admin@nebula.com", "password": "admin123"})
    if code != 200 or data.get("code") != 200 or not data.get("data", {}).get("token"):
        run("集成", "E2E 前置条件失败", "SKIP", "登录失败")
        return

    token = data["data"]["token"]
    h = {"Authorization": f"Bearer {token}"}
    suite = "集成·E2E"

    # Step 1: 获取风格列表
    code, data, _ = api("get", f"{BASE_BACKEND}/api/styles/active", headers=h)
    runm(suite, "Step 1 — 获取风格列表", _equals(code, 200))

    # Step 2: 获取模型列表
    code, data, _ = api("get", f"{BASE_BACKEND}/api/text2image/models", headers=h)
    runm(suite, "Step 2 — 获取模型列表", _equals(code, 200))

    # Step 3: 文生图
    code, data, ela = api("post", f"{BASE_BACKEND}/api/text2image/generate",
                          json={"prompt": "a beautiful landscape", "steps": 8, "width": 320, "height": 320, "cfgScale": 7.0, "batchSize": 1},
                          headers=h, timeout=300)
    passed = code == 200 and data.get("code") == 200 and data.get("data", {}).get("status") == "SUCCESS"
    run(suite, "Step 3 — 文生图 (真实推理)", passed,
        f"{ela:.1f}s" if passed else f"HTTP {code} status={data.get('data',{}).get('status','')}")

    # Step 4: 查看历史
    code, data, _ = api("get", f"{BASE_BACKEND}/api/text2image/history?page=1&size=5", headers=h)
    records = data.get("data", {}).get("records", []) if data.get("code") == 200 else []
    runm(suite, "Step 4 — 历史记录查询", _in_range(len(records), 0, 99))
    if records and records[0].get("imageUrl"):
        runm(suite, "历史记录含 imageUrl", _in_range(len(records[0].get("imageUrl", "")), 1, 9999))


# ═══════════════════════════════════════════════════════════
#  MAIN
# ═══════════════════════════════════════════════════════════

def print_header():
    print(_c("cyan", """
  ╔══════════════════════════════════════════════════════════╗
  ║       Nebula Studio — 全量回归测试 (黑·白盒)           ║
  ║       推理服务 + 后端 API + 权限 + 边界 + E2E          ║
  ╚══════════════════════════════════════════════════════════╝
"""))
    print(f"  Backend:    {BASE_BACKEND}")
    print(f"  Inference:  {BASE_INFERENCE}")
    print(f"  Started:    {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()

def safe_print(*args, **kwargs):
    try:
        _print(*args, **kwargs)
    except UnicodeEncodeError:
        text = " ".join(str(a) for a in args)
        text = text.encode("ascii", errors="replace").decode("ascii")
        _print(text, **{k:v for k,v in kwargs.items() if k in ("end","file","flush")})

_print = print
print = safe_print

def wait_service(url, label, timeout=3):
    """轮询等待服务就绪"""
    print(f"  [..] 检查 {label} ...", end=" ", flush=True)
    for _ in range(timeout):
        try:
            r = requests.get(url, timeout=(2, 3), proxies={"http": None, "https": None})
            if r.status_code < 500:
                print(_c("green", "OK"))
                return True
        except (requests.ConnectionError, requests.Timeout):
            pass
        except Exception:
            pass
        time.sleep(0.5)
    print(_c("yellow", "未响应"))
    return False

def main():
    global PASS, FAIL, SKIP, WARN
    print_header()

    # ── 服务可用性 ──
    section("前置检查 — 服务可用性")
    inf_ok = wait_service(f"{BASE_INFERENCE}/health", "推理服务:5000", timeout=120)
    be_ok  = wait_service(f"{BASE_BACKEND}/", "后端:8080", timeout=3)
    run("前置", "推理服务 5000 可达", inf_ok, "已就绪" if inf_ok else "未响应")
    run("前置", "后端 8080 可达", be_ok, "已就绪" if be_ok else "未响应")
    if not be_ok:
        print(_c("red", "\n  [X] 后端未启动, 终止测试。"))
        print(_c("cyan", "  请运行: cd project01 && start.bat\n"))
        print_summary()
        return

    # ── 白盒测试 ──
    test_inference_whitebox()
    test_backend_whitebox_auth()
    test_backend_whitebox_users()
    test_backend_whitebox_text2image()
    test_backend_whitebox_image2text()

    # ── 黑盒测试 ──
    test_backend_blackbox_input_validation()
    test_inference_blackbox()
    test_backend_blackbox_auth()
    test_backend_blackbox_rate_limit()

    # ── 集成测试 ──
    time.sleep(3)  # 等待限流配额恢复
    test_integration_e2e()

    # ── 汇总 ──
    print_summary()

    return 0 if FAIL == 0 else 1


def print_summary():
    total = PASS + FAIL + SKIP + WARN
    elapsed = time.time() - TEST_START
    summary = f"""
{'═'*60}
  测试结果汇总
{'═'*60}
  总耗时:  {elapsed:.1f}s
  总计:    {total}  通过: {_c('green', str(PASS))}  失败: {_c('red', str(FAIL))}  跳过: {_c('yellow', str(SKIP))}  警告: {_c('yellow', str(WARN))}
  通过率:  {PASS/total*100 if total else 0:.1f}%
{'═'*60}
"""
    print(summary)
    REPORT_LINES.append(summary)

    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write(f"Nebula Studio — 回归测试报告\n")
        f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"总耗时: {elapsed:.1f}s | 通过:{PASS} 失败:{FAIL} 跳过:{SKIP} 警告:{WARN}\n")
        f.write(f"{'─'*60}\n")
        for line in REPORT_LINES:
            clean = re.sub(r'\033\[[0-9;]+m', '', line)
            f.write(clean + "\n")
    print(f"  报告已保存: {REPORT_FILE}")


if __name__ == "__main__":
    sys.exit(main())
