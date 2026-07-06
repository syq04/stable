import json, subprocess, os, time

class MCP:
    def __init__(self):
        env = os.environ.copy()
        env["PYTHONIOENCODING"] = "utf-8"
        self._id = 0
        self.p = subprocess.Popen(
            ["uvx", "--from", "office-powerpoint-mcp-server", "ppt_mcp_server"],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env
        )
        self._send("initialize", {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"t","version":"1.0"}})
        self._read()
        self._send_raw({"jsonrpc":"2.0","method":"notifications/initialized"})

    def _send_raw(self, obj):
        self.p.stdin.write((json.dumps(obj, ensure_ascii=False)+"\n").encode("utf-8"))
        self.p.stdin.flush()

    def _send(self, method, params):
        self._id += 1
        self._send_raw({"jsonrpc":"2.0","id":self._id,"method":method,"params":params})
        return self._id

    def _read(self):
        deadline = time.time() + 30
        while time.time() < deadline:
            line = self.p.stdout.readline()
            if not line:
                time.sleep(0.05)
                continue
            try:
                r = json.loads(line.decode("utf-8",errors="replace").strip())
                return r
            except: pass
        return None

    def tool(self, name, args):
        rid = self._send("tools/call", {"name":name,"arguments":args})
        while True:
            r = self._read()
            if r and r.get("id") == rid:
                return r.get("result")

    def close(self):
        try: self.p.stdin.close()
        except: pass
        try: self.p.wait(timeout=3)
        except: self.p.kill()

m = MCP()

# Open existing presentation
r = m.tool("open_presentation", {"file_path":"F:\\Stable\\mcp_test.pptx"})
pid = None
if r and "content" in r:
    for item in r["content"]:
        if isinstance(item, dict) and "text" in item:
            try:
                pid = json.loads(item["text"]).get("presentation_id")
            except: pass
print(f"Opened: {pid}")

# Check slide info for transitions
for i in range(6):
    r = m.tool("get_slide_info", {"slide_index":i, "presentation_id":pid})
    if r and "content" in r:
        for item in r["content"]:
            if isinstance(item, dict) and "text" in item:
                try:
                    data = json.loads(item["text"])
                    transition = data.get("transition", "none")
                    shapes = data.get("shapes_count", 0)
                    print(f"  Slide {i}: transition={transition}, shapes={shapes}")
                except: pass

# Apply professional design to make it look better
r = m.tool("apply_professional_design", {
    "operation": "theme",
    "color_scheme": "modern_blue",
    "apply_to_existing": True,
    "presentation_id": pid
})
print(f"\nTheme applied: {json.dumps(r, ensure_ascii=False)[:200]}")

# Save with overwrite
r = m.tool("save_presentation", {"file_path":"F:\\Stable\\mcp_test.pptx", "presentation_id":pid})
print(f"Saved: {json.dumps(r, ensure_ascii=False)[:200]}")

m.close()
