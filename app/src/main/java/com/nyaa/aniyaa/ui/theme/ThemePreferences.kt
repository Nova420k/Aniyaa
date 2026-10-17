package com.nyaa.aniyaa.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val usesDynamicColor: Boolean = false,
    val staticSchemeIndex: Int = 0
)

val APP_THEMES = listOf(
    AppTheme(
        name = "Material You",
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
        usesDynamicColor = true,
        staticSchemeIndex = 0
    ),
    AppTheme("Nyaa Blue", NyaaPrimary, NyaaSecondary, NyaaTertiary, staticSchemeIndex = 1),
    AppTheme("Sakura Pink", Color(0xFFBF3059), Color(0xFF8B1E51), Color(0xFFFF6B9D), staticSchemeIndex = 2),
    AppTheme("Matcha Green", Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF66BB6A), staticSchemeIndex = 3),
    AppTheme("Sunset Orange", Color(0xFFE64A19), Color(0xFFBF360C), Color(0xFFFF7043), staticSchemeIndex = 4),
    AppTheme("Aniyaa Purple", Purple40, PurpleGrey40, Pink40, staticSchemeIndex = 0),
    AppTheme("AMOLED", Color(0xFF1565C0), Color(0xFF455A64), Color(0xFF4FC3F7), staticSchemeIndex = 5),
    AppTheme("Ink", Color(0xFF1C1C1C), Color(0xFF424242), Color(0xFF757575), staticSchemeIndex = 6),
    AppTheme("Umi", Color(0xFF00695C), Color(0xFF004D40), Color(0xFF00838F), staticSchemeIndex = 7),
    AppTheme("Yuzu", Color(0xFF5C6B00), Color(0xFF827717), Color(0xFFF9A825), staticSchemeIndex = 8),
    AppTheme("Wisteria", Color(0xFF6B4C8A), Color(0xFF7E5A9B), Color(0xFF9C6B9A), staticSchemeIndex = 9),
    AppTheme("Crimson", Color(0xFFB71C1C), Color(0xFF8E2424), Color(0xFFC62828), staticSchemeIndex = 10),
    AppTheme("Paper", Color(0xFF6D4C41), Color(0xFF8D6E63), Color(0xFFA67C52), staticSchemeIndex = 11)
)

fun themeAt(index: Int): AppTheme =
    APP_THEMES.getOrElse(index.coerceIn(0, APP_THEMES.lastIndex)) { APP_THEMES.first() }

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = prefs.getInt("theme_index", 0)
        set(value) {
            prefs.edit().putInt("theme_index", value).apply()
        }
}
