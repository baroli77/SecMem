package com.secondmemory.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.secondmemory.app.domain.Appearance

val Paper = Color(0xFFF3EFE6)
val Ink = Color(0xFF1A1814)
val Forest = Color(0xFF2C5C4F)
val ForestOn = Color(0xFFF3EFE6)
val CardBg = Color(0xFFFAF8F3)
val Muted = Color(0xFFE7E1D4)
val MutedFg = Color(0xFF6A655C)
val Destructive = Color(0xFF8F3D32)

val PaperDark = Color(0xFF12110E)
val InkDark = Color(0xFFECE8DF)
val CardDark = Color(0xFF1C1A16)
val MutedDark = Color(0xFF2A2722)
val MutedFgDark = Color(0xFFA39C90)
val ForestDark = Color(0xFF8FB9AB)
val ForestContainerDark = Color(0xFF2C5C4F)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = ForestOn,
    primaryContainer = Color(0xFFD5E6DF),
    onPrimaryContainer = Forest,
    secondary = Color(0xFF5C574E),
    onSecondary = Paper,
    secondaryContainer = Muted,
    onSecondaryContainer = Ink,
    tertiary = Forest,
    onTertiary = ForestOn,
    tertiaryContainer = Color(0xFFD5E6DF),
    onTertiaryContainer = Forest,
    background = Paper,
    onBackground = Ink,
    surface = CardBg,
    onSurface = Ink,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedFg,
    error = Destructive,
    onError = Paper,
    outline = Color(0x331A1814),
    outlineVariant = Color(0x1A1A1814),
)

private val DarkColors = darkColorScheme(
    primary = ForestDark,
    onPrimary = PaperDark,
    primaryContainer = ForestContainerDark,
    onPrimaryContainer = Color(0xFFD5EDE4),
    secondary = Color(0xFFC4BDB0),
    onSecondary = PaperDark,
    secondaryContainer = Color(0xFF3A3530),
    onSecondaryContainer = InkDark,
    tertiary = ForestDark,
    onTertiary = PaperDark,
    tertiaryContainer = ForestContainerDark,
    onTertiaryContainer = Color(0xFFD5EDE4),
    background = PaperDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = MutedDark,
    onSurfaceVariant = MutedFgDark,
    error = Color(0xFFD98980),
    onError = PaperDark,
    outline = Color(0x33ECE8DF),
    outlineVariant = Color(0x1AECE8DF),
)

private val Typography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
    ),
)

@Composable
fun SecondMemoryTheme(
    appearance: Appearance = Appearance.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (appearance) {
        Appearance.DARK -> true
        Appearance.LIGHT -> false
        Appearance.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
