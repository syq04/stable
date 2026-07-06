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

def text(result):
    if result and "content" in result:
        for item in result["content"]:
            if isinstance(item, dict) and "text" in item:
                try:
                    return json.loads(item["text"])
                except:
                    return item["text"]
    return result

m = MCP()

# 1. Create
r = text(m.tool("create_presentation", {}))
pid = r.get("presentation_id")
print(f"1. Created: {pid}")

# 2. Add slides
slides = [
    (0, "AI Tech & Future Trends"),
    (1, "Core Technologies"),
    (1, "Market Data"),
    (1, "Growth Chart"),
    (1, "Workflow"),
    (0, "Thank You")
]
for i, (layout, title) in enumerate(slides):
    r = text(m.tool("add_slide", {"layout_index":layout, "title":title, "presentation_id":pid}))
    print(f"2.{i}. Slide {i}: {title} -> {r}")

# 3. Add bullets to slide 1
r = text(m.tool("add_bullet_points", {"slide_index":1, "placeholder_idx":1,
    "bullet_points":["LLM (GPT/Claude)","Multimodal AI","Edge Computing","Autonomous Driving"],
    "presentation_id":pid}))
print(f"3. Bullets: {r}")

# 4. Add table to slide 2
r = text(m.tool("add_table", {"slide_index":2, "rows":3, "cols":2, "left":1.0, "top":2.5, "width":8.0, "height":2.0,
    "data":[["Feature","Status"],["Templates","OK"],["Animations","Transitions OK"]],
    "presentation_id":pid}))
print(f"4. Table: {r}")

# 5. Add chart to slide 3
r = text(m.tool("add_chart", {"slide_index":3, "chart_type":"column", "left":1.0, "top":2.0, "width":8.0, "height":4.5,
    "categories":["Q1","Q2","Q3","Q4"], "series_names":["2024","2025"],
    "series_values":[[100,120,140,160],[110,130,150,170]], "has_legend":True,
    "title":"Quarterly", "presentation_id":pid}))
print(f"5. Chart: {r}")

# 6. Add shapes to slide 4
for i,(txt,c) in enumerate(zip(["Research","Design","Build","Ship"],[[0,120,215],[0,166,81],[255,165,0],[220,53,69]])):
    m.tool("add_shape", {"slide_index":4, "shape_type":"rectangle", "left":0.5+i*2.3, "top":2.5, "width":2.0, "height":1.2,
        "fill_color":c, "text":txt, "font_size":16, "font_color":[255,255,255], "presentation_id":pid})
for i in range(3):
    m.tool("add_connector", {"slide_index":4, "connector_type":"straight",
        "start_x":2.5+i*2.3, "start_y":3.1, "end_x":2.8+i*2.3, "end_y":3.1,
        "line_width":2, "color":[100,100,100], "presentation_id":pid})
print("6. Shapes+Connectors done")

# 7. Populate slide 5
r = text(m.tool("populate_placeholder", {"slide_index":5, "placeholder_idx":1, "text":"Questions?", "presentation_id":pid}))
print(f"7. Placeholder: {r}")

# 8. Transitions
for i,t in enumerate(["fade","push","wipe","cover","cover","fade"]):
    r = text(m.tool("manage_slide_transitions", {"slide_index":i, "operation":"set", "transition_type":t, "duration":1.5, "presentation_id":pid}))
    print(f"8.{i}. Transition {i}: {t} -> {r}")

# 9. Apply theme
r = text(m.tool("apply_professional_design", {"operation":"theme", "color_scheme":"modern_blue", "apply_to_existing":True, "presentation_id":pid}))
print(f"9. Theme: {r}")

# 10. Save
r = text(m.tool("save_presentation", {"file_path":"F:\\Stable\\mcp_test.pptx", "presentation_id":pid}))
print(f"10. Saved: {r}")

# 11. Verify - open and check
m.close()
m2 = MCP()
r = text(m2.tool("open_presentation", {"file_path":"F:\\Stable\\mcp_test.pptx"}))
pid2 = r.get("presentation_id")
print(f"\n11. Re-opened: {pid2}")

for i in range(6):
    r = text(m2.tool("get_slide_info", {"slide_index":i, "presentation_id":pid2}))
    print(f"    Slide {i}: {json.dumps(r, ensure_ascii=False)[:150]}")

m2.close()

sz = os.path.getsize("F:\\Stable\\mcp_test.pptx")
print(f"\nFinal: F:\\Stable\\mcp_test.pptx ({sz:,} bytes)")
