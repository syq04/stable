import re
import os

# Base paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, '../../'))
DOCS_DIR = os.path.join(PROJECT_ROOT, 'docs', '01-需求分析')

PROG_FILE = os.path.join(DOCS_DIR, 'feature-list.md')
OUTPUT_FILE = os.path.join(DOCS_DIR, 'feature-prototype-list.md')

def generate_list():
    print(f"Reading {PROG_FILE}...")
    if not os.path.exists(PROG_FILE):
        print("File not found.")
        return

    with open(PROG_FILE, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Regex to parse table row
    # | F01.01.01 | MO 创建 | [ ] `mo_list.html` | ...
    # Group 1: FID
    # Group 2: FName
    # Group 3: Checkbox content (x or space)
    # Group 4: Prototype File
    row_pat = re.compile(r'\|\s*([A-Z0-9\.]+)\s*\|\s*([^|]+)\s*\|\s*\[([ xX])\]\s*`([^`]+)`')

    prototypes = {} # Map filename -> { features: [], status: bool }

    for line in lines:
        m = row_pat.search(line)
        if m:
            fid = m.group(1).strip()
            fname = m.group(2).strip()
            checked = m.group(3).strip().lower() == 'x'
            proto_file = m.group(4).strip()
            
            if proto_file not in prototypes:
                prototypes[proto_file] = {
                    'features': [],
                    'status': True # Default true, if any unchecked, set to false? Or all checked?
                                   # Let's say if ALL features for this prototype are checked, then it is Done.
                                   # But here we are tracking per feature. 
                                   # Actually, usually prototype is one file covering multiple features.
                                   # If ANY feature marks it as done, is it done?
                                   # Usually [x] `file.html` means "Prototype for this feature is done".
                                   # If all features sharing this file have [x], then file is done.
                }
            
            prototypes[proto_file]['features'].append(f"{fid} {fname}")
            if not checked:
                prototypes[proto_file]['status'] = False

    # Sort by filename or module?
    # Maybe sort by first feature ID to keep module order
    sorted_protos = sorted(prototypes.items(), key=lambda x: x[1]['features'][0])
    
    print(f"Found {len(sorted_protos)} prototypes.")

    # Generate Markdown
    content = []
    content.append("# 页面原型清单 (Page Prototype List)\n")
    content.append("\n")
    content.append("**版本**: v1.0.0\n")
    content.append(f"**生成日期**: {os.popen('date /t').read().strip() if os.name == 'nt' else 'Today'}\n")
    content.append("\n")
    content.append("---\n")
    content.append("\n")
    content.append("| 序号 | 原型文件名 | 对应功能点 | 状态 | 说明 |\n")
    content.append("| :--- | :--- | :--- | :---: | :--- |\n")
    
    count = 1
    for proto_file, data in sorted_protos:
        features_str = "<br>".join(data['features'])
        status_str = "✅ 完成" if data['status'] else "⬜ 待设计"
        
        # Check if it is a dashboard (ends with _dashboard.html)
        note = ""
        if "_dashboard.html" in proto_file:
            note = "模块概览仪表盘"
            
        content.append(f"| {count} | `{proto_file}` | {features_str} | {status_str} | {note} |\n")
        count += 1
        
    content.append("\n")

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.writelines(content)
    print(f"Generated {OUTPUT_FILE}")

if __name__ == '__main__':
    generate_list()
