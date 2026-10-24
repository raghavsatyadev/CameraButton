package io.github.raghavsatyadev.camerabutton.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.raghavsatyadev.camerabutton.CameraButtonApp
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.presentation.MainActivity

/**
 * Keeps the process alive while a phone is connected, so the link survives the watch screen
 * turning off or the user leaving the app. Shows who is connected and offers Disconnect.
 */
class RemoteService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val remote = (application as CameraButtonApp).remote
    if (intent?.action == ACTION_DISCONNECT) {
      remote.disconnect()
      return START_NOT_STICKY
    }
    val host = (remote.state.value as? RemoteState.Connected)?.host
    if (host == null) {
      stopSelf()
      return START_NOT_STICKY
    }
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      notification(host),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
    )
    return START_NOT_STICKY
  }

  private fun notification(host: Host): android.app.Notification {
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
    )
    val open =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    val disconnect =
      PendingIntent.getService(
        this,
        1,
        Intent(this, RemoteService::class.java).setAction(ACTION_DISCONNECT),
        PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL)
      .setSmallIcon(R.drawable.ic_shutter)
      .setContentTitle(getString(R.string.notification_title))
      .setContentText(getString(R.string.notification_text, host.name))
      .setContentIntent(open)
      .addAction(R.drawable.ic_bluetooth, getString(R.string.action_disconnect), disconnect)
      .setOngoing(true)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  companion object {
    private const val TAG = "RemoteService"
    private const val CHANNEL = "remote"
    private const val NOTIFICATION_ID = 1
    private const val ACTION_DISCONNECT = "disconnect"

    /** Starts the service when [state] is connected and stops it otherwise. */
    fun sync(context: Context, state: RemoteState) {
      val intent = Intent(context, RemoteService::class.java)
      if (state !is RemoteState.Connected) {
        context.stopService(intent)
        return
      }
      try {
        ContextCompat.startForegroundService(context, intent)
      } catch (e: IllegalStateException) {
        // Android refuses foreground starts from the background; the link still works meanwhile.
        Log.w(TAG, "Could not start the foreground service", e)
      }
    }
  }
}
