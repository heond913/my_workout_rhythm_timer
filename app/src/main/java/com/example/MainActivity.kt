package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LanguageSelectionDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.LogScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Force system splash screen to remain on screen until isReady becomes true
        splashScreen.setKeepOnScreenCondition {
            !isReady
        }

        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()

        // Pre-initialize DailyAdManager and preload interstitial ad on app start
        com.example.ad.DailyAdManager.getInstance(this)

        // Set default language to English on initial installation (if no language selected yet)
        val prefs = getSharedPreferences("workout_rhythm_prefs", android.content.Context.MODE_PRIVATE)
        val isLangSelected = prefs.getBoolean("is_language_selected", false)
        if (!isLangSelected) {
            val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            if (appLocales.isEmpty) {
                val englishLocale = androidx.core.os.LocaleListCompat.forLanguageTags("en")
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(englishLocale)
            }
        }

        // Prompt for POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Fetch ViewModel instance early to kick off Room query and observe its emission
        val viewModelInstance = androidx.lifecycle.ViewModelProvider(this)[WorkoutViewModel::class.java]
        lifecycleScope.launch {
            // Wait for Room database flow to emit at least once, guaranteeing first read is done
            viewModelInstance.allRecords.first()
            // Wait an additional 150ms buffer to allow Jetpack Compose layout measurement to complete smoothly
            delay(150)
            isReady = true
        }

        setContent {
            val viewModel: WorkoutViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val currentLocale = configuration.locales[0]

            val isDarkTheme = when (uiState.appTheme) {
                com.example.viewmodel.AppTheme.LIGHT -> false
                com.example.viewmodel.AppTheme.DARK -> true
                com.example.viewmodel.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            androidx.compose.runtime.key(currentLocale, isDarkTheme) {
                MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                val workoutRecords by viewModel.allRecords.collectAsStateWithLifecycle(initialValue = emptyList())

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            WorkoutBottomBar(
                                currentTab = uiState.currentTab,
                                onTabSelected = { viewModel.setTab(it) }
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        val listTabs = listOf(AppTab.Timer, AppTab.Log, AppTab.Calendar, AppTab.Stats)
                        val pagerState = rememberPagerState(
                            initialPage = listTabs.indexOf(uiState.currentTab).coerceAtLeast(0),
                            pageCount = { listTabs.size }
                        )

                        // Sync from ViewModel tab selection to Page
                        LaunchedEffect(uiState.currentTab) {
                            val targetPage = listTabs.indexOf(uiState.currentTab).coerceAtLeast(0)
                            if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }

                        // Sync from Page swipe to ViewModel when completely settled
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.settledPage }.collect { settledPage ->
                                val targetTab = listTabs[settledPage]
                                if (uiState.currentTab != targetTab) {
                                    viewModel.setTab(targetTab)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true
                            ) { page ->
                                when (listTabs[page]) {
                                    AppTab.Timer -> TimerScreen(viewModel = viewModel)
                                    AppTab.Log -> LogScreen(viewModel = viewModel)
                                    AppTab.Calendar -> CalendarScreen(viewModel = viewModel, workoutRecords = workoutRecords)
                                    AppTab.Stats -> StatsScreen(viewModel = viewModel, workoutRecords = workoutRecords)
                                }
                            }
                        }
                    }

                    if (uiState.showLanguageSelection) {
                        LanguageSelectionDialog(
                            onDismiss = { /* Optionally handle dismiss if needed, or force selection */ },
                            onLanguageSelected = { viewModel.onLanguageSelected() }
                        )
                    }

                    if (uiState.showThemeSelection) {
                        com.example.ui.components.ThemeSelectionDialog(
                            currentTheme = uiState.appTheme,
                            onThemeSelected = { viewModel.updateAppTheme(it) },
                            onDismiss = { viewModel.updateShowThemeSelection(false) }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun WorkoutBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val barColor = Color(0xFFF1F5F2) // Vibrant light mint-grey
    val inactiveColor = Color(0xFF3F4947) // VibrantSlateGrey
    val activePillColor = Color(0xFFCCE8E3) // VibrantTealSoftBg
    val activeTextColor = Color(0xFF00201C) // VibrantTealDeepContrast

    NavigationBar(
        modifier = Modifier
            .testTag("workout_bottom_navigation_bar"),
        containerColor = barColor,
        tonalElevation = 8.dp
    ) {
        // Tab 1: Timer
        NavigationBarItem(
            selected = currentTab == AppTab.Timer,
            onClick = { onTabSelected(AppTab.Timer) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Rhythm Timer Tab"
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.tab_timer),
                    fontWeight = if (currentTab == AppTab.Timer) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = activePillColor,
                selectedIconColor = activeTextColor,
                selectedTextColor = activeTextColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            ),
            modifier = Modifier.testTag("tab_timer")
        )

        // Tab 2: Manual Log
        NavigationBarItem(
            selected = currentTab == AppTab.Log,
            onClick = { onTabSelected(AppTab.Log) },
            icon = {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = "Log Workout Tab"
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.tab_log),
                    fontWeight = if (currentTab == AppTab.Log) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = activePillColor,
                selectedIconColor = activeTextColor,
                selectedTextColor = activeTextColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            ),
            modifier = Modifier.testTag("tab_log")
        )

        // Tab 3: Calendar
        NavigationBarItem(
            selected = currentTab == AppTab.Calendar,
            onClick = { onTabSelected(AppTab.Calendar) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Workout Calendar Tab"
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.tab_calendar),
                    fontWeight = if (currentTab == AppTab.Calendar) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = activePillColor,
                selectedIconColor = activeTextColor,
                selectedTextColor = activeTextColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            ),
            modifier = Modifier.testTag("tab_calendar")
        )

        // Tab 4: Statistics
        NavigationBarItem(
            selected = currentTab == AppTab.Stats,
            onClick = { onTabSelected(AppTab.Stats) },
            icon = {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Workout Statistics Tab"
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.tab_stats),
                    fontWeight = if (currentTab == AppTab.Stats) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = activePillColor,
                selectedIconColor = activeTextColor,
                selectedTextColor = activeTextColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            ),
            modifier = Modifier.testTag("tab_stats")
        )
    }
}
