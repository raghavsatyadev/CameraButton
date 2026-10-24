package io.github.raghavsatyadev.camerabutton

import android.app.Application
import io.github.raghavsatyadev.camerabutton.bluetooth.HidRemote
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteService
import io.github.raghavsatyadev.camerabutton.data.SettingsRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/** Owns the process-wide remote so a phone connection outlives any one activity. */
class CameraButtonApp : Application() {
  private val scope = MainScope()

  val settings by lazy { SettingsRepository(this) }
  val remote by lazy { HidRemote(this, settings, scope) }

  override fun onCreate() {
    super.onCreate()
    // Keep a foreground service up exactly while a phone is connected.
    scope.launch { remote.state.collect { RemoteService.sync(this@CameraButtonApp, it) } }
  }
}
