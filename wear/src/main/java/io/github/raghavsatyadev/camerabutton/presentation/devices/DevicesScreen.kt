package io.github.raghavsatyadev.camerabutton.presentation.devices

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.bluetooth.Host
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.RemoteViewModel
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

private const val DISCOVERABLE_SECONDS = 120

@Composable
fun DevicesScreen(viewModel: RemoteViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val hosts by viewModel.hosts.collectAsStateWithLifecycle()
  var pairing by remember { mutableStateOf(false) }
  val discoverable =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      // The system answers with the granted duration, or RESULT_CANCELED if the user said no.
      pairing = it.resultCode != Activity.RESULT_CANCELED
    }

  DevicesContent(
    state = state,
    hosts = hosts,
    pairing = pairing,
    onHost = { host ->
      if (state.connectedAddress() != host.address) viewModel.connect(host.address)
    },
    onDisconnect = viewModel::disconnect,
    onPair = {
      val intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
          .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
      try {
        discoverable.launch(intent)
      } catch (_: ActivityNotFoundException) {
        pairing = true
      } catch (_: SecurityException) {
        pairing = true
      }
    },
  )
}

private fun RemoteState.connectedAddress(): String? =
  when (this) {
    is RemoteState.Connected -> host.address
    is RemoteState.Connecting -> host.address
    else -> null
  }

@Composable
private fun DevicesContent(
  state: RemoteState,
  hosts: List<Host>,
  pairing: Boolean,
  onHost: (Host) -> Unit,
  onDisconnect: () -> Unit,
  onPair: () -> Unit,
) {
  val listState = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()
  val active = state.connectedAddress()

  ScreenScaffold(
    scrollState = listState,
    edgeButton = {
      EdgeButton(onClick = onPair, buttonSize = EdgeButtonSize.Medium) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        Text(stringResource(R.string.pair_new_phone))
      }
    },
  ) { contentPadding ->
    TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
      item {
        ListHeader(
          modifier =
            Modifier.minimumVerticalContentPadding(
                ListHeaderDefaults.minimumTopListContentPadding,
                ListHeaderDefaults.minimumBottomListContentPadding,
              )
              .transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
        ) {
          Text(stringResource(R.string.devices_title))
        }
      }
      if (pairing || hosts.isEmpty()) {
        item {
          Text(
            text =
              stringResource(if (pairing) R.string.pairing_guide else R.string.devices_empty),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .transformedHeight(this, spec),
          )
        }
      }
      items(count = hosts.size, key = { hosts[it].address }) { index ->
        val host = hosts[index]
        val selected = host.address == active
        Button(
          onClick = { onHost(host) },
          modifier =
            Modifier.minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
              .fillMaxWidth()
              .transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
          colors =
            if (selected) ButtonDefaults.buttonColors()
            else ButtonDefaults.filledTonalButtonColors(),
          icon = {
            Icon(
              painterResource(if (host.isPhone) R.drawable.ic_phone else R.drawable.ic_bluetooth),
              contentDescription = null,
            )
          },
          secondaryLabel = { Text(hostStatus(state, host)) },
          label = { Text(host.name, maxLines = 1) },
        )
      }
      if (active != null) {
        item {
          Button(
            onClick = onDisconnect,
            modifier =
              Modifier.minimumVerticalContentPadding(
                  ButtonDefaults.minimumVerticalListContentPadding
                )
                .fillMaxWidth()
                .transformedHeight(this, spec),
            transformation = SurfaceTransformation(spec),
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            label = { Text(stringResource(R.string.action_disconnect)) },
          )
        }
      }
    }
  }
}

@Composable
private fun hostStatus(state: RemoteState, host: Host): String =
  when {
    state is RemoteState.Connected && state.host.address == host.address ->
      stringResource(R.string.host_connected)
    state is RemoteState.Connecting && state.host.address == host.address ->
      stringResource(R.string.host_connecting)
    else -> stringResource(R.string.host_paired)
  }

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun DevicesPreview() {
  val phone = Host("Mi 11X Pro", "00:11:22:33:44:55", isPhone = true)
  CameraButtonTheme {
    DevicesContent(
      state = RemoteState.Connected(phone),
      hosts = listOf(phone, Host("Galaxy S25 Ultra", "66:77:88:99:AA:BB", isPhone = true)),
      pairing = false,
      onHost = {},
      onDisconnect = {},
      onPair = {},
    )
  }
}

@WearPreviewDevices
@Composable
private fun DevicesEmptyPreview() {
  CameraButtonTheme {
    DevicesContent(
      state = RemoteState.Ready,
      hosts = emptyList(),
      pairing = true,
      onHost = {},
      onDisconnect = {},
      onPair = {},
    )
  }
}
