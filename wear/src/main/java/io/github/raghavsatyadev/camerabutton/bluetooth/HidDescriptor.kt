package io.github.raghavsatyadev.camerabutton.bluetooth

/**
 * HID report descriptor for the remote: a consumer-control report and a boot-style keyboard.
 *
 * Report 1 carries one 16-bit consumer usage (volume up is the shutter in most camera apps).
 * Report 2 is a standard 8-byte keyboard report (modifiers, reserved, six key codes).
 */
internal object HidDescriptor {
  const val CONSUMER_REPORT_ID = 1
  const val KEYBOARD_REPORT_ID = 2

  val BYTES: ByteArray =
    intArrayOf(
        // Consumer control, report 1: a single 16-bit usage, array style.
        0x05, 0x0C, // Usage Page (Consumer)
        0x09, 0x01, // Usage (Consumer Control)
        0xA1, 0x01, // Collection (Application)
        0x85, CONSUMER_REPORT_ID, // Report ID
        0x15, 0x00, // Logical Minimum (0)
        0x26, 0xFF, 0x03, // Logical Maximum (1023)
        0x19, 0x00, // Usage Minimum (0)
        0x2A, 0xFF, 0x03, // Usage Maximum (1023)
        0x75, 0x10, // Report Size (16)
        0x95, 0x01, // Report Count (1)
        0x81, 0x00, // Input (Data, Array, Absolute)
        0xC0, // End Collection
        // Keyboard, report 2.
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x06, // Usage (Keyboard)
        0xA1, 0x01, // Collection (Application)
        0x85, KEYBOARD_REPORT_ID, // Report ID
        0x05, 0x07, // Usage Page (Keyboard)
        0x19, 0xE0, // Usage Minimum (Left Control)
        0x29, 0xE7, // Usage Maximum (Right GUI)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x01, // Logical Maximum (1)
        0x75, 0x01, // Report Size (1)
        0x95, 0x08, // Report Count (8)
        0x81, 0x02, // Input (Data, Variable, Absolute): modifiers
        0x95, 0x01, // Report Count (1)
        0x75, 0x08, // Report Size (8)
        0x81, 0x01, // Input (Constant): reserved byte
        0x95, 0x06, // Report Count (6)
        0x75, 0x08, // Report Size (8)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x65, // Logical Maximum (101)
        0x05, 0x07, // Usage Page (Keyboard)
        0x19, 0x00, // Usage Minimum (0)
        0x29, 0x65, // Usage Maximum (101)
        0x81, 0x00, // Input (Data, Array): key codes
        0xC0, // End Collection
      )
      .map { it.toByte() }
      .toByteArray()
}
