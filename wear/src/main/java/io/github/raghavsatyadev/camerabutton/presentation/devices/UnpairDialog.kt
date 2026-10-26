package io.github.raghavsatyadev.camerabutton.presentation.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.bluetooth.Host

/** Asks before forgetting [host]; hidden while [host] is null. */
@Composable
fun UnpairDialog(host: Host?, onConfirm: (Host) -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    visible = host != null,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.unpair_title, host?.name.orEmpty())) },
    text = { Text(stringResource(R.string.unpair_body), textAlign = TextAlign.Center) },
    confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = { host?.let(onConfirm) }) },
    dismissButton = { AlertDialogDefaults.DismissButton(onClick = onDismiss) },
  )
}
