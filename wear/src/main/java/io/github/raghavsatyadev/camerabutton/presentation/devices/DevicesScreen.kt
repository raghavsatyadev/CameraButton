package io.github.raghavsatyadev.camerabutton.presentation.devices

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import io.github.raghavsatyadev.camerabutton.bluetooth.Host
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.RemoteViewModel

private const val DISCOVERABLE_SECONDS = 120

/** Lists paired phones, pairs new ones and unpairs old ones. */
@Composable
fun DevicesScreen(viewModel: RemoteViewModel) {
  val context = LocalContext.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val hosts by viewModel.hosts.collectAsStateWithLifecycle()
  var pairing by remember { mutableStateOf(false) }
  var unpairing by remember { mutableStateOf<Host?>(null) }
  val listState = rememberTransformingLazyColumnState()

  val discoverable =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      // The system answers with the granted duration, or RESULT_CANCELED if the user said no.
      pairing = it.resultCode != Activity.RESULT_CANCELED
      if (pairing) viewModel.startPairing(it.resultCode.takeIf { s -> s > 0 } ?: DISCOVERABLE_SECONDS)
    }

  fun startPairing() {
    val intent =
      Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
    try {
      discoverable.launch(intent)
    } catch (_: ActivityNotFoundException) {
      context.openBluetoothSettings()
    } catch (_: SecurityException) {
      context.openBluetoothSettings()
    }
  }

  // Making the watch discoverable needs its own permission, separate from connecting.
  val advertise =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) startPairing() else context.openBluetoothSettings()
    }

  // The pairing steps sit above the list; bring them into view as soon as pairing starts.
  LaunchedEffect(pairing) { if (pairing) listState.animateScrollToItem(0) }
  LaunchedEffect(state) { if (state is RemoteState.Connected) pairing = false }

  DevicesContent(
    state = state,
    hosts = hosts,
    pairing = pairing,
    listState = listState,
    onHost = { host ->
      if (state.connectedAddress() != host.address) viewModel.connect(host.address)
    },
    onUnpair = { unpairing = it },
    onDisconnect = viewModel::disconnect,
    onPair = {
      if (context.canAdvertise()) startPairing()
      else advertise.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
    },
  )

  UnpairDialog(
    host = unpairing,
    onConfirm = { host ->
      unpairing = null
      // Some watches keep unpairing to the system; let the user forget the phone there.
      if (!viewModel.unpair(host.address)) context.openBluetoothSettings()
    },
    onDismiss = { unpairing = null },
  )
}

internal fun RemoteState.connectedAddress(): String? =
  when (this) {
    is RemoteState.Connected -> host.address
    is RemoteState.Connecting -> host.address
    else -> null
  }

private fun Context.canAdvertise() =
  ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) ==
    PackageManager.PERMISSION_GRANTED

private fun Context.openBluetoothSettings() {
  try {
    startActivity(
      Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  } catch (_: ActivityNotFoundException) {
    // Nothing else to offer; the pairing guide still explains what to do.
  }
}
