package com.example

import com.example.util.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateCheckerTest {

    @Test
    fun versionMetadataStaysAtV16Code7() {
        assertEquals("com.noorbangla.islam", BuildConfig.APPLICATION_ID)
        assertEquals(7, BuildConfig.VERSION_CODE)
        assertEquals("1.6", BuildConfig.VERSION_NAME)
        assertEquals(
            "https://raw.githubusercontent.com/ahmeddesignhouse-sys/nur-bangla-updates/main/version.json",
            UpdateChecker.VERSION_JSON_URL
        )
    }

    @Test
    fun parseVersionJsonHandlesOptionalAndForceUpdates() {
        val sampleV16 = """
            {
              "latestVersionCode": 7,
              "latestVersionName": "1.6",
              "apkUrl": "https://github.com/ahmeddesignhouse-sys/nur-bangla-updates/releases/download/v1.6/app-debug.apk",
              "changelog": "Guest Mode — use the app without Google Sign-In.",
              "forceUpdate": false
            }
        """.trimIndent()

        val parsedV16 = UpdateChecker.parseVersionJson(sampleV16)
        assertEquals(7, parsedV16.latestVersionCode)
        assertEquals("1.6", parsedV16.latestVersionName)
        assertEquals(
            "https://github.com/ahmeddesignhouse-sys/nur-bangla-updates/releases/download/v1.6/app-debug.apk",
            parsedV16.apkUrl
        )
        assertEquals("Guest Mode — use the app without Google Sign-In.", parsedV16.changelog)
        assertFalse(parsedV16.forceUpdate)

        val sampleV17Force = """
            {
              "latestVersionCode": 8,
              "latestVersionName": "1.7",
              "apkUrl": "https://github.com/ahmeddesignhouse-sys/nur-bangla-updates/releases/download/v1.7/app-debug.apk",
              "changelog": "Critical prayer time sync fix.",
              "forceUpdate": true
            }
        """.trimIndent()

        val parsedV17 = UpdateChecker.parseVersionJson(sampleV17Force)
        assertEquals(8, parsedV17.latestVersionCode)
        assertEquals("1.7", parsedV17.latestVersionName)
        assertTrue(parsedV17.forceUpdate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseVersionJsonRejectsMissingApkUrl() {
        UpdateChecker.parseVersionJson(
            """
            {
              "latestVersionCode": 8,
              "latestVersionName": "1.7",
              "apkUrl": "",
              "changelog": "Missing URL",
              "forceUpdate": false
            }
            """.trimIndent()
        )
    }

    @Test
    fun isUpdateAvailableComparesVersionCodeCorrectly() {
        assertFalse(UpdateChecker.isUpdateAvailable(latestVersionCode = 6, currentVersionCode = 7))
        assertFalse(UpdateChecker.isUpdateAvailable(latestVersionCode = 7, currentVersionCode = 7))
        assertTrue(UpdateChecker.isUpdateAvailable(latestVersionCode = 8, currentVersionCode = 7))
        assertFalse(UpdateChecker.isUpdateAvailable(latestVersionCode = 7))
        assertTrue(UpdateChecker.isUpdateAvailable(latestVersionCode = 8))
    }

    @Test
    fun formatMegabytesSupportsEnglishAndBangla() {
        val fiveMb = 5L * 1024L * 1024L
        assertEquals("5.0 MB", UpdateChecker.formatMegabytes(fiveMb, isEnglish = true))
        assertEquals("৫.০ মে.বা.", UpdateChecker.formatMegabytes(fiveMb, isEnglish = false))
    }
}
