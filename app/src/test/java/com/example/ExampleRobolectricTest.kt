package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.animation.AnimationTrack
import com.example.animation.FramerateSettings
import com.example.animation.MovementPreset
import com.example.model3d.Mesh3D
import com.example.model3d.ModelParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    assertEquals("Photogrammetry 3D", appName)
  }

  @Test
  fun `generate 3D meshes verify topology and bounds`() {
    val amphora = Mesh3D.generateAmphora()
    assertTrue(amphora.vertices.isNotEmpty())
    assertTrue(amphora.triangles.isNotEmpty())
    assertTrue(amphora.polygonCount > 100)

    val objExport = ModelParsers.exportObj(amphora)
    assertTrue(objExport.contains("v "))
    assertTrue(objExport.contains("f "))

    val parsedObj = ModelParsers.parseObj("test_obj", objExport).getOrNull()
    assertNotNull(parsedObj)
    assertEquals(amphora.vertexCount, parsedObj?.vertexCount)
  }

  @Test
  fun `animation presets and framerate evaluation`() {
    val track = AnimationTrack().withPreset(MovementPreset.TURNTABLE_360)
    assertEquals(4.0f, track.durationSec, 0.01f)

    val transformStart = track.sampleTransform(0f)
    assertEquals(0f, transformStart.rotYDeg, 0.1f)

    val transformMid = track.sampleTransform(2.0f)
    assertEquals(180f, transformMid.rotYDeg, 5.0f)

    val fps60 = FramerateSettings(60)
    assertEquals(240, fps60.totalFrames(track.durationSec))
  }
}

