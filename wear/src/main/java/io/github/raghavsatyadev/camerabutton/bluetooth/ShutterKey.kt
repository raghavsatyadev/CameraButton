package io.github.raghavsatyadev.camerabutton.bluetooth

import androidx.annotation.StringRes
import io.github.raghavsatyadev.camerabutton.R

/** A key the watch can press on the phone. Each one knows its own HID report. */
enum class ShutterKey(@StringRes val label: Int, @StringRes val hint: Int, val reportId: Int, private val usage: Int) {
  VOLUME_UP(R.string.key_volume_up, R.string.key_volume_up_hint, HidDescriptor.CONSUMER_REPORT_ID, 0xE9),
  VOLUME_DOWN(R.string.key_volume_down, R.string.key_volume_down_hint, HidDescriptor.CONSUMER_REPORT_ID, 0xEA),
  ENTER(R.string.key_enter, R.string.key_enter_hint, HidDescriptor.KEYBOARD_REPORT_ID, 0x28),
  SPACE(R.string.key_space, R.string.key_space_hint, HidDescriptor.KEYBOARD_REPORT_ID, 0x2C);

  fun pressReport(): ByteArray =
    when (reportId) {
      HidDescriptor.CONSUMER_REPORT_ID -> byteArrayOf((usage and 0xFF).toByte(), (usage shr 8).toByte())
      else -> ByteArray(8).also { it[2] = usage.toByte() }
    }

  fun releaseReport(): ByteArray =
    ByteArray(if (reportId == HidDescriptor.CONSUMER_REPORT_ID) 2 else 8)
}
