import re

with open("build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('alias(libs.plugins.secrets) apply false', 'alias(libs.plugins.secrets) apply false\n  alias(libs.plugins.hilt) apply false')

with open("build.gradle.kts", "w") as f:
    f.write(content)
