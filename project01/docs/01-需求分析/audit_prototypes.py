import re
import os

# Base paths - Dynamically calculate project root
# Assuming this script is located in prompts/01-需求分析/ or similar depth (2 levels deep from root)
# Adjust the '../../' if the folder depth changes.
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, '../../'))

# Configurable paths relative to Project Root
# These can be modified to adapt to other systems
DOCS_REL_PATH = os.path.join('docs', '01-需求分析')
PROTO_REL_PATH = os.path.join('docs', '04-页面原型', 'prototype')
FEATURE_LIST_NAME = "feature-list.md"
REPORT_FILE_NAME = "prototype_audit_report.md"

# Absolute paths
DOCS_DIR = os.path.join(PROJECT_ROOT, DOCS_REL_PATH)
PROTO_DIR = os.path.join(PROJECT_ROOT, PROTO_REL_PATH)
FEATURE_LIST = os.path.join(DOCS_DIR, FEATURE_LIST_NAME)
REPORT_FILE = os.path.join(DOCS_DIR, REPORT_FILE_NAME)

def analyze():
    print(f"Starting Audit...")
    print(f"Project Root: {PROJECT_ROOT}")
    print(f"Feature List: {FEATURE_LIST}")
    print(f"Prototype Dir: {PROTO_DIR}")

    if not os.path.exists(FEATURE_LIST):
        print(f"Error: Feature list file not found at {FEATURE_LIST}")
        return

    if not os.path.exists(PROTO_DIR):
        print(f"Error: Prototype directory not found at {PROTO_DIR}")
        return

    with open(FEATURE_LIST, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Regex for table row: | F01.01.01 | MO 创建 | [ ] `mo_list.html` | ...
    # Group 1: ID, Group 2: Name, Group 3: HTML File
    row_pat = re.compile(r'\|\s*(F[\d\.]+)\s*\|\s*([^|]+)\s*\|\s*\[.*?\]\s*`([^`]+)`')

    results = []
    
    for line in lines:
        m = row_pat.search(line)
        if m:
            fid = m.group(1).strip()
            fname = m.group(2).strip()
            html_file = m.group(3).strip()
            
            html_path = os.path.join(PROTO_DIR, html_file)
            
            status = "OK"
            details = ""
            
            if not os.path.exists(html_path):
                status = "Missing File"
                details = "File does not exist"
            else:
                # Check content
                try:
                    with open(html_path, 'r', encoding='utf-8') as hf:
                        content = hf.read()
                        
                    # Simple keyword check: check if function name keywords exist in content
                    if len(fname) > 1:
                        # Try to match at least some part
                        if fname not in content:
                            # Try splitting by space or brackets
                            parts = re.split(r'[ \(\)]', fname)
                            parts = [p for p in parts if len(p) > 1]
                            found_any = False
                            
                            # If no parts (e.g. name was too short or just symbols), skip strict check
                            if not parts:
                                found_any = True
                            
                            for p in parts:
                                if p in content:
                                    found_any = True
                                    break
                            
                            if not found_any:
                                status = "Content Mismatch?"
                                details = f"Keywords from '{fname}' not found in {html_file}"
                except Exception as e:
                    status = "Error"
                    details = str(e)

            results.append({
                'fid': fid,
                'fname': fname,
                'html': html_file,
                'status': status,
                'details': details
            })

    # Calculate stats
    missing_files = [r for r in results if r['status'] == "Missing File"]
    mismatch_content = [r for r in results if "Mismatch" in r['status']]
    
    # Output Report to Markdown
    with open(REPORT_FILE, 'w', encoding='utf-8') as f:
        f.write("# ODiS 页面原型覆盖审计报告\n\n")
        f.write(f"**生成日期**: {os.popen('date /t').read().strip() if os.name == 'nt' else 'Today'}\n\n")
        f.write("## 审计概览\n\n")
        f.write(f"- **功能点总数**: {len(results)}\n")
        f.write(f"- **缺失文件**: {len(missing_files)}\n")
        f.write(f"- **内容疑似缺失**: {len(mismatch_content)}\n")
        f.write(f"- **已验证**: {len(results) - len(missing_files) - len(mismatch_content)}\n\n")
        
        f.write("## 详细清单与推进计划\n\n")
        f.write("| 功能ID | 功能名称 | 对应原型文件 | 状态 | 问题描述 | 推进计划 |\n")
        f.write("| :--- | :--- | :--- | :--- | :--- | :--- |\n")
        
        for r in results:
            plan = "已完成"
            if r['status'] == "Missing File":
                plan = "需创建文件"
            elif r['status'] != "OK":
                plan = "需补充页面内容"
            
            status_icon = "✅"
            if r['status'] == "Missing File": status_icon = "❌"
            elif "Mismatch" in r['status']: status_icon = "⚠️"
            elif "Error" in r['status']: status_icon = "🚫"
            
            f.write(f"| {r['fid']} | {r['fname']} | `{r['html']}` | {status_icon} {r['status']} | {r['details']} | {plan} |\n")
            
    print(f"Report generated at {REPORT_FILE}")

if __name__ == "__main__":
    analyze()
