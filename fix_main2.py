with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

out_lines = []
found_isReady = False
for line in lines:
    if 'private var isReady = false' in line:
        if not found_isReady:
            out_lines.append(line)
            found_isReady = True
    else:
        out_lines.append(line)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.writelines(out_lines)
