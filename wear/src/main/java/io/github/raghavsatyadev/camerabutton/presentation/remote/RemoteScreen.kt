package io.github.raghavsatyadev.camerabutton.presentation.remote

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.bluetooth.Host
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.Countdown
import io.github.raghavsatyadev.camerabutton.presentation.RemoteViewModel
import io.github.raghavsatyadev.camerabutton.presentation.ShotEvent
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

@Composable
fun RemoteScreen(viewModel: RemoteViewModel, onDevices: () -> Unit, onSettings: () -> Unit) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val countdown by viewModel.countdown.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current
  val pulse = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    viewModel.shots.collect { event ->
      when (event) {
        ShotEvent.Tick -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        is ShotEvent.Fired ->
          if (event.sent) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            pulse.animateTo(0.86f, tween(70))
            pulse.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
          } else {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
          }
      }
    }
  }

  RemoteContent(
    state = state,
    countdown = countdown,
    timerSeconds = settings.timerSeconds,
    pulse = pulse,
    onShoot = viewModel::shoot,
    onTimer = viewModel::cycleTimer,
    onDevices = onDevices,
    onSettings = onSettings,
  )
}

/** Phone chip on top, the shutter in the middle, timer and settings along the bottom curve. */
@Composable
private fun RemoteContent(
  state: RemoteState,
  countdown: Countdown?,
  timerSeconds: Int,
  pulse: Animatable<Float, AnimationVector1D>,
  onShoot: () -> Unit,
  onTimer: () -> Unit,
  onDevices: () -> Unit,
  onSettings: () -> Unit,
) {
  // The screen never scrolls, so a plain layout inside ScreenScaffold is fine.
  ScreenScaffold { contentPadding ->
    BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding)) {
      StatusChip(
        state = state,
        onClick = onDevices,
        modifier = Modifier.align(Alignment.TopCenter).width(maxWidth * 0.78f),
      )
      ShutterButton(
        enabled = state is RemoteState.Connected,
        countdown = countdown,
        pulse = pulse,
        onClick = onShoot,
        modifier = Modifier.align(Alignment.Center).size(maxWidth * 0.46f),
      )
      Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(maxWidth * 0.18f),
      ) {
        FilledTonalIconButton(
          onClick = onTimer,
          shapes = IconButtonDefaults.animatedShapes(),
          modifier = Modifier.size(IconButtonDefaults.SmallButtonSize),
        ) {
          if (timerSeconds == 0) {
            Icon(painterResource(R.drawable.ic_timer), stringResource(R.string.timer_off))
          } else {
            Text(
              text = stringResource(R.string.timer_short, timerSeconds),
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
        FilledTonalIconButton(
          onClick = onSettings,
          shapes = IconButtonDefaults.animatedShapes(),
          modifier = Modifier.size(IconButtonDefaults.SmallButtonSize),
        ) {
          Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings))
        }
      }
    }
  }
}

@Composable
private fun StatusChip(state: RemoteState, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val connected = state is RemoteState.Connected
  val label =
    when (state) {
      is RemoteState.Connected -> state.host.name
      is RemoteState.Connecting -> stringResource(R.string.status_connecting, state.host.name)
      RemoteState.Starting -> stringResource(R.string.status_starting)
      else -> stringResource(R.string.status_choose_phone)
    }
  CompactButton(
    onClick = onClick,
    modifier = modifier,
    colors =
      if (connected) ButtonDefaults.filledTonalButtonColors()
      else ButtonDefaults.outlinedButtonColors(),
    border = if (connected) null else ButtonDefaults.outlinedButtonBorder(enabled = true),
    icon = {
      Icon(
        painter = painterResource(if (connected) R.drawable.ic_phone else R.drawable.ic_bluetooth),
        contentDescription = null,
      )
    },
    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
  )
}

private val previewHost = Host("Mi 11X Pro", "00:11:22:33:44:55", isPhone = true)

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RemoteConnectedPreview() {
  CameraButtonTheme {
    RemoteContent(
      state = RemoteState.Connected(previewHost),
      countdown = null,
      timerSeconds = 3,
      pulse = remember { Animatable(1f) },
      onShoot = {},
      onTimer = {},
      onDevices = {},
      onSettings = {},
    )
  }
}

@WearPreviewDevices
@Composable
private fun RemoteCountdownPreview() {
  CameraButtonTheme {
    RemoteContent(
      state = RemoteState.Connected(previewHost),
      countdown = Countdown(left = 2, total = 5),
      timerSeconds = 5,
      pulse = remember { Animatable(1f) },
      onShoot = {},
      onTimer = {},
      onDevices = {},
      onSettings = {},
    )
  }
}

@WearPreviewDevices
@Composable
private fun RemoteReadyPreview() {
  CameraButtonTheme {
    RemoteContent(
      state = RemoteState.Ready,
      countdown = null,
      timerSeconds = 0,
      pulse = remember { Animatable(1f) },
      onShoot = {},
      onTimer = {},
      onDevices = {},
      onSettings = {},
    )
  }
}
