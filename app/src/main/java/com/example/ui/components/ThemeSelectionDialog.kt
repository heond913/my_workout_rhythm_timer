package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.AppTheme

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
    val lang = if (!appLocales.isEmpty) appLocales.get(0)?.language ?: "en" else java.util.Locale.getDefault().language

    val title = when (lang) {
        "ko" -> "화면 테마 설정"
        "ja" -> "テーマ設定"
        "es" -> "Ajustes de tema"
        "de" -> "Theme-Einstellungen"
        "fr" -> "Paramètres du thème"
        else -> "Theme Settings"
    }
    val subtitle = when (lang) {
        "ko" -> "원하는 화면 모드를 선택해 주세요."
        "ja" -> "表示モードを選択してください。"
        "es" -> "Elige el modo de pantalla."
        "de" -> "Wähle den Bildschirmmodus."
        "fr" -> "Choisissez le mode d'affichage."
        else -> "Choose your preferred display mode."
    }
    val lightLabel = when (lang) {
        "ko" -> "☀️ 라이트 모드"
        "ja" -> "☀️ ライトモード"
        "es" -> "☀️ Modo claro"
        "de" -> "☀️ Heller Modus"
        "fr" -> "☀️ Mode clair"
        else -> "☀️ Light Mode"
    }
    val darkLabel = when (lang) {
        "ko" -> "🌙 다크 모드"
        "ja" -> "🌙 ダークモード"
        "es" -> "🌙 Modo oscuro"
        "de" -> "🌙 Dunkler Modus"
        "fr" -> "🌙 Mode sombre"
        else -> "🌙 Dark Mode"
    }
    val systemLabel = when (lang) {
        "ko" -> "⚙️ 시스템 기본값"
        "ja" -> "⚙️ システムデフォルト"
        "es" -> "⚙️ Predeterminado"
        "de" -> "⚙️ Systemstandard"
        "fr" -> "⚙️ Par défaut du système"
        else -> "⚙️ System Default"
    }
    val closeButtonLabel = when (lang) {
        "ko" -> "닫기"
        "ja" -> "閉じる"
        "es" -> "Cerrar"
        "de" -> "Schließen"
        "fr" -> "Fermer"
        else -> "Close"
    }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme() || MaterialTheme.colorScheme.background == Color(0xFF121212) || MaterialTheme.colorScheme.background == Color(0xFF191C1B)
    val dialogBg = MaterialTheme.colorScheme.surface
    val activeTeal = MaterialTheme.colorScheme.primary
    val secondaryText = if (isSystemDark) Color(0xFFBEC9C6) else Color(0xFF3F4947)
    val primaryText = if (isSystemDark) Color(0xFFE1E3E0) else Color(0xFF191C1B)
    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = if (isSystemDark) Color(0xFF3F4945) else Color(0xFFDCE5E2)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTeal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = secondaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                val options = listOf(
                    AppTheme.LIGHT to lightLabel,
                    AppTheme.DARK to darkLabel,
                    AppTheme.SYSTEM to systemLabel
                )

                options.forEach { (theme, label) ->
                    val isSelected = currentTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) activeTeal else outlineColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onThemeSelected(theme) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onThemeSelected(theme) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = activeTeal,
                                unselectedColor = secondaryText
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = primaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(100),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = closeButtonLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
