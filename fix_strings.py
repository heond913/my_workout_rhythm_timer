with open("app/src/main/res/values/strings.xml", "r") as f:
    content = f.read()

content = content.replace('</resources>', '    <string name="empty"></string>\n</resources>')

with open("app/src/main/res/values/strings.xml", "w") as f:
    f.write(content)
