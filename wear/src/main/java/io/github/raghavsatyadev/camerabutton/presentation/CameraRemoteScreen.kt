package io.github.raghavsatyadev.camerabutton.presentation

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun CameraRemoteScreen(viewModel: CameraRemoteViewModel) {
  val connectionState by viewModel.connectionState.collectAsState()
  val pairedDevices by viewModel.pairedDevices.collectAsState()
  val registered by viewModel.registered.collectAsState()
  val rejectedByHost by viewModel.rejectedByHost.collectAsState()
  val context = LocalContext.current

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      permissions ->
      val granted = permissions.entries.all { it.value }
      if (granted) {
        viewModel.initializeBluetooth()
      } else {
        Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
      }
    }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.BLUETOOTH_CONNECT,
          Manifest.permission.BLUETOOTH_ADVERTISE,
          Manifest.permission.BLUETOOTH_SCAN,
        )
      )
    } else {
      viewModel.initializeBluetooth()
    }
  }

  CameraRemoteContent(
    connectionState = connectionState,
    registered = registered,
    rejectedByHost = rejectedByHost,
    pairedDevices = pairedDevices,
    onTrigger = { key -> viewModel.triggerShutter(key) },
    onConnect = viewModel::connectToDevice,
    onRefreshDevices = viewModel::refreshPairedDevices,
  )
}

@Composable
fun CameraRemoteContent(
  connectionState: String,
  registered: Boolean,
  rejectedByHost: Boolean,
  pairedDevices: List<BluetoothDevice>,
  onTrigger: (ShutterKey) -> Unit,
  onConnect: (BluetoothDevice) -> Unit,
  onRefreshDevices: () -> Unit,
) {
  val listState = rememberScalingLazyListState()
  val connected = connectionState == "Connected"
  // Round screens clip the corners of full-width rows, so inset by ~10% of the screen width.
  val sidePadding = (LocalConfiguration.current.screenWidthDp * 0.1f).dp
  Scaffold(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
    timeText = { TimeText() },
    positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
  ) {
    // One scrolling list for everything: it keeps rows inside the round screen's safe area.
    ScalingLazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(horizontal = sidePadding, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      item {
        ListHeader {
          Text(
            text = connectionState,
            textAlign = TextAlign.Center,
            color = if (connected) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
          )
        }
      }
      statusHint(registered, rejectedByHost, connected)?.let { hint ->
        item {
          Text(
            text = hint,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
          )
        }
      }
      if (connected) {
        items(ShutterKey.entries) { key ->
          WideChip(label = key.label, primary = key == ShutterKey.VOLUME_UP) { onTrigger(key) }
        }
      } else {
        items(pairedDevices) { device ->
          WideChip(label = device.name ?: "Unknown", primary = true) { onConnect(device) }
        }
        item { WideChip(label = "Refresh", primary = false, onClick = onRefreshDevices) }
      }
    }
  }
}

private fun statusHint(registered: Boolean, rejectedByHost: Boolean, connected: Boolean): String? =
  when {
    !registered -> "Starting remote… keep this app open"
    rejectedByHost -> "Phone refused. Re-pair the watch so the phone sees it as an input device"
    !connected -> "Tap your phone to connect"
    else -> null
  }

@Composable
private fun WideChip(label: String, primary: Boolean, onClick: () -> Unit) {
  Chip(
    label = {
      Text(text = label, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    },
    onClick = onClick,
    colors = if (primary) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
    modifier = Modifier.fillMaxWidth(),
  )
}

private val ShutterKey.label: String
  get() =
    when (this) {
      ShutterKey.VOLUME_UP -> "Volume up"
      ShutterKey.VOLUME_DOWN -> "Volume down"
      ShutterKey.ENTER -> "Enter"
      ShutterKey.SPACE -> "Space"
    }
