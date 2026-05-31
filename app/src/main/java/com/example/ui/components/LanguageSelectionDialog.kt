package com.example.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFBFDF9), // VibrantSoftGreenBg
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
                    color = Color(0xFF006A60) // tealActive
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.language_selection_subtitle),
                    fontSize = 14.sp,
                    color = Color(0xFF3F4947), // secondaryGray
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                    label = stringResource(id = R.string.language_english),
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
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
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFCCE8E3), // VibrantTealSoftBg
            contentColor = Color(0xFF00201C) // VibrantTealDeepContrast
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
