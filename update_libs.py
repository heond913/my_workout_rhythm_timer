import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace('[versions]', '[versions]\nhilt = "2.51.1"\nhiltNavigationCompose = "1.2.0"')
content = content.replace('[libraries]', '[libraries]\nhilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }\nhilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }\nandroidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }\nfirebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }')
content = content.replace('[plugins]', '[plugins]\nhilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }')

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
