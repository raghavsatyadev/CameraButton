package io.github.raghavsatyadev.camerabutton.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val viewModel = ViewModelProvider(this)[CameraRemoteViewModel::class.java]
    setContent { CameraButtonTheme { CameraRemoteScreen(viewModel) } }
  }
}
