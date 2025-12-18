package io.github.raghavsatyadev.camerabutton.presentation

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun CameraRemoteScreen(viewModel: CameraRemoteViewModel) {
  val connectionState by viewModel.connectionState.collectAsState()
  val pairedDevices by viewModel.pairedDevices.collectAsState()
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
    pairedDevices = pairedDevices,
    onTrigger = { key -> viewModel.triggerShutter(key) },
    onConnect = viewModel::connectToDevice,
    onRefreshDevices = viewModel::refreshPairedDevices,
  )
}

@Composable
fun CameraRemoteContent(
  connectionState: String,
  pairedDevices: List<BluetoothDevice>,
  onTrigger: (ShutterKey) -> Unit,
  onConnect: (BluetoothDevice) -> Unit,
  onRefreshDevices: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "Status: $connectionState",
      color = MaterialTheme.colors.onBackground,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    if (connectionState == "Connected") {
      ScalingLazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        state = rememberScalingLazyListState(),
      ) {
        items(ShutterKey.values()) { key ->
          Button(
            onClick = { onTrigger(key) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
          ) {
            Text(text = key.name.replace("_", " "))
          }
        }
      }
    } else {
      Text(text = "Tap to Connect:", color = MaterialTheme.colors.secondary)
      ScalingLazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        state = rememberScalingLazyListState(),
      ) {
        items(pairedDevices) { device ->
          Chip(
            label = { Text(text = device.name ?: "Unknown") },
            onClick = { onConnect(device) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
          )
        }
        item {
          Button(onClick = onRefreshDevices, modifier = Modifier.padding(top = 4.dp)) {
            Text("Refresh")
          }
        }
      }
    }
  }
}
