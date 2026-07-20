import re
import sys
import os

KEYWORDS = ["DIVACA", "PADRICIANO", "REDIPUGLIA", "MELINA", "BERICEVO", "LIENZ", "MALTA", "NAUDERS", "VILLACH"]

def is_relevant(line):
    line_upper = line.upper()
    for kw in KEYWORDS:
        if kw in line_upper:
            return True
    return False

def clean_uct(filepath):
    # Only keep some lines to reduce size, but don't break UCTE importer
    # For now, let's just remove comments (except version) and keep all data lines
    with open(filepath, 'r', encoding='latin-1') as f:
        lines = f.readlines()
    if not lines:
        return
    
    new_lines = [lines[0]]
    for line in lines[1:]:
        if line.startswith('##C'):
            continue
        new_lines.append(line)
            
    with open(filepath, 'w', encoding='latin-1') as f:
        f.writelines(new_lines)

if __name__ == "__main__":
    for arg in sys.argv[1:]:
        if os.path.isfile(arg):
            clean_uct(arg)
        elif os.path.isdir(arg):
            for root, dirs, filenames in os.walk(arg):
                for filename in filenames:
                    if filename.lower().endswith('.uct'):
                        clean_uct(os.path.join(root, filename))
