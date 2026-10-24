package io.github.raghavsatyadev.camerabutton.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: CameraRemoteViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = ViewModelProvider(this)[CameraRemoteViewModel::class.java]
    setContent { CameraButtonTheme { CameraRemoteScreen(viewModel) } }
  }

  override fun onResume() {
    super.onResume()
    // HID registration is refused while the app is in the background, so retry each time it
    // comes back to the foreground.
    viewModel.ensureRegistered()
  }
}
