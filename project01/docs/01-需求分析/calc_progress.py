import re
import os
from datetime import datetime

# Base paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, '../../'))
DOCS_DIR = os.path.join(PROJECT_ROOT, 'docs', '01-需求分析')
PROG_FILE = os.path.join(DOCS_DIR, 'feature-list.md')

def calc_progress():
    if not os.path.exists(PROG_FILE):
        print(f"File not found: {PROG_FILE}")
        return

    with open(PROG_FILE, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    total_funcs = 0
    completed_funcs = 0
    
    # Stages
    stages = ['proto', 'fe', 'be', 'test', 'int']
    counts = {s: {'total': 0, 'done': 0} for s in stages}
    
    # Module stats: F01 -> {total: 0, proto: 0, fe: 0, be: 0, test: 0, int: 0}
    module_stats = {}
    
    # Table Row Regex
    # | F01.01.01 | MO 创建 | [ ] `html` | [ ] | [ ] | [ ] | [ ] |
    # Groups: 1=ID, 2=Name, 3=Proto, 4=FE, 5=BE, 6=Test, 7=Int
    row_pattern = re.compile(r'^\s*\|\s*(F(\d+)\.\d+\.\d+)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|')
    
    # Summary Row Regex: | F01 建模对象 (MO) 管理 | 33 | ...
    summary_row_pattern = re.compile(r'^\s*\|\s*(F(\d+)\s+.*?)\s*\|\s*\d+\s*\|')
    
    for line in lines:
        m = row_pattern.match(line)
        if m:
            fid_full = m.group(1) # F01.01.01
            mod_id = m.group(2)   # 01
            mod_key = f"F{mod_id}"
            
            if mod_key not in module_stats:
                module_stats[mod_key] = {'total': 0, 'proto': 0, 'fe': 0, 'be': 0, 'test': 0, 'int': 0}
            
            module_stats[mod_key]['total'] += 1
            total_funcs += 1
            
            # Helper
            def is_checked(text):
                return '[x]' in text.lower()
            
            # Check columns
            # Proto (Group 3)
            if is_checked(m.group(3)):
                counts['proto']['done'] += 1
                module_stats[mod_key]['proto'] += 1
            counts['proto']['total'] += 1
            
            # FE (Group 4)
            if is_checked(m.group(4)):
                counts['fe']['done'] += 1
                module_stats[mod_key]['fe'] += 1
            counts['fe']['total'] += 1
            
            # BE (Group 5)
            if is_checked(m.group(5)):
                counts['be']['done'] += 1
                module_stats[mod_key]['be'] += 1
            counts['be']['total'] += 1
            
            # Test (Group 6)
            if is_checked(m.group(6)):
                counts['test']['done'] += 1
                module_stats[mod_key]['test'] += 1
            counts['test']['total'] += 1
            
            # Int (Group 7)
            if is_checked(m.group(7)):
                counts['int']['done'] += 1
                module_stats[mod_key]['int'] += 1
            counts['int']['total'] += 1

            # Row completion (if all checked? No, logic was if row_done in original script)
            # Original script checked if all checked for completed_funcs?
            # Actually, "completed_funcs" variable was used but logic was:
            # if row_done: completed_funcs += 1
            # where row_done started as True and set to False if any check missing.
            # Let's replicate that.
            row_done = True
            if not is_checked(m.group(3)): row_done = False
            if not is_checked(m.group(4)): row_done = False
            if not is_checked(m.group(5)): row_done = False
            if not is_checked(m.group(6)): row_done = False
            if not is_checked(m.group(7)): row_done = False
            
            if row_done:
                completed_funcs += 1

    # Calc Percentages Helper
    def pct_val(done, total):
        return (done / total * 100) if total > 0 else 0
        
    def pct_str(done, total):
        return f"{pct_val(done, total):.0f}%"
        
    total_pct = pct_val(completed_funcs, total_funcs)
    
    # Reconstruct File
    new_lines = []
    
    for line in lines:
        # Check if it is a summary row
        sm = summary_row_pattern.match(line)
        if sm:
            mod_name = sm.group(1) # F01 建模对象...
            mod_id = sm.group(2)   # 01
            mod_key = f"F{mod_id}"
            
            if mod_key in module_stats:
                st = module_stats[mod_key]
                t = st['total']
                # | Module | Total | Proto | FE | BE | Test | Int |
                # Format: | F01 ... | 34 | 0% | 0% | 0% | 0% | 0% |
                new_row = f"| {mod_name} | {t} | {pct_str(st['proto'], t)} | {pct_str(st['fe'], t)} | {pct_str(st['be'], t)} | {pct_str(st['test'], t)} | {pct_str(st['int'], t)} |\n"
                new_lines.append(new_row)
            else:
                new_lines.append(line)
        
        elif '| **合计** |' in line:
            # | **合计** | **237** | **7%** | ...
            p_proto = pct_str(counts['proto']['done'], counts['proto']['total'])
            p_fe = pct_str(counts['fe']['done'], counts['fe']['total'])
            p_be = pct_str(counts['be']['done'], counts['be']['total'])
            p_test = pct_str(counts['test']['done'], counts['test']['total'])
            p_int = pct_str(counts['int']['done'], counts['int']['total'])
            
            new_total_line = f"| **合计** | **{total_funcs}** | **{p_proto}** | **{p_fe}** | **{p_be}** | **{p_test}** | **{p_int}** |\n"
            new_lines.append(new_total_line)
            
        else:
            new_lines.append(line)
            
    # Write
    with open(PROG_FILE, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
    print(f"Progress stats updated. Total functions: {total_funcs}")

if __name__ == '__main__':
    calc_progress()
