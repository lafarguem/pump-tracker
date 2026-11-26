package com.lafarguem.pumptracker.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5C),
    primaryContainer = Color(0xFFA8F2DA),
    onPrimaryContainer = Color(0xFF002019),
    secondaryContainer = Color(0xFFCDE8DE),
    onSecondaryContainer = Color(0xFF07201A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD5BF),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFA8F2DA),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = Color(0xFFCDE8DE),
)

@Composable
fun PumpTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
