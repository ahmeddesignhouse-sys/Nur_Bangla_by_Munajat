package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("নূর বাংলা", appName)
  }

  @Test
  fun `offline quran bangla json loads and searches`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.QuranRepository(context)
    val surahs = repo.loadSurahs()
    org.junit.Assert.assertTrue(surahs.size >= 15)
    val hits = repo.searchVerses("করুণাময়")
    org.junit.Assert.assertTrue(hits.isNotEmpty())
  }
}
