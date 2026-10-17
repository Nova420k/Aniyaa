package com.nyaa.aniyaa.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferencesTest {

    @Test
    fun materialYouIsTheDefaultTheme() {
        assertEquals("Material You", APP_THEMES[0].name)
        assertTrue(APP_THEMES[0].usesDynamicColor)
        assertEquals(0, APP_THEMES[0].staticSchemeIndex)
    }

    @Test
    fun existingPaletteIndicesStayStable() {
        assertEquals("Nyaa Blue", APP_THEMES[1].name)
        assertEquals("Sakura Pink", APP_THEMES[2].name)
        assertEquals("Matcha Green", APP_THEMES[3].name)
        assertEquals("Sunset Orange", APP_THEMES[4].name)
        assertFalse(APP_THEMES[1].usesDynamicColor)
        assertFalse(APP_THEMES[2].usesDynamicColor)
        assertFalse(APP_THEMES[3].usesDynamicColor)
        assertFalse(APP_THEMES[4].usesDynamicColor)
    }

    @Test
    fun aniyaaPurpleIsAStaticOption() {
        val purple = APP_THEMES.single { it.name == "Aniyaa Purple" }
        assertFalse(purple.usesDynamicColor)
        assertEquals(0, purple.staticSchemeIndex)
        assertEquals(5, APP_THEMES.indexOf(purple))
    }

    @Test
    fun addedPalettesHaveTheirOwnSchemes() {
        assertEquals(
            listOf("AMOLED", "Ink", "Umi", "Yuzu", "Wisteria", "Crimson", "Paper"),
            APP_THEMES.drop(6).map { it.name }
        )
        val added = APP_THEMES.drop(6)
        assertEquals((5..11).toList(), added.map { it.staticSchemeIndex })
        assertTrue(added.all { !it.usesDynamicColor })
        assertEquals(darkSchemeCount, lightSchemeCount)
        assertTrue(APP_THEMES.all { it.staticSchemeIndex in 0 until darkSchemeCount })
    }

    @Test
    fun onlyMaterialYouUsesDynamicColor() {
        assertEquals(1, APP_THEMES.count { it.usesDynamicColor })
    }

    @Test
    fun themeAtCoercesOutOfRangeIndices() {
        assertEquals(APP_THEMES.first(), themeAt(-1))
        assertEquals(APP_THEMES.last(), themeAt(APP_THEMES.size + 4))
        assertEquals("Nyaa Blue", themeAt(1).name)
    }
}
