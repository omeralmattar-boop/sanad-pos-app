package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

val SanadDarkColorScheme = darkColorScheme(
    primary = SanadElectricBlue,
    onPrimary = Color.White,
    primaryContainer = SanadBlueContainer,
    onPrimaryContainer = SanadOnBlueContainer,
    secondary = SanadEmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = SanadEmeraldContainer,
    onSecondaryContainer = SanadOnEmeraldContainer,
    tertiary = SanadAmberWarning,
    onTertiary = SanadTextInverse,
    tertiaryContainer = SanadAmberContainer,
    onTertiaryContainer = SanadOnAmberContainer,
    error = SanadCoralRed,
    onError = Color.White,
    errorContainer = SanadRedContainer,
    onErrorContainer = SanadOnRedContainer,
    background = SanadNavyDark,
    onBackground = SanadTextPrimary,
    surface = SanadSurface,
    onSurface = SanadTextPrimary,
    surfaceVariant = SanadSurfaceElevated,
    onSurfaceVariant = SanadTextSecondary,
    outline = SanadBorder,
    outlineVariant = SanadBorderSubtle
)

val SanadShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SanadTheme(
    forceRtl: Boolean = true,
    immersiveFullScreen: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SanadNavyDark.toArgb()
                window.navigationBarColor = SanadNavyDeep.toArgb()
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
                if (immersiveFullScreen) {
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    val layoutDirection = if (forceRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = SanadDarkColorScheme,
            typography = Typography,
            shapes = SanadShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SanadTheme(forceRtl = true, immersiveFullScreen = true, content = content)
}
