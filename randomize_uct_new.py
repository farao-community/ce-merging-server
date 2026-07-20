import re
import random
import sys
import os

def randomize_digit(c):
    if c.isdigit():
        return str(random.randint(0, 9))
    return c

def randomize_match(match):
    s = match.group(0)
    return "".join(randomize_digit(c) for c in s)

KEYWORDS = ["DIVACA", "PADRICIANO", "REDIPUGLIA", "MELINA", "BERICEVO", "LIENZ", "MALTA", "NAUDERS", "VILLACH"]

def is_relevant(line):
    line_upper = line.upper()
    for kw in KEYWORDS:
        if kw in line_upper:
            return True
    return False

def process_file(filepath):
    with open(filepath, 'r', encoding='latin-1') as f:
        lines = f.readlines()
    
    if not lines:
        return

    new_lines = [lines[0]]
    
    number_regex = re.compile(r'(?<!\S)-?\d+\.\d+(?!\S)')
    
    for line in lines[1:]:
        if is_relevant(line):
            new_line = number_regex.sub(randomize_match, line)
            new_lines.append(new_line)
        else:
            new_lines.append(line)
    
    with open(filepath, 'w', encoding='latin-1') as f:
        f.writelines(new_lines)

if __name__ == "__main__":
    files = []
    for arg in sys.argv[1:]:
        if os.path.isfile(arg):
            files.append(arg)
        elif os.path.isdir(arg):
            for root, dirs, filenames in os.walk(arg):
                for filename in filenames:
                    if filename.lower().endswith('.uct'):
                        files.append(os.path.join(root, filename))
    
    for f in files:
        process_file(f)
