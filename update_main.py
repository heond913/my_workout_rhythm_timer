import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace('class MainActivity : AppCompatActivity() {',
'''class MainActivity : AppCompatActivity() {
    private var isReady = false
    private lateinit var analytics: com.example.analytics.AnalyticsRepository

''', 1)

content = content.replace('super.onCreate(savedInstanceState)',
'''super.onCreate(savedInstanceState)
        analytics = com.example.analytics.FirebaseAnalyticsRepository(com.google.firebase.analytics.FirebaseAnalytics.getInstance(this))
        analytics.logAppOpen()
        
        com.example.ad.DailyAdManager.getInstance(this).setAnalyticsRepository(analytics)
''', 1)

content = content.replace('com.example.ad.DailyAdManager.getInstance(this)', '// initialized above', 1)

content = content.replace('    override fun onCreate(savedInstanceState: Bundle?) {',
'''    override fun onDestroy() {
        if (::analytics.isInitialized) {
            analytics.logAppClose()
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {''', 1)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
