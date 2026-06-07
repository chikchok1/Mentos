import os
import random
import shutil

base_dir = "app/src/main/assets/character_layers"
grades = ["common", "rare", "unique", "legendary"]

# Find all files in subdirectories
items = []
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith('.png'):
            items.append(os.path.join(root, f))

# Assign random grades
random.shuffle(items)
total = len(items)

# Distribution: common 50%, rare 30%, unique 15%, legendary 5%
c_count = int(total * 0.5)
r_count = int(total * 0.3)
u_count = int(total * 0.15)
l_count = total - (c_count + r_count + u_count)

counts = [c_count, r_count, u_count, l_count]
grade_assignments = []
for i, g in enumerate(grades):
    grade_assignments.extend([g] * counts[i])

for i in range(total):
    src = items[i]
    grade = grade_assignments[i]
    # src looks like app/src/main/assets/character_layers/<category>/<filename>
    rel_path = os.path.relpath(src, base_dir)
    category = rel_path.split('/')[0]
    filename = os.path.basename(src)
    
    # Ignore existing grade folders if any
    if category in grades:
        continue
        
    dest_dir = os.path.join(base_dir, grade, category)
    os.makedirs(dest_dir, exist_ok=True)
    dest = os.path.join(dest_dir, filename)
    
    shutil.move(src, dest)

# Clean up empty original category folders
for d in os.listdir(base_dir):
    dir_path = os.path.join(base_dir, d)
    if os.path.isdir(dir_path) and d not in grades:
        if not os.listdir(dir_path):
            os.rmdir(dir_path)

print(f"Moved {total} files successfully.")
