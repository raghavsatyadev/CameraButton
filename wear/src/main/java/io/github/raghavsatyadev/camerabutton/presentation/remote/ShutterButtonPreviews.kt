package io.github.raghavsatyadev.camerabutton.presentation.remote

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import io.github.raghavsatyadev.camerabutton.presentation.Countdown
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

// Render with: android studio render-compose-preview <this file> ShutterButtonIdlePreview

@WearPreviewDevices
@Composable
fun ShutterButtonIdlePreview() {
  CameraButtonTheme {
    ShutterButton(enabled = true, countdown = null, pulse = remember { Animatable(0f) }, onClick = {})
  }
}

@WearPreviewDevices
@Composable
fun ShutterButtonCountdownPreview() {
  CameraButtonTheme {
    ShutterButton(
      enabled = true,
      countdown = Countdown(left = 2, total = 3),
      pulse = remember { Animatable(0f) },
      onClick = {},
    )
  }
}
