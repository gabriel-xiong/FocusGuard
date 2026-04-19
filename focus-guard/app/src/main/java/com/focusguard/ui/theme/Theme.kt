package com.focusguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// EarnedIt color system — IBM Carbon-inspired
// Primary: orange-red #E8553D, Secondary: IBM blue #0F62FE
object EarnedColors {
    // Brand
    val Primary = Color(0xFFE8553D)
    val PrimaryGlow = Color(0xFFF47B5C)
    val Secondary = Color(0xFF0F62FE)

    // Semantic
    val Focus = Color(0xFF3DA87A)       // green = good
    val Warning = Color(0xFFF59E0B)     // amber
    val Danger = Color(0xFFEF4444)      // red
    val Points = Color(0xFFD4910A)      // amber/gold

    // Light surfaces
    val LightBg = Color(0xFFF7F3EC)     // warm cream
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFFAF8F5)
    val LightForeground = Color(0xFF1A2233)
    val LightMuted = Color(0xFFEBE7E0)
    val LightMutedFg = Color(0xFF5A5F6B)
    val LightBorder = Color(0xFFD4A08A)

    // Dark surfaces
    val DarkBg = Color(0xFF141820)
    val DarkSurface = Color(0xFF1E2330)
    val DarkSurfaceElevated = Color(0xFF282E3C)
    val DarkForeground = Color(0xFFF0E8DD)
    val DarkMuted = Color(0xFF2A2F3B)
    val DarkMutedFg = Color(0xFF8B92A5)
    val DarkBorder = Color(0xFF2E3545)

    // Sidebar (bottom nav)
    val SidebarBg = Color(0xFF1A2E52)
    val SidebarFg = Color.White
}

// Custom "EarnedIt" dark scheme matching the React CSS
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEB6B54),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A13),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFF6B9BFF),
    onSecondary = Color.White,
    surface = EarnedColors.DarkSurface,
    surfaceVariant = EarnedColors.DarkSurfaceElevated,
    onSurface = EarnedColors.DarkForeground,
    onSurfaceVariant = EarnedColors.DarkMutedFg,
    background = EarnedColors.DarkBg,
    onBackground = EarnedColors.DarkForeground,
    outline = EarnedColors.DarkBorder,
    outlineVariant = EarnedColors.DarkBorder,
    error = EarnedColors.Danger,
)

private val LightColorScheme = lightColorScheme(
    primary = EarnedColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF3D1A13),
    secondary = EarnedColors.Secondary,
    onSecondary = Color.White,
    surface = EarnedColors.LightSurface,
    surfaceVariant = EarnedColors.LightMuted,
    onSurface = EarnedColors.LightForeground,
    onSurfaceVariant = EarnedColors.LightMutedFg,
    background = EarnedColors.LightBg,
    onBackground = EarnedColors.LightForeground,
    outline = EarnedColors.LightBorder,
    outlineVariant = EarnedColors.LightBorder,
    error = EarnedColors.Danger,
)

private val EarnedTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    ),
)

@Composable
fun FocusGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use our branded colors — no dynamic theming
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EarnedTypography,
        content = content
    )
}
