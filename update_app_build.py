import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('alias(libs.plugins.secrets)', 'alias(libs.plugins.secrets)\n  alias(libs.plugins.hilt)')
content = content.replace('implementation(platform(libs.firebase.bom))', 'implementation(platform(libs.firebase.bom))\n  implementation(libs.firebase.analytics)\n  implementation(libs.hilt.android)\n  ksp(libs.hilt.compiler)\n  implementation(libs.androidx.hilt.navigation.compose)')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
