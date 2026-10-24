package io.github.raghavsatyadev.camerabutton.bluetooth

/** Where the watch is on the way to being a working shutter remote. */
sealed interface RemoteState {
  /** The watch has no Bluetooth or its stack does not offer the HID Device profile. */
  data object Unsupported : RemoteState

  data object NeedsPermission : RemoteState

  data object BluetoothOff : RemoteState

  /** Binding the HID profile and registering the remote with the Bluetooth stack. */
  data object Starting : RemoteState

  /** Another app holds the watch's single HID Device slot. */
  data object Busy : RemoteState

  /** Registered and waiting for a phone. */
  data object Ready : RemoteState

  data class Connecting(val host: Host) : RemoteState

  data class Connected(val host: Host) : RemoteState
}

/** A phone (or anything else) the watch can act as a remote for. */
data class Host(val name: String, val address: String, val isPhone: Boolean)
