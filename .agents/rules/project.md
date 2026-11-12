---
trigger: always_on
description: CameraButton-specific overrides and facts for the shared agent-workflow skills and rules. Wins over them.
---

# Project overrides — CameraButton

Shared skills/rules are links into agent-workflow (`scripts/link-project.sh`); this file is this
repo's own.

- **Wear OS app**, one module `:wear`. Device tests run on the paired watch (find it with
  `adb devices -l`), not the phone; the phone is only the bridge for watch ADB.
- **Theme**: Wear Compose Material 3, not phone M3E; use `material3-expressive` only where the Wear
  M3 API has the same component.
- **Preview example**: `wear/src/main/java/io/github/raghavsatyadev/camerabutton/presentation/remote/ShutterButtonPreviews.kt ShutterButtonIdlePreview`.
- **Format**: no ktfmt (`FORMAT_CHECK=0`).
