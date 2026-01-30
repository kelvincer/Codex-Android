package com.example.inventoryapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFF4CAF50)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5D6A7),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFF66BB6A)
)

@Composable
fun InventoryTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = WindowCompat.getInsetsController(view.context as android.app.Activity, view)
            window.isAppearanceLightStatusBars = !darkTheme
            (view.context as android.app.Activity).window.statusBarColor = colorScheme.primary.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
