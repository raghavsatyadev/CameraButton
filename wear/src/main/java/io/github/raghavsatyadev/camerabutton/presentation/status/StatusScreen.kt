package io.github.raghavsatyadev.camerabutton.presentation.status

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.bluetooth.RemoteState
import io.github.raghavsatyadev.camerabutton.presentation.theme.CameraButtonTheme

/** The runtime permissions the remote needs before it can register with Bluetooth. */
private val PERMISSIONS = buildList {
  add(Manifest.permission.BLUETOOTH_CONNECT)
  add(Manifest.permission.BLUETOOTH_ADVERTISE)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    add(Manifest.permission.POST_NOTIFICATIONS)
  }
}

private data class StatusCopy(
  @DrawableRes val icon: Int,
  @StringRes val title: Int,
  @StringRes val body: Int,
  @StringRes val action: Int,
)

private fun RemoteState.statusCopy(): StatusCopy =
  when (this) {
    RemoteState.NeedsPermission ->
      StatusCopy(R.drawable.ic_bluetooth, R.string.status_permission_title, R.string.status_permission_body, R.string.action_allow)
    RemoteState.BluetoothOff ->
      StatusCopy(R.drawable.ic_bluetooth, R.string.status_off_title, R.string.status_off_body, R.string.action_turn_on)
    RemoteState.Busy ->
      StatusCopy(R.drawable.ic_shutter, R.string.status_busy_title, R.string.status_busy_body, R.string.action_retry)
    else ->
      StatusCopy(R.drawable.ic_shutter, R.string.status_unsupported_title, R.string.status_unsupported_body, R.string.action_retry)
  }

/** Explains why the remote cannot work yet and offers the one action that fixes it. */
@Composable
fun StatusScreen(state: RemoteState, onRetry: () -> Unit) {
  val context = LocalContext.current
  // Android stops showing the permission dialog after two refusals; then send them to settings.
  var refusals by remember { mutableIntStateOf(0) }
  val permissions =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      granted ->
      if (granted[Manifest.permission.BLUETOOTH_CONNECT] != true) refusals++
      onRetry()
    }
  val enable =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      onRetry()
    }

  StatusContent(
    state = state,
    onAction = {
      when (state) {
        RemoteState.NeedsPermission ->
          if (refusals >= 2) context.openAppSettings()
          else permissions.launch(PERMISSIONS.toTypedArray())
        RemoteState.BluetoothOff ->
          try {
            enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
          } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
          } catch (_: SecurityException) {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
          }
        else -> onRetry()
      }
    },
  )
}

private fun Context.openAppSettings() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  )
}

@Composable
private fun StatusContent(state: RemoteState, onAction: () -> Unit) {
  val copy = state.statusCopy()
  val listState = rememberTransformingLazyColumnState()
  val spec = rememberTransformationSpec()

  ScreenScaffold(
    scrollState = listState,
    edgeButton = { EdgeButton(onClick = onAction) { Text(stringResource(copy.action)) } },
  ) { contentPadding ->
    TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
      item {
        Icon(
          painter = painterResource(copy.icon),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp).transformedHeight(this, spec),
        )
      }
      item {
        Text(
          text = stringResource(copy.title),
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
          modifier =
            Modifier.fillMaxWidth().padding(top = 6.dp).transformedHeight(this, spec),
        )
      }
      item {
        Text(
          text = stringResource(copy.body),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .transformedHeight(this, spec),
        )
      }
    }
  }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun PermissionPreview() {
  CameraButtonTheme { StatusContent(state = RemoteState.NeedsPermission, onAction = {}) }
}

@WearPreviewDevices
@Composable
private fun UnsupportedPreview() {
  CameraButtonTheme { StatusContent(state = RemoteState.Unsupported, onAction = {}) }
}
