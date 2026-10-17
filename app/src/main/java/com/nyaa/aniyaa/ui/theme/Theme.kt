package com.nyaa.aniyaa.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nyaa.aniyaa.data.model.DarkMode

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerLowest = Color(0xFF0F0D13),
    background = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFF3EDF7),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    background = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val surfaceLow: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color
)

private fun PaletteColors.toScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = onSurface,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = surfaceHigh,
        onTertiaryContainer = onSurface,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surfaceLow,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = background,
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceLow,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        outline = outline,
        outlineVariant = outlineVariant
    )
}

private val AmoledDark = PaletteColors(
    primary = Color(0xFF8ECAFF),
    onPrimary = Color(0xFF003355),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFD1E8FF),
    secondary = Color(0xFFB7C9D8),
    onSecondary = Color(0xFF22323D),
    tertiary = Color(0xFF9ECAFF),
    onTertiary = Color(0xFF003258),
    background = Color(0xFF000000),
    surfaceLow = Color(0xFF0A0A0A),
    surfaceHigh = Color(0xFF141414),
    surfaceHighest = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF3F6FA),
    onSurfaceVariant = Color(0xFFC5CDD6),
    outline = Color(0xFF8B949E),
    outlineVariant = Color(0xFF2C3136)
)

private val AmoledLight = PaletteColors(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    tertiary = Color(0xFF0277BD),
    onTertiary = Color.White,
    background = Color(0xFFF7F9FC),
    surfaceLow = Color(0xFFEEF3F8),
    surfaceHigh = Color(0xFFE3EAF2),
    surfaceHighest = Color(0xFFD7E1EC),
    onSurface = Color(0xFF12181F),
    onSurfaceVariant = Color(0xFF3E4A56),
    outline = Color(0xFF6E7C8A),
    outlineVariant = Color(0xFFD0D8E0)
)

private val InkDark = PaletteColors(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFC6C6C6),
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color(0xFF1A1A1A),
    background = Color(0xFF000000),
    surfaceLow = Color(0xFF0C0C0C),
    surfaceHigh = Color(0xFF161616),
    surfaceHighest = Color(0xFF222222),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFF2E2E2E)
)

private val InkLight = PaletteColors(
    primary = Color(0xFF1C1C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E6E6),
    onPrimaryContainer = Color(0xFF1C1C1C),
    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    tertiary = Color(0xFF5C5C5C),
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    surfaceLow = Color(0xFFF2F2F2),
    surfaceHigh = Color(0xFFE8E8E8),
    surfaceHighest = Color(0xFFDEDEDE),
    onSurface = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF3A3A3A),
    outline = Color(0xFF6B6B6B),
    outlineVariant = Color(0xFFD4D4D4)
)

private val UmiDark = PaletteColors(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF00332C),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB6FFF2),
    secondary = Color(0xFF8ED9CC),
    onSecondary = Color(0xFF00332C),
    tertiary = Color(0xFF7FDBEA),
    onTertiary = Color(0xFF003640),
    background = Color(0xFF041410),
    surfaceLow = Color(0xFF0C221C),
    surfaceHigh = Color(0xFF123028),
    surfaceHighest = Color(0xFF183E34),
    onSurface = Color(0xFFD8F6EF),
    onSurfaceVariant = Color(0xFFB4D4CC),
    outline = Color(0xFF7AA89E),
    outlineVariant = Color(0xFF2A4A42)
)

private val UmiLight = PaletteColors(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF004D40),
    onSecondary = Color.White,
    tertiary = Color(0xFF00838F),
    onTertiary = Color.White,
    background = Color(0xFFF2FBFA),
    surfaceLow = Color(0xFFE5F4F1),
    surfaceHigh = Color(0xFFD4EBE6),
    surfaceHighest = Color(0xFFC3E2DB),
    onSurface = Color(0xFF0D1F1C),
    onSurfaceVariant = Color(0xFF3D534C),
    outline = Color(0xFF6B857C),
    outlineVariant = Color(0xFFC5DDD6)
)

private val YuzuDark = PaletteColors(
    primary = Color(0xFFE6EE9C),
    onPrimary = Color(0xFF2A3000),
    primaryContainer = Color(0xFF4A5200),
    onPrimaryContainer = Color(0xFFF4F8C8),
    secondary = Color(0xFFD4E157),
    onSecondary = Color(0xFF2A3000),
    tertiary = Color(0xFFFFF59D),
    onTertiary = Color(0xFF3A3000),
    background = Color(0xFF101208),
    surfaceLow = Color(0xFF1E2210),
    surfaceHigh = Color(0xFF282E14),
    surfaceHighest = Color(0xFF343C1A),
    onSurface = Color(0xFFF4F6E4),
    onSurfaceVariant = Color(0xFFD2D8B4),
    outline = Color(0xFF9AA67A),
    outlineVariant = Color(0xFF3E4624)
)

private val YuzuLight = PaletteColors(
    primary = Color(0xFF5C6B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F4C3),
    onPrimaryContainer = Color(0xFF1A2000),
    secondary = Color(0xFF827717),
    onSecondary = Color.White,
    tertiary = Color(0xFFF9A825),
    onTertiary = Color(0xFF3E2723),
    background = Color(0xFFFBFCF3),
    surfaceLow = Color(0xFFF4F6E4),
    surfaceHigh = Color(0xFFEEF2D4),
    surfaceHighest = Color(0xFFE6EDC0),
    onSurface = Color(0xFF1C1E10),
    onSurfaceVariant = Color(0xFF454832),
    outline = Color(0xFF74785A),
    outlineVariant = Color(0xFFD8DEBC)
)

private val WisteriaDark = PaletteColors(
    primary = Color(0xFFD4C4F5),
    onPrimary = Color(0xFF2E1A4A),
    primaryContainer = Color(0xFF4A3470),
    onPrimaryContainer = Color(0xFFEDE4FF),
    secondary = Color(0xFFCDB8E8),
    onSecondary = Color(0xFF2E1A4A),
    tertiary = Color(0xFFF0C4E8),
    onTertiary = Color(0xFF3E1840),
    background = Color(0xFF120E18),
    surfaceLow = Color(0xFF201A2C),
    surfaceHigh = Color(0xFF2A2238),
    surfaceHighest = Color(0xFF362C46),
    onSurface = Color(0xFFF3ECFA),
    onSurfaceVariant = Color(0xFFD4C8E4),
    outline = Color(0xFFA090B8),
    outlineVariant = Color(0xFF463C56)
)

private val WisteriaLight = PaletteColors(
    primary = Color(0xFF6B4C8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE4F8),
    onPrimaryContainer = Color(0xFF2A1448),
    secondary = Color(0xFF7E5A9B),
    onSecondary = Color.White,
    tertiary = Color(0xFF9C6B9A),
    onTertiary = Color.White,
    background = Color(0xFFF8F6FC),
    surfaceLow = Color(0xFFF1ECF8),
    surfaceHigh = Color(0xFFE8E0F4),
    surfaceHighest = Color(0xFFDDD2EE),
    onSurface = Color(0xFF1C1428),
    onSurfaceVariant = Color(0xFF4A3E5C),
    outline = Color(0xFF7A6C8C),
    outlineVariant = Color(0xFFD8CEE6)
)

private val CrimsonDark = PaletteColors(
    primary = Color(0xFFFF8A80),
    onPrimary = Color(0xFF4E0008),
    primaryContainer = Color(0xFF8C1520),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF4E0008),
    tertiary = Color(0xFFFFCDD2),
    onTertiary = Color(0xFF4E0008),
    background = Color(0xFF120608),
    surfaceLow = Color(0xFF241014),
    surfaceHigh = Color(0xFF32161C),
    surfaceHighest = Color(0xFF421E26),
    onSurface = Color(0xFFFBEAEC),
    onSurfaceVariant = Color(0xFFE4C4C8),
    outline = Color(0xFFB0888E),
    outlineVariant = Color(0xFF5A3036)
)

private val CrimsonLight = PaletteColors(
    primary = Color(0xFFB71C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF8E2424),
    onSecondary = Color.White,
    tertiary = Color(0xFFC62828),
    onTertiary = Color.White,
    background = Color(0xFFFFF8F7),
    surfaceLow = Color(0xFFFCEEEC),
    surfaceHigh = Color(0xFFF8E0DC),
    surfaceHighest = Color(0xFFF4D2CC),
    onSurface = Color(0xFF2A1212),
    onSurfaceVariant = Color(0xFF5C3A3A),
    outline = Color(0xFF8C6464),
    outlineVariant = Color(0xFFE8C8C4)
)

private val PaperDark = PaletteColors(
    primary = Color(0xFFD7CCC8),
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFF5EDE6),
    secondary = Color(0xFFBCAAA4),
    onSecondary = Color(0xFF3E2723),
    tertiary = Color(0xFFE6CCB2),
    onTertiary = Color(0xFF3E2723),
    background = Color(0xFF14110E),
    surfaceLow = Color(0xFF26211C),
    surfaceHigh = Color(0xFF322C26),
    surfaceHighest = Color(0xFF403830),
    onSurface = Color(0xFFF6EFE6),
    onSurfaceVariant = Color(0xFFD7CBBE),
    outline = Color(0xFFA89888),
    outlineVariant = Color(0xFF4A4038)
)

private val PaperLight = PaletteColors(
    primary = Color(0xFF6D4C41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFE0D4),
    onPrimaryContainer = Color(0xFF2C1810),
    secondary = Color(0xFF8D6E63),
    onSecondary = Color.White,
    tertiary = Color(0xFFA67C52),
    onTertiary = Color.White,
    background = Color(0xFFF7F1E6),
    surfaceLow = Color(0xFFF3EBDD),
    surfaceHigh = Color(0xFFEADFCB),
    surfaceHighest = Color(0xFFE0D2BC),
    onSurface = Color(0xFF2C2118),
    onSurfaceVariant = Color(0xFF5C4E42),
    outline = Color(0xFF8A7768),
    outlineVariant = Color(0xFFDCCFBE)
)

private val darkSchemes = listOf(
    DefaultDarkColorScheme,
    darkColorScheme(
        primary = NyaaBlue80,
        secondary = NyaaBlueGrey80,
        tertiary = NyaaTeal80,
        surface = Color(0xFF0F1419),
        surfaceVariant = Color(0xFF151C24),
        surfaceContainer = Color(0xFF1A2330),
        surfaceContainerHigh = Color(0xFF1F2A38),
        surfaceContainerHighest = Color(0xFF253140),
        surfaceContainerLow = Color(0xFF151C24),
        surfaceContainerLowest = Color(0xFF0A0F14),
        background = Color(0xFF0F1419),
        onSurface = Color(0xFFE1E6ED),
        onSurfaceVariant = Color(0xFFB8C4D0),
        outline = Color(0xFF8899AA),
        outlineVariant = Color(0xFF3D4D5C)
    ),
    darkColorScheme(
        primary = Sakura80,
        secondary = SakuraGrey80,
        tertiary = SakuraPink80,
        surface = Color(0xFF1A1015),
        surfaceVariant = Color(0xFF22151C),
        surfaceContainer = Color(0xFF2A1B24),
        surfaceContainerHigh = Color(0xFF33222D),
        surfaceContainerHighest = Color(0xFF3C2936),
        surfaceContainerLow = Color(0xFF22151C),
        surfaceContainerLowest = Color(0xFF140C11),
        background = Color(0xFF1A1015),
        onSurface = Color(0xFFF0DEE6),
        onSurfaceVariant = Color(0xFFD4B8C4),
        outline = Color(0xFFA48898),
        outlineVariant = Color(0xFF534050)
    ),
    darkColorScheme(
        primary = Matcha80,
        secondary = MatchaGrey80,
        tertiary = MatchaLight80,
        surface = Color(0xFF0F1610),
        surfaceVariant = Color(0xFF151E16),
        surfaceContainer = Color(0xFF1A261C),
        surfaceContainerHigh = Color(0xFF1F2E22),
        surfaceContainerHighest = Color(0xFF253628),
        surfaceContainerLow = Color(0xFF151E16),
        surfaceContainerLowest = Color(0xFF0A110B),
        background = Color(0xFF0F1610),
        onSurface = Color(0xFFDCEBDE),
        onSurfaceVariant = Color(0xFFB4CCBA),
        outline = Color(0xFF85A08C),
        outlineVariant = Color(0xFF3D4F40)
    ),
    darkColorScheme(
        primary = Sunset80,
        secondary = SunsetGrey80,
        tertiary = SunsetLight80,
        surface = Color(0xFF1A120E),
        surfaceVariant = Color(0xFF221814),
        surfaceContainer = Color(0xFF2A1E1A),
        surfaceContainerHigh = Color(0xFF332520),
        surfaceContainerHighest = Color(0xFF3C2D27),
        surfaceContainerLow = Color(0xFF221814),
        surfaceContainerLowest = Color(0xFF140D0A),
        background = Color(0xFF1A120E),
        onSurface = Color(0xFFF0E0D8),
        onSurfaceVariant = Color(0xFFD4BBB0),
        outline = Color(0xFFA49088),
        outlineVariant = Color(0xFF534740)
    ),
    AmoledDark.toScheme(dark = true),
    InkDark.toScheme(dark = true),
    UmiDark.toScheme(dark = true),
    YuzuDark.toScheme(dark = true),
    WisteriaDark.toScheme(dark = true),
    CrimsonDark.toScheme(dark = true),
    PaperDark.toScheme(dark = true)
)

private val lightSchemes = listOf(
    DefaultLightColorScheme,
    lightColorScheme(
        primary = NyaaPrimary,
        secondary = NyaaSecondary,
        tertiary = NyaaTertiary,
        surface = Color(0xFFF8FAFF),
        surfaceVariant = Color(0xFFEDF2FA),
        surfaceContainer = Color(0xFFEDF2FA),
        surfaceContainerHigh = Color(0xFFE2EAF5),
        surfaceContainerHighest = Color(0xFFD8E4F2),
        background = Color(0xFFF8FAFF)
    ),
    lightColorScheme(
        primary = APP_THEMES[2].primary,
        secondary = APP_THEMES[2].secondary,
        tertiary = APP_THEMES[2].tertiary,
        surface = Color(0xFFFFF8FA),
        surfaceVariant = Color(0xFFFAEDF2),
        surfaceContainer = Color(0xFFFAEDF2),
        surfaceContainerHigh = Color(0xFFF5E2EA),
        surfaceContainerHighest = Color(0xFFF2D8E4),
        background = Color(0xFFFFF8FA)
    ),
    lightColorScheme(
        primary = APP_THEMES[3].primary,
        secondary = APP_THEMES[3].secondary,
        tertiary = APP_THEMES[3].tertiary,
        surface = Color(0xFFF8FFF8),
        surfaceVariant = Color(0xFFEDFAED),
        surfaceContainer = Color(0xFFEDFAED),
        surfaceContainerHigh = Color(0xFFE2F5E2),
        surfaceContainerHighest = Color(0xFFD8F2D8),
        background = Color(0xFFF8FFF8)
    ),
    lightColorScheme(
        primary = APP_THEMES[4].primary,
        secondary = APP_THEMES[4].secondary,
        tertiary = APP_THEMES[4].tertiary,
        surface = Color(0xFFFFFAF8),
        surfaceVariant = Color(0xFFFAF0ED),
        surfaceContainer = Color(0xFFFAF0ED),
        surfaceContainerHigh = Color(0xFFF5E6E2),
        surfaceContainerHighest = Color(0xFFF2DDD8),
        background = Color(0xFFFFFAF8)
    ),
    AmoledLight.toScheme(dark = false),
    InkLight.toScheme(dark = false),
    UmiLight.toScheme(dark = false),
    YuzuLight.toScheme(dark = false),
    WisteriaLight.toScheme(dark = false),
    CrimsonLight.toScheme(dark = false),
    PaperLight.toScheme(dark = false)
)

internal val darkSchemeCount: Int get() = darkSchemes.size
internal val lightSchemeCount: Int get() = lightSchemes.size

data class ThemeSwatch(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val primaryContainer: Color,
    val onPrimary: Color
)

@Composable
fun rememberThemeSwatch(theme: AppTheme, darkTheme: Boolean): ThemeSwatch {
    val context = LocalContext.current
    return remember(theme, darkTheme) {
        if (theme.usesDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            ThemeSwatch(
                primary = scheme.primary,
                secondary = scheme.secondary,
                tertiary = scheme.tertiary,
                primaryContainer = scheme.primaryContainer,
                onPrimary = scheme.onPrimary
            )
        } else {
            ThemeSwatch(
                primary = theme.primary,
                secondary = theme.secondary,
                tertiary = theme.tertiary,
                primaryContainer = theme.secondary,
                onPrimary = Color.White
            )
        }
    }
}

private fun ColorScheme.forEdgeToEdge(): ColorScheme = copy(
    background = surface,
    onBackground = onSurface
)

@Composable
fun AniyaaTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    themeIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val theme = themeAt(themeIndex)
    val schemeIndex = theme.staticSchemeIndex.coerceIn(0, darkSchemes.lastIndex)
    val colorScheme = when {
        theme.usesDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamic.forEdgeToEdge()
        }
        darkTheme -> darkSchemes[schemeIndex].forEdgeToEdge()
        else -> lightSchemes[schemeIndex].forEdgeToEdge()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AniyaaShapes,
        content = content
    )
}
