package io.github.raghavsatyadev.camerabutton.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Turns the watch into a Bluetooth HID remote (keyboard plus media keys) for a paired phone.
 *
 * One instance lives for the whole process, so a connection survives the activity being recreated
 * or sent to the background. The remote stays registered while the UI is visible or a phone is
 * connected, and lets go of the HID slot otherwise. Bluetooth callbacks arrive on the main thread
 * and [scope] must be a main-thread scope.
 */
@SuppressLint("MissingPermission") // Every entry point checks hasConnectPermission() first.
class HidRemote(
  context: Context,
  private val settings: SettingsRepository,
  private val scope: CoroutineScope,
) {
  private val app = context.applicationContext
  private val adapter: BluetoothAdapter? =
    app.getSystemService(BluetoothManager::class.java)?.adapter
  private val executor = ContextCompat.getMainExecutor(app)

  private val _state = MutableStateFlow<RemoteState>(RemoteState.Starting)
  val state: StateFlow<RemoteState> = _state.asStateFlow()

  private val _hosts = MutableStateFlow<List<Host>>(emptyList())

  /** Bonded devices, phones first. */
  val hosts: StateFlow<List<Host>> = _hosts.asStateFlow()

  private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)

  /** One-off problems worth telling the user about, as string resources. */
  val messages: SharedFlow<Int> = _messages.asSharedFlow()

  private var hid: BluetoothHidDevice? = null
  private var registered = false
  private var uiVisible = false
  private var bindTimeout: Job? = null
  private var connectTimeout: Job? = null
  private var connectedAtMs = 0L

  /** True while an automatic reconnect runs; its failure is not worth an error dialog. */
  private var quietConnect = false

  /** While the watch is discoverable, stay registered so a pairing phone sees the remote. */
  private var pairingUntilMs = 0L

  /** A phone that paired while the remote was not registered; connect to it next. */
  private var newlyBonded: String? = null

  private val systemReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
          BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
            refreshHosts()
            val bonded =
              intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE) ==
                BluetoothDevice.BOND_BONDED
            val device =
              IntentCompat.getParcelableExtra(
                intent,
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java,
              )
            if (bonded && device != null) onBonded(device)
          }
          BluetoothAdapter.ACTION_STATE_CHANGED ->
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
              BluetoothAdapter.STATE_ON -> if (uiVisible) refresh()
              BluetoothAdapter.STATE_TURNING_OFF,
              BluetoothAdapter.STATE_OFF -> {
                dropProxy()
                _state.value = RemoteState.BluetoothOff
                refreshHosts()
              }
            }
        }
      }
    }

  init {
    val filter =
      IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
      }
    // Bluetooth broadcasts come from the Bluetooth app, not the system, so a non-exported receiver
    // never gets them. Both actions are protected broadcasts, so other apps can't fake them.
    ContextCompat.registerReceiver(app, systemReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
  }

  /** The UI calls this from onStart/onStop; it decides whether the remote should be registered. */
  fun setUiVisible(visible: Boolean) {
    uiVisible = visible
    if (visible) refresh() else releaseIfIdle()
  }

  /** Re-evaluates permissions and Bluetooth, then binds and registers as needed. */
  fun refresh() {
    val bt = adapter
    when {
      bt == null -> _state.value = RemoteState.Unsupported
      !hasConnectPermission() -> _state.value = RemoteState.NeedsPermission
      !bt.isEnabled -> _state.value = RemoteState.BluetoothOff
      hid == null -> bindProxy(bt)
      !registered -> register()
    }
    refreshHosts()
  }

  /** The watch is discoverable for [seconds]; keep the remote ready for a new phone until then. */
  fun startPairing(seconds: Int) {
    pairingUntilMs = SystemClock.elapsedRealtime() + seconds * 1_000L
    scope.launch {
      delay(seconds * 1_000L)
      releaseIfIdle()
    }
  }

  private fun isPairing() = SystemClock.elapsedRealtime() < pairingUntilMs

  /** Connects to [address]. A [quiet] attempt fails without telling the user. */
  fun connect(address: String, quiet: Boolean = false) {
    val device = hid ?: return
    val bt = adapter ?: return
    if (!registered || !hasConnectPermission()) return
    quietConnect = quiet
    val target = bt.getRemoteDevice(address)
    device.connectedDevices.filter { it.address != address }.forEach { device.disconnect(it) }
    _state.value = RemoteState.Connecting(target.toHost())
    if (!device.connect(target)) {
      _state.value = RemoteState.Ready
      if (!quiet) _messages.tryEmit(R.string.error_connect)
      return
    }
    connectTimeout?.cancel()
    connectTimeout =
      scope.launch {
        delay(CONNECT_TIMEOUT_MS)
        if (_state.value is RemoteState.Connecting) {
          _state.value = RemoteState.Ready
          if (!quiet) _messages.tryEmit(R.string.error_connect)
        }
      }
  }

  fun disconnect() {
    val device = hid ?: return
    if (!hasConnectPermission()) return
    device.connectedDevices.forEach { device.disconnect(it) }
  }

  /**
   * Forgets the pairing with [address] so the phone has to pair again. Android keeps this call
   * from ordinary apps on some watches; it returns false then and the caller sends the user to the
   * system Bluetooth settings.
   */
  fun unpair(address: String): Boolean {
    val bt = adapter ?: return false
    if (!hasConnectPermission()) return false
    val target = bt.getRemoteDevice(address)
    hid?.takeIf { target in it.connectedDevices }?.disconnect(target)
    scope.launch { settings.clearLastHost(address) }
    val removed =
      runCatching { target.javaClass.getMethod("removeBond").invoke(target) as Boolean }
        .onFailure { Log.w(TAG, "removeBond is not available", it) }
        .getOrDefault(false)
    Log.i(TAG, "unpair $address removed=$removed")
    refreshHosts()
    return removed
  }

  /** Presses and releases [key] on the connected phone. Returns false if nothing was sent. */
  suspend fun press(key: ShutterKey): Boolean {
    val connected = _state.value as? RemoteState.Connected ?: return false
    val device = hid ?: return false
    val bt = adapter ?: return false
    if (!hasConnectPermission()) return false
    val target = bt.getRemoteDevice(connected.host.address)
    val sent = device.sendReport(target, key.reportId, key.pressReport())
    delay(KEY_HOLD_MS)
    device.sendReport(target, key.reportId, key.releaseReport())
    if (!sent) Log.w(TAG, "sendReport failed for $key")
    return sent
  }

  private fun bindProxy(bt: BluetoothAdapter) {
    _state.value = RemoteState.Starting
    if (bindTimeout?.isActive == true) return
    if (!bt.getProfileProxy(app, proxyListener, BluetoothProfile.HID_DEVICE)) {
      Log.w(TAG, "HID Device profile proxy is not available")
      _state.value = RemoteState.Unsupported
      return
    }
    bindTimeout =
      scope.launch {
        delay(BIND_TIMEOUT_MS)
        if (!registered && _state.value == RemoteState.Starting) {
          Log.w(TAG, "HID Device profile did not come up in time")
          _state.value = RemoteState.Unsupported
        }
      }
  }

  private fun register() {
    val device = hid ?: return
    _state.value = RemoteState.Starting
    val sdp =
      BluetoothHidDeviceAppSdpSettings(
        app.getString(R.string.hid_name),
        app.getString(R.string.hid_description),
        app.getString(R.string.hid_provider),
        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
        HidDescriptor.BYTES,
      )
    if (!device.registerApp(sdp, null, null, executor, hidCallback)) {
      // Also refused while the app is not in the foreground; MainActivity retries from onResume.
      Log.w(TAG, "registerApp refused; app in background or another app holds the HID slot")
      bindTimeout?.cancel()
      _state.value = RemoteState.Busy
    }
  }

  private fun onRegistered(plugged: BluetoothDevice?) {
    bindTimeout?.cancel()
    val device = hid ?: return
    val already = device.connectedDevices.firstOrNull()
    if (already != null) {
      onConnected(already)
      return
    }
    if (_state.value !is RemoteState.Connecting) _state.value = RemoteState.Ready
    scope.launch {
      val bt = adapter ?: return@launch
      val fresh = newlyBonded?.let { bt.getRemoteDevice(it) }
      newlyBonded = null
      // While pairing, don't keep the radio busy calling an old phone that may not be around.
      val last =
        if (isPairing()) null
        else
          settings.current().lastHostAddress?.let { bt.getRemoteDevice(it) }?.takeIf {
            it.bondState == BluetoothDevice.BOND_BONDED
          }
      val target = plugged ?: fresh ?: last
      if (target != null && _state.value == RemoteState.Ready) {
        connect(target.address, quiet = target == last)
      }
    }
  }

  /** A phone that just paired with the watch usually wants the remote, so connect to it. */
  private fun onBonded(device: BluetoothDevice) {
    if (!uiVisible && !isPairing()) return
    if (!registered) {
      // The system pairing screen covered the app; connect once the remote is back.
      newlyBonded = device.address
      return
    }
    scope.launch {
      // Give the phone a moment to open the link itself; most do right after pairing.
      delay(BOND_CONNECT_DELAY_MS)
      if (_state.value == RemoteState.Ready) connect(device.address)
    }
  }

  private fun onConnected(device: BluetoothDevice) {
    connectTimeout?.cancel()
    connectedAtMs = SystemClock.elapsedRealtime()
    _state.value = RemoteState.Connected(device.toHost())
    scope.launch { settings.setLastHost(device.address) }
  }

  private fun releaseIfIdle() {
    val current = _state.value
    if (uiVisible || isPairing()) return
    if (current is RemoteState.Connected || current is RemoteState.Connecting) return
    bindTimeout?.cancel()
    connectTimeout?.cancel()
    val device = hid
    if (device != null && hasConnectPermission()) {
      if (registered) device.unregisterApp()
      adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
    }
    hid = null
    registered = false
    _state.value = RemoteState.Starting
  }

  private fun dropProxy() {
    bindTimeout?.cancel()
    connectTimeout?.cancel()
    hid = null
    registered = false
  }

  private fun refreshHosts() {
    val bt = adapter
    if (bt == null || !hasConnectPermission() || !bt.isEnabled) {
      _hosts.value = emptyList()
      return
    }
    _hosts.value =
      bt.bondedDevices
        .orEmpty()
        .map { it.toHost() }
        .sortedWith(compareBy({ !it.isPhone }, { it.name.lowercase() }))
  }

  private fun BluetoothDevice.toHost() =
    Host(
      name = name?.takeIf { it.isNotBlank() } ?: address,
      address = address,
      isPhone = bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.PHONE,
    )

  private fun hasConnectPermission() =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
      ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

  private val proxyListener =
    object : BluetoothProfile.ServiceListener {
      override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
        if (profile != BluetoothProfile.HID_DEVICE) return
        Log.i(TAG, "HID Device proxy connected")
        hid = proxy as BluetoothHidDevice
        if (uiVisible) register() else releaseIfIdle()
      }

      override fun onServiceDisconnected(profile: Int) {
        if (profile != BluetoothProfile.HID_DEVICE) return
        Log.i(TAG, "HID Device proxy disconnected")
        dropProxy()
        if (uiVisible) refresh()
      }
    }

  private val hidCallback =
    object : BluetoothHidDevice.Callback() {
      override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        Log.i(TAG, "onAppStatusChanged registered=$registered plugged=${pluggedDevice?.address}")
        this@HidRemote.registered = registered
        if (registered) onRegistered(pluggedDevice)
        else if (hid != null && uiVisible) _state.value = RemoteState.Busy
      }

      override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
        Log.i(TAG, "onConnectionStateChanged ${device.address} state=$state")
        when (state) {
          BluetoothProfile.STATE_CONNECTING ->
            _state.value = RemoteState.Connecting(device.toHost())
          BluetoothProfile.STATE_CONNECTED -> onConnected(device)
          BluetoothProfile.STATE_DISCONNECTED -> onDisconnected(device)
        }
      }

      override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
        // Some hosts poll the current state once after connecting; nothing is pressed.
        val size = if (id.toInt() == HidDescriptor.CONSUMER_REPORT_ID) 2 else 8
        if (hasConnectPermission()) hid?.replyReport(device, type, id, ByteArray(size))
      }
    }

  private fun onDisconnected(device: BluetoothDevice) {
    val current = _state.value
    val host =
      (current as? RemoteState.Connected)?.host ?: (current as? RemoteState.Connecting)?.host
    if (host?.address != device.address) return
    connectTimeout?.cancel()
    _state.value = RemoteState.Ready
    if (current is RemoteState.Connecting) {
      if (!quietConnect) _messages.tryEmit(R.string.error_connect)
    }
    // A phone that bonded before the watch offered a keyboard accepts the link, then drops it at once.
    else if (SystemClock.elapsedRealtime() - connectedAtMs < QUICK_DROP_MS) {
      Log.w(TAG, "${device.address} dropped the link right away; it needs a fresh pairing")
      _messages.tryEmit(R.string.error_rejected)
    }
    releaseIfIdle()
  }

  private companion object {
    const val TAG = "HidRemote"
    const val BIND_TIMEOUT_MS = 8_000L
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val QUICK_DROP_MS = 2_000L
    const val BOND_CONNECT_DELAY_MS = 2_000L

    // Shorter taps can reach the phone ~3 ms apart over Bluetooth, and camera apps ignore them.
    const val KEY_HOLD_MS = 150L
  }
}
