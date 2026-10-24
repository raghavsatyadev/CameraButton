package io.github.raghavsatyadev.camerabutton.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.raghavsatyadev.camerabutton.bluetooth.ShutterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Settings(
  val shutterKey: ShutterKey = ShutterKey.VOLUME_UP,
  val timerSeconds: Int = 0,
  val keepScreenOn: Boolean = true,
  val lastHostAddress: String? = null,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
  private val store = context.applicationContext.dataStore

  val settings: Flow<Settings> = store.data.map { it.toSettings() }

  suspend fun current(): Settings = settings.first()

  suspend fun setShutterKey(key: ShutterKey) = store.edit { it[SHUTTER_KEY] = key.name }

  suspend fun setTimerSeconds(seconds: Int) = store.edit { it[TIMER] = seconds }

  suspend fun setKeepScreenOn(on: Boolean) = store.edit { it[KEEP_SCREEN_ON] = on }

  suspend fun setLastHost(address: String) = store.edit { it[LAST_HOST] = address }

  private fun Preferences.toSettings() =
    Settings(
      shutterKey = this[SHUTTER_KEY]?.let { name -> ShutterKey.entries.firstOrNull { it.name == name } }
        ?: ShutterKey.VOLUME_UP,
      timerSeconds = this[TIMER] ?: 0,
      keepScreenOn = this[KEEP_SCREEN_ON] ?: true,
      lastHostAddress = this[LAST_HOST],
    )

  companion object {
    /** Self-timer choices offered in settings and cycled by the timer button. */
    val TIMER_CHOICES = listOf(0, 3, 5, 10)

    private val SHUTTER_KEY = stringPreferencesKey("shutter_key")
    private val TIMER = intPreferencesKey("timer_seconds")
    private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val LAST_HOST = stringPreferencesKey("last_host")
  }
}
