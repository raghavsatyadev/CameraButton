package io.github.raghavsatyadev.camerabutton.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class CameraRemoteViewModel(application: Application) : AndroidViewModel(application) {

  private val TAG = "CameraRemoteVM"
  private val bluetoothManager =
    application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
  private val executor = Executors.newSingleThreadExecutor()

  private var hidDevice: BluetoothHidDevice? = null
  private var hostDevice: BluetoothDevice? = null

  private val _connectionState = MutableStateFlow("Disconnected")
  val connectionState = _connectionState.asStateFlow()

  private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
  val pairedDevices = _pairedDevices.asStateFlow()

  private val callback =
    object : BluetoothHidDevice.Callback() {
      override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        super.onAppStatusChanged(pluggedDevice, registered)
        Log.d(TAG, "onAppStatusChanged: registered=$registered")
        if (registered) {
          // Determine initial connection state if pluggedDevice is available
          if (pluggedDevice != null) {
            // It seems we might be connected
          }
        }
      }

      override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        super.onConnectionStateChanged(device, state)
        Log.d(TAG, "onConnectionStateChanged: state=$state")
        _connectionState.value =
          when (state) {
            BluetoothProfile.STATE_CONNECTED -> {
              hostDevice = device
              "Connected"
            }
            BluetoothProfile.STATE_CONNECTING -> "Connecting"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            BluetoothProfile.STATE_DISCONNECTED -> {
              if (device == hostDevice) hostDevice = null
              "Disconnected"
            }
            else -> "Unknown"
          }
      }
    }

  private val serviceListener =
    object : BluetoothProfile.ServiceListener {
      override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
        if (profile == BluetoothProfile.HID_DEVICE) {
          Log.d(TAG, "HID Device Proxy Connected")
          hidDevice = proxy as BluetoothHidDevice
          registerHidApp()
          refreshPairedDevices()
        }
      }

      override fun onServiceDisconnected(profile: Int) {
        if (profile == BluetoothProfile.HID_DEVICE) {
          Log.d(TAG, "HID Device Proxy Disconnected")
          hidDevice = null
        }
      }
    }

  @SuppressLint("MissingPermission")
  fun initializeBluetooth() {
    bluetoothAdapter?.getProfileProxy(
      getApplication(),
      serviceListener,
      BluetoothProfile.HID_DEVICE,
    )
  }

  @SuppressLint("MissingPermission")
  fun refreshPairedDevices() {
    _pairedDevices.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
  }

  @SuppressLint("MissingPermission")
  private fun registerHidApp() {
    val sdpSettings =
      BluetoothHidDeviceAppSdpSettings(
        "Camera Remote",
        "Remote Shutter",
        "RaghavSatyadev",
        BluetoothHidDevice.SUBCLASS1_COMBO,
        Descriptor.BYTES,
      )

    hidDevice?.registerApp(sdpSettings, null, null, executor, callback)
  }

  @SuppressLint("MissingPermission")
  fun connectToDevice(device: BluetoothDevice) {
    Log.d(TAG, "Connecting to ${device.name}")
    hidDevice?.connect(device)
  }

  @SuppressLint("MissingPermission")
  fun triggerShutter(key: ShutterKey = ShutterKey.VOLUME_UP) {
    val device = hostDevice ?: return

    viewModelScope.launch(Dispatchers.IO) {
      Log.d(TAG, "Triggering Shutter: $key")
      when (key) {
        ShutterKey.ENTER -> {
          // Keyboard Report (ID 2)
          // Press Enter (0x28)
          val pressReport = ByteArray(8)
          pressReport[2] = 0x28 // Key Code for Enter
          hidDevice?.sendReport(device, Descriptor.ID_KEYBOARD, pressReport)
          delay(50)
          // Release
          val releaseReport = ByteArray(8)
          hidDevice?.sendReport(device, Descriptor.ID_KEYBOARD, releaseReport)
        }
        ShutterKey.SPACE -> {
          // Keyboard Report (ID 2)
          // Press Space (0x2C)
          val pressReport = ByteArray(8)
          pressReport[2] = 0x2C // Key Code for Space
          hidDevice?.sendReport(device, Descriptor.ID_KEYBOARD, pressReport)
          delay(50)
          // Release
          val releaseReport = ByteArray(8)
          hidDevice?.sendReport(device, Descriptor.ID_KEYBOARD, releaseReport)
        }
        else -> {
          // Consumer Control Report (ID 1)
          // bit 0: Vol Up, bit 1: Vol Down
          // Enum ordinals: VOLUME_UP=0, VOLUME_DOWN=1.
          val reportByte = (1 shl key.ordinal).toByte()
          hidDevice?.sendReport(device, Descriptor.ID_CONSUMER_CONTROL, byteArrayOf(reportByte))
          delay(50)
          hidDevice?.sendReport(device, Descriptor.ID_CONSUMER_CONTROL, byteArrayOf(0x00))
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    // HID device unregistration needs permission check, which we can't guarantee here easily
    // without suppressing
    // strictly speaking we should unregister, but often the system cleans up the proxy
    // If we want to be clean:
    try {
      hidDevice?.unregisterApp()
      bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

enum class ShutterKey {
  VOLUME_UP,
  VOLUME_DOWN,
  ENTER,
  SPACE,
}

object Descriptor {
  const val ID_CONSUMER_CONTROL = 1
  const val ID_KEYBOARD = 2

  val BYTES =
    byteArrayOf(
      // CONSUMER CONTROL (Report ID 1)
      0x05,
      0x0C, // USAGE_PAGE (Consumer Devices)
      0x09,
      0x01, // USAGE (Consumer Control)
      0xA1.toByte(),
      0x01, // COLLECTION (Application)
      0x85.toByte(),
      ID_CONSUMER_CONTROL.toByte(), // REPORT_ID (1)
      0x09,
      0xE9.toByte(), // USAGE (Volume Up)
      0x09,
      0xEA.toByte(), // USAGE (Volume Down)
      0x09,
      0x65.toByte(), // USAGE (Snapshot)
      0x09,
      0xCD.toByte(), // USAGE (Play/Pause)
      0x15,
      0x00, // LOGICAL_MINIMUM (0)
      0x25,
      0x01, // LOGICAL_MAXIMUM (1)
      0x75,
      0x01, // REPORT_SIZE (1)
      0x95.toByte(),
      0x04, // REPORT_COUNT (4) - 4 buttons
      0x81.toByte(),
      0x02, // INPUT (Data,Var,Abs)
      0x95.toByte(),
      0x04, // REPORT_COUNT (4) - Padding (8 - 4 = 4)
      0x81.toByte(),
      0x03, // INPUT (Cnst,Var,Abs)
      0xC0.toByte(), // END_COLLECTION

      // KEYBOARD (Report ID 2)
      0x05,
      0x01, // USAGE_PAGE (Generic Desktop)
      0x09,
      0x06, // USAGE (Keyboard)
      0xA1.toByte(),
      0x01, // COLLECTION (Application)
      0x85.toByte(),
      ID_KEYBOARD.toByte(), // REPORT_ID (2)
      0x05,
      0x07, //   USAGE_PAGE (Keyboard)
      0x19,
      0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
      0x29,
      0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
      0x15,
      0x00, //   LOGICAL_MINIMUM (0)
      0x25,
      0x01, //   LOGICAL_MAXIMUM (1)
      0x75,
      0x01, //   REPORT_SIZE (1)
      0x95.toByte(),
      0x08, //   REPORT_COUNT (8)
      0x81.toByte(),
      0x02, //   INPUT (Data,Var,Abs) - Modifiers
      0x95.toByte(),
      0x01, //   REPORT_COUNT (1)
      0x75,
      0x08, //   REPORT_SIZE (8)
      0x81.toByte(),
      0x03, //   INPUT (Cnst,Var,Abs) - Reserved
      0x95.toByte(),
      0x06, //   REPORT_COUNT (6)
      0x75,
      0x08, //   REPORT_SIZE (8)
      0x15,
      0x00, //   LOGICAL_MINIMUM (0)
      0x25,
      0x65, //   LOGICAL_MAXIMUM (101)
      0x05,
      0x07, //   USAGE_PAGE (Keyboard)
      0x19,
      0x00, //   USAGE_MINIMUM (Reserved (no event indicated))
      0x29,
      0x65, //   USAGE_MAXIMUM (Keyboard Application)
      0x81.toByte(),
      0x00, //   INPUT (Data,Ary,Abs) - Key arrays (6 bytes)
      0xC0.toByte(), // END_COLLECTION
    )
}
