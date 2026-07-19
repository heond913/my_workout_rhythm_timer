import re

with open("app/src/main/java/com/example/ad/DailyAdManager.kt", "r") as f:
    content = f.read()

content = content.replace('class DailyAdManager private constructor(context: Context) : WorkoutAdManager {',
'''class DailyAdManager private constructor(context: Context) : WorkoutAdManager {
    private var analyticsRepository: com.example.analytics.AnalyticsRepository? = null

    fun setAnalyticsRepository(analytics: com.example.analytics.AnalyticsRepository) {
        this.analyticsRepository = analytics
    }
''')

with open("app/src/main/java/com/example/ad/DailyAdManager.kt", "w") as f:
    f.write(content)
