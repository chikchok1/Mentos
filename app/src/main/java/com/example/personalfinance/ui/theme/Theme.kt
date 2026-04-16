package com.example.personalfinance.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = Blue500,
    onPrimary            = Color.White,
    primaryContainer     = Blue50,
    onPrimaryContainer   = Blue500,
    secondary            = Purple500,
    onSecondary          = Color.White,
    secondaryContainer   = Purple50,
    onSecondaryContainer = Purple500,
    background           = Color.White,
    onBackground         = Gray900,
    surface              = Color.White,
    onSurface            = Gray900,
    surfaceVariant       = Gray50,
    onSurfaceVariant     = Gray600,
    outline              = Gray200,
    error                = RedDanger,
)

@Composable
fun PersonalFinanceTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
