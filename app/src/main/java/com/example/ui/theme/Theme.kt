package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF5EEAD4), // Desaturated teal (Teal 300) for premium touch
    secondary = Color(0xFF94A3B8), // Slate gray for secondary details
    tertiary = Color(0xFF60A5FA), // Desaturated blue (Blue 400) for Lunge highlight
    background = Color(0xFF121212), // 1. 앱의 메인 기본 배경 (Dark Background)
    surface = Color(0xFF1E1E1E), // 1. 일반 인풋 카드 및 리스트 컨테이너 (Dark Surface)
    surfaceVariant = Color(0xFF242424), // 1. 소셜 공유, 스트릭, 숫자 인풋창 등 Elevated Surface
    onPrimary = Color(0xFF121212), // High contrast dark text on light desaturated primary
    onSecondary = Color(0xFF121212),
    onTertiary = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3), // 2. 중요도가 가장 높은 타이틀 및 메인 데이터 (High Emphasis)
    onSurface = Color(0xFFE3E3E3), // 2. 중요도가 가장 높은 타이틀 및 메인 데이터 (High Emphasis)
    onSurfaceVariant = Color(0xFF94A3B8), // 2. 서브 라벨 및 설명 문구 (Medium Emphasis)
    outline = Color(0xFF2C2C2C) // 1. 다크모드 카드 테두리 (Stroke)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VibrantTealActive,
    onPrimary = Color.White,
    primaryContainer = VibrantTealSoftBg,
    onPrimaryContainer = VibrantTealDeepContrast,
    secondary = VibrantLungeIndigo,
    onSecondary = Color.White,
    secondaryContainer = VibrantLungeLavenderBg,
    onSecondaryContainer = Color(0xFF001B3E),
    tertiary = VibrantPlankCrimson,
    onTertiary = Color.White,
    tertiaryContainer = VibrantPlankCoralBg,
    onTertiaryContainer = Color(0xFF410002),
    background = VibrantSoftGreenBg,
    onBackground = VibrantCharcoalDark,
    surface = VibrantSoftGreenBg,
    onSurface = VibrantCharcoalDark,
    surfaceVariant = VibrantPaleMintGray,
    onSurfaceVariant = VibrantSlateGrey,
    outline = VibrantLightBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
