package io.github.raghavsatyadev.camerabutton.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.devices.DevicesScreen
import io.github.raghavsatyadev.camerabutton.presentation.remote.RemoteScreen
import io.github.raghavsatyadev.camerabutton.presentation.settings.SettingsScreen
import io.github.raghavsatyadev.camerabutton.presentation.status.StatusScreen

private sealed interface Route {
  data object Remote : Route

  data object Devices : Route

  data object Settings : Route
}

/** The app's single AppScaffold: a blocking status screen, or the remote and its two sub-screens. */
@Composable
fun CameraButtonNav(viewModel: RemoteViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val backStack = remember { mutableStateListOf<Route>(Route.Remote) }
  var error by remember { mutableStateOf<Int?>(null) }

  LaunchedEffect(Unit) { viewModel.messages.collect { error = it } }
  // Picking a phone on the devices screen returns to the shutter as soon as it connects.
  LaunchedEffect(state) {
    if (state is RemoteState.Connected && backStack.lastOrNull() == Route.Devices) {
      backStack.removeAt(backStack.lastIndex)
    }
  }

  AppScaffold {
    when (state) {
      RemoteState.Unsupported,
      RemoteState.NeedsPermission,
      RemoteState.BluetoothOff,
      RemoteState.Busy -> StatusScreen(state = state, onRetry = viewModel::refresh)
      else ->
        NavDisplay(
          backStack = backStack,
          onBack = { backStack.removeLastOrNull() },
          sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
          entryProvider =
            entryProvider {
              entry<Route.Remote> {
                RemoteScreen(
                  viewModel = viewModel,
                  onDevices = { backStack.add(Route.Devices) },
                  onSettings = { backStack.add(Route.Settings) },
                )
              }
              entry<Route.Devices> { DevicesScreen(viewModel) }
              entry<Route.Settings> { SettingsScreen(viewModel) }
            },
        )
    }

    val style = ConfirmationDialogDefaults.curvedTextStyle
    val text = error?.let { stringResource(it) }.orEmpty()
    FailureConfirmationDialog(
      visible = error != null,
      onDismissRequest = { error = null },
      curvedText = { confirmationDialogCurvedText(text, style) },
    )
  }
}
