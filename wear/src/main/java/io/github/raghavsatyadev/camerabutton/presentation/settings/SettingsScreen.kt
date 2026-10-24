package io.github.raghavsatyadev.camerabutton.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.bluetooth.ShutterKey
import io.github.raghavsatyadev.camerabutton.data.Settings
import io.github.raghavsatyadev.camerabutton.data.SettingsRepository
import io.github.raghavsatyadev.camerabutton.presentation.RemoteViewModel
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

@Composable
fun SettingsScreen(viewModel: RemoteViewModel) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  SettingsContent(
    settings = settings,
    onShutterKey = viewModel::setShutterKey,
    onTimer = viewModel::setTimer,
    onKeepScreenOn = viewModel::setKeepScreenOn,
  )
}

@Composable
private fun SettingsContent(
  settings: Settings,
  onShutterKey: (ShutterKey) -> Unit,
  onTimer: (Int) -> Unit,
  onKeepScreenOn: (Boolean) -> Unit,
) {
  val listState = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()

  ScreenScaffold(scrollState = listState) { contentPadding ->
    TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
      item { Header(stringResource(R.string.settings_shutter_key), spec) }
      items(count = ShutterKey.entries.size) { index ->
        val key = ShutterKey.entries[index]
        RadioButton(
          selected = settings.shutterKey == key,
          onSelect = { onShutterKey(key) },
          modifier =
            Modifier.minimumVerticalContentPadding(
                ButtonDefaults.minimumVerticalListContentPadding
              )
              .fillMaxWidth()
              .transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
          secondaryLabel = {
            Text(stringResource(key.hint), maxLines = 2, overflow = TextOverflow.Ellipsis)
          },
          label = { Text(stringResource(key.label)) },
        )
      }
      item { Header(stringResource(R.string.settings_timer), spec) }
      items(count = SettingsRepository.TIMER_CHOICES.size) { index ->
        val seconds = SettingsRepository.TIMER_CHOICES[index]
        RadioButton(
          selected = settings.timerSeconds == seconds,
          onSelect = { onTimer(seconds) },
          modifier =
            Modifier.minimumVerticalContentPadding(
                ButtonDefaults.minimumVerticalListContentPadding
              )
              .fillMaxWidth()
              .transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
          label = {
            Text(
              if (seconds == 0) stringResource(R.string.timer_off)
              else stringResource(R.string.timer_seconds, seconds)
            )
          },
        )
      }
      item { Header(stringResource(R.string.settings_screen), spec) }
      item {
        SwitchButton(
          checked = settings.keepScreenOn,
          onCheckedChange = onKeepScreenOn,
          modifier =
            Modifier.minimumVerticalContentPadding(
                ButtonDefaults.minimumVerticalListContentPadding
              )
              .fillMaxWidth()
              .transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
          secondaryLabel = {
            Text(
              stringResource(R.string.settings_keep_screen_on_hint),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          },
          label = { Text(stringResource(R.string.settings_keep_screen_on)) },
        )
      }
    }
  }
}

@Composable
private fun TransformingLazyColumnItemScope.Header(text: String, spec: TransformationSpec) {
  ListHeader(
    modifier =
      Modifier.minimumVerticalContentPadding(
          ListHeaderDefaults.minimumTopListContentPadding,
          ListHeaderDefaults.minimumBottomListContentPadding,
        )
        .transformedHeight(this, spec),
    transformation = SurfaceTransformation(spec),
  ) {
    Text(text)
  }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun SettingsPreview() {
  CameraButtonTheme {
    SettingsContent(
      settings = Settings(shutterKey = ShutterKey.VOLUME_UP, timerSeconds = 3, keepScreenOn = true),
      onShutterKey = {},
      onTimer = {},
      onKeepScreenOn = {},
    )
  }
}
