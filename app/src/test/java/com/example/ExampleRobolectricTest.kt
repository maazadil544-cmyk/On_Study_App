package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Province
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
    assertEquals("ON Study", appName)
  }

  @Test
  fun `verify all 4 provinces configured`() {
    val provinces = Province.values()
    assertEquals(4, provinces.size)
  }
}
