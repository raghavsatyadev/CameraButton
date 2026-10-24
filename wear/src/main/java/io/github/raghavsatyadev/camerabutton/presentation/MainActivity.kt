package io.github.raghavsatyadev.camerabutton.presentation

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.raghavsatyadev.camerabutton.CameraButtonApp
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: RemoteViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent { CameraButtonTheme { CameraButtonNav(viewModel) } }

    // While a phone is connected the watch is a viewfinder companion: keep it awake if asked to.
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(viewModel.state, viewModel.settings) { state, settings ->
            state is RemoteState.Connected && settings.keepScreenOn
          }
          .collect { awake ->
            if (awake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    (application as CameraButtonApp).remote.setUiVisible(true)
  }

  override fun onStop() {
    (application as CameraButtonApp).remote.setUiVisible(false)
    super.onStop()
  }

  /** Programmable hardware buttons on watches that have them also fire the shutter. */
  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (keyCode in STEM_KEYS && event.repeatCount == 0) {
      viewModel.shoot()
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  private companion object {
    val STEM_KEYS = setOf(KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2, KeyEvent.KEYCODE_STEM_3)
  }
}
