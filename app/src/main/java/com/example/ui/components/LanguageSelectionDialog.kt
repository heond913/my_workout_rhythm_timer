package com.example.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import com.example.R

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: () -> Unit
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == Color(0xFF121212)
    val dialogBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFBFDF9)
    val titleColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF006A60)
    val secondaryText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF3F4947)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.language_selection_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.language_selection_subtitle),
                    fontSize = 14.sp,
                    color = secondaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LanguageOption(
                    label = stringResource(id = R.string.language_english),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        onLanguageSelected()
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LanguageOption(
                    label = stringResource(id = R.string.language_korean),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ko")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        onLanguageSelected()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LanguageOption(
                    label = stringResource(id = R.string.language_japanese),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ja")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        onLanguageSelected()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LanguageOption(
                    label = stringResource(id = R.string.language_spanish),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("es")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        onLanguageSelected()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LanguageOption(
                    label = stringResource(id = R.string.language_german),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("de")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        onLanguageSelected()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LanguageOption(
                    label = stringResource(id = R.string.language_french),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("fr")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        onLanguageSelected()
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    label: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == Color(0xFF121212)
    val containerCol = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFFCCE8E3)
    val contentCol = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00201C)

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerCol,
            contentColor = contentCol
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
