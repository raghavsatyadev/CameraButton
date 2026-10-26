package io.github.raghavsatyadev.camerabutton.presentation.devices

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
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
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.bluetooth.Host
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

/** Pairing steps, the paired phones, and Disconnect/Unpair for the active one. */
@Composable
internal fun DevicesContent(
  state: RemoteState,
  hosts: List<Host>,
  pairing: Boolean,
  listState: TransformingLazyColumnState,
  onHost: (Host) -> Unit,
  onUnpair: (Host) -> Unit,
  onDisconnect: () -> Unit,
  onPair: () -> Unit,
) {
  val spec = rememberTransformationSpec()
  val active = state.connectedAddress()
  val activeHost = hosts.firstOrNull { it.address == active }

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
          Hint(
            text = stringResource(if (pairing) R.string.pairing_guide else R.string.devices_empty),
            spec = spec,
            highlight = pairing,
          )
        }
      }
      items(count = hosts.size, key = { hosts[it].address }) { index ->
        val host = hosts[index]
        Button(
          onClick = { onHost(host) },
          onLongClick = { onUnpair(host) },
          onLongClickLabel = stringResource(R.string.action_unpair),
          modifier =
            Modifier.minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
              .fillMaxWidth()
              .transformedHeight(this, spec),
          transformation = SurfaceTransformation(spec),
          colors =
            if (host.address == active) ButtonDefaults.buttonColors()
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
      if (activeHost != null) {
        item {
          OutlinedAction(stringResource(R.string.action_disconnect), spec, onDisconnect)
        }
        item {
          OutlinedAction(stringResource(R.string.action_unpair), spec) { onUnpair(activeHost) }
        }
      }
      if (hosts.isNotEmpty()) {
        item { Hint(text = stringResource(R.string.unpair_hint), spec = spec) }
      }
    }
  }
}

@Composable
private fun TransformingLazyColumnItemScope.OutlinedAction(
  label: String,
  spec: TransformationSpec,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    modifier =
      Modifier.minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
        .fillMaxWidth()
        .transformedHeight(this, spec),
    transformation = SurfaceTransformation(spec),
    colors = ButtonDefaults.outlinedButtonColors(),
    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    label = { Text(label) },
  )
}

@Composable
private fun TransformingLazyColumnItemScope.Hint(
  text: String,
  spec: TransformationSpec,
  highlight: Boolean = false,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color =
      if (highlight) MaterialTheme.colorScheme.onSurface
      else MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .transformedHeight(this, spec),
  )
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
      listState = rememberTransformingLazyColumnState(),
      onHost = {},
      onUnpair = {},
      onDisconnect = {},
      onPair = {},
    )
  }
}

@WearPreviewDevices
@Composable
private fun DevicesPairingPreview() {
  CameraButtonTheme {
    DevicesContent(
      state = RemoteState.Ready,
      hosts = emptyList(),
      pairing = true,
      listState = rememberTransformingLazyColumnState(),
      onHost = {},
      onUnpair = {},
      onDisconnect = {},
      onPair = {},
    )
  }
}
