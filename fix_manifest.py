import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

content = content.replace('<application', '''<attribution android:tag="@string/empty" android:label="@string/app_name" />
    <attribution android:tag="play-services-ads" android:label="@string/app_name" />
    <application''')

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
