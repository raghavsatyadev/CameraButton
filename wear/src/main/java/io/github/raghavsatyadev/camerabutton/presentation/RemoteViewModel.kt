package io.github.raghavsatyadev.camerabutton.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.camerabutton.CameraButtonApp
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.bluetooth.ShutterKey
import io.github.raghavsatyadev.camerabutton.data.Settings
import io.github.raghavsatyadev.camerabutton.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A running self-timer: [left] of [total] seconds remain. */
data class Countdown(val left: Int, val total: Int)

sealed interface ShotEvent {
  /** One second of the self-timer passed. */
  data object Tick : ShotEvent

  data class Fired(val sent: Boolean) : ShotEvent
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as CameraButtonApp
  private val remote = app.remote
  private val repository = app.settings

  val state: StateFlow<RemoteState> = remote.state
  val hosts = remote.hosts
  val messages = remote.messages
  val settings: StateFlow<Settings> =
    repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

  private val _countdown = MutableStateFlow<Countdown?>(null)
  val countdown: StateFlow<Countdown?> = _countdown.asStateFlow()

  private val _shots = MutableSharedFlow<ShotEvent>(extraBufferCapacity = 4)
  val shots: SharedFlow<ShotEvent> = _shots.asSharedFlow()

  private var timer: Job? = null

  /** Takes a photo, after the self-timer if one is set. Tapping during a countdown cancels it. */
  fun shoot() {
    if (state.value !is RemoteState.Connected) return
    if (timer?.isActive == true) {
      timer?.cancel()
      _countdown.value = null
      return
    }
    val seconds = settings.value.timerSeconds
    timer =
      viewModelScope.launch {
        try {
          for (left in seconds downTo 1) {
            _countdown.value = Countdown(left, seconds)
            _shots.emit(ShotEvent.Tick)
            delay(1_000)
          }
        } finally {
          _countdown.value = null
        }
        _shots.emit(ShotEvent.Fired(remote.press(settings.value.shutterKey)))
      }
  }

  fun cycleTimer() {
    val choices = SettingsRepository.TIMER_CHOICES
    val next = choices[(choices.indexOf(settings.value.timerSeconds) + 1) % choices.size]
    setTimer(next)
  }

  fun setTimer(seconds: Int) = viewModelScope.launch { repository.setTimerSeconds(seconds) }

  fun setShutterKey(key: ShutterKey) = viewModelScope.launch { repository.setShutterKey(key) }

  fun setKeepScreenOn(on: Boolean) = viewModelScope.launch { repository.setKeepScreenOn(on) }

  fun connect(address: String) = remote.connect(address)

  fun disconnect() = remote.disconnect()

  fun refresh() = remote.refresh()
}
