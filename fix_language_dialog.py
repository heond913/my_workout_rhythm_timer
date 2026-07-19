import re

with open("app/src/main/java/com/example/ui/components/LanguageSelectionDialog.kt", "r") as f:
    content = f.read()

# Fix the incorrect replacement
content = content.replace('onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")', 'onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")')
content = content.replace('onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ko")', 'onLanguageSelected("ko")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ko")')
content = content.replace('onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ja")', 'onLanguageSelected("ja")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ja")')
content = content.replace('onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("es")', 'onLanguageSelected("es")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("es")')
content = content.replace('onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("de")', 'onLanguageSelected("de")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("de")')
content = content.replace('onLanguageSelected("en")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("fr")', 'onLanguageSelected("fr")\n                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("fr")')

with open("app/src/main/java/com/example/ui/components/LanguageSelectionDialog.kt", "w") as f:
    f.write(content)
