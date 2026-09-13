package com.nyaa.aniyaa.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun isNewer_comparesDottedVersions() {
        assertTrue(UpdateChecker.latestUrls.first().contains("Nova420k/Aniyaa"))
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("v1.2.0", "1.1.9"))
        assertFalse(UpdateChecker.isNewer("1.1.0", "1.1.0"))
        assertFalse(UpdateChecker.isNewer("1.0.1", "1.1.0"))
    }

    @Test
    fun apkAssetUrl_picksApk() {
        val assets = listOf(
            "notes.txt" to "https://example/notes",
            "Aniyaa-v2.3.0.apk" to "https://example/app.apk"
        )
        assertEquals("https://example/app.apk", UpdateChecker.apkAssetUrl(assets))
        assertEquals("", UpdateChecker.apkAssetUrl(emptyList()))
    }
}
