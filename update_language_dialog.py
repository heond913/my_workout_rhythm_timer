import re

with open("app/src/main/java/com/example/ui/components/LanguageSelectionDialog.kt", "r") as f:
    content = f.read()

content = content.replace('onLanguageSelected: () -> Unit', 'onLanguageSelected: (String) -> Unit')
content = content.replace('onLanguageSelected()', 'onLanguageSelected("en")') # Wait, I'll need to do it precisely
with open("app/src/main/java/com/example/ui/components/LanguageSelectionDialog.kt", "w") as f:
    f.write(content)
