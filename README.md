# BezelPulse

An open-source Android app that adds smooth, iOS-style bezel press animations for your phone's physical buttons — Volume Up, Volume Down, Power, and custom hardware keys.

BezelPulse draws directly onto a hardware-accelerated `Canvas` synced to your display's VSYNC (`Choreographer`), delivering responsive 60/90/120Hz edge animations with 0% idle CPU usage.

<p align="left">
  <a href="https://github.com/partiush/bezelpulse/releases/latest">
    <img src="https://img.shields.io/badge/Download_from_GitHub-Latest_Release-0EA5E9?style=for-the-badge&logo=android&logoColor=white" alt="Download from GitHub" />
  </a>
  <a href="https://github.com/partiush/bezelpulse/releases/latest/download/BezelPulse.apk">
    <img src="https://img.shields.io/badge/Direct_Download-BezelPulse.apk-10B981?style=for-the-badge&logo=github&logoColor=white" alt="Direct APK Download" />
  </a>
  <a href="https://github.com/partiush/bezelpulse/issues/new/choose">
    <img src="https://img.shields.io/badge/Report_a_Bug-GitHub_Issues-F59E0B?style=for-the-badge&logo=github&logoColor=white" alt="Report a Bug" />
  </a>
</p>

> **Version:** `v1.0.0` · **Android 7.0+ (API 24+)** · **100% Offline · Open Source (MIT)**

---

## Download & Setup

1. Tap **Download from GitHub** (or **Direct Download**) above to download the latest `BezelPulse.apk`.
2. Open the `.apk` file on your Android device to install it.
3. Launch **BezelPulse**, tap **Align Edges** to line up the handles with your phone's physical buttons, and tap **Enable Service** to activate animations across all apps.

---

## Features

- **Spring physics engine** — Real damped harmonic oscillator (`k` stiffness, `ζ` damping ratio, sustained press-and-hold swell, and minimum quick-tap duration) with a live response curve graph.
- **4 bezel styles** — *iOS Liquid* (smooth cubic Bézier bezel flex), *Tactile Pill*, *Laser Arc*, and *Ripple Wave*.
- **Deep customization** — Tune depth, shoulder spread, peak flatness, fill color & opacity, multi-pass GPU rim glow, optional micro-glyphs (`+`, `−`, `⏻`), and synchronized haptics.
- **Custom hardware buttons** — Add extra physical keys (Camera shutter, AI/Assist key, Action/Mute button, gaming shoulder triggers, headset buttons) via presets or live key capture.
- **Visual edge calibration** — Drag handles directly beside your physical buttons with 1% fine-tune controls, or start from built-in device layouts.
- **Quick Settings tile & landscape pause** — Toggle animations from your notification shade and automatically pause during fullscreen landscape videos and games.
- **Background reliability** — Optional silent keep-alive foreground service and boot auto-start to stay active after clearing recent apps or rebooting.
- **Shareable JSON configs** — Save local presets or export/import full configurations as JSON.

---

## Background Setup by Android Skin

Stock Android devices generally keep Accessibility services active automatically. On Android skins with aggressive background limits, adjusting battery settings helps keep BezelPulse running reliably:

| Device / Android Skin | Recommended Settings |
| --- | --- |
| **Pixel / AOSP / Nothing / Motorola** | Tap **Battery Settings** in BezelPulse → *Unrestricted*. |
| **Samsung (One UI)** | Battery → *Unrestricted*, and exclude BezelPulse from *Sleeping* / *Deep sleeping apps*. |
| **Xiaomi / Redmi / POCO (HyperOS / MIUI)** | Battery saver → *No restrictions*, enable **Auto-Start**, and optionally lock BezelPulse in Recents. |
| **OnePlus / OPPO / Realme (OxygenOS / ColorOS)** | Enable *Allow background activity* and *Allow auto-launch* in App Management. |
| **Vivo / iQOO / Huawei / Honor** | Enable *Autostart* / manual launch and disable battery optimization for BezelPulse. |

---

## Feedback & Bug Reports

`v1.0.0` is the initial public release. Because Android hardware button handling, screen shapes, and OEM skins vary widely across devices, you may still encounter bugs, visual glitches, or device-specific quirks.

If something does not work as expected on your device, please **[open an issue](https://github.com/partiush/bezelpulse/issues/new/choose)** with your device model, Android version, and steps to reproduce so it can be fixed in future releases.

---

## Privacy & Permissions

BezelPulse runs completely offline with no ads, analytics, or internet permission.

| Permission | Purpose |
| --- | --- |
| **Accessibility Service** (`flagRequestFilterKeyEvents`, `canRetrieveWindowContent="false"`) | Listens for hardware key presses and draws a non-touchable `TYPE_ACCESSIBILITY_OVERLAY` along the screen edge. Screen content is never read and normal button/touch input is never blocked. |
| `VIBRATE` | Optional tactile haptic click synchronized with button presses. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Optional silent keep-alive service to prevent Android from unloading the overlay when recent apps are cleared. |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start after device reboot. |
| `POST_NOTIFICATIONS` | Required on Android 13+ for the low-priority silent notification when the keep-alive service is enabled. |

---

## For Developers

### Tech Stack
- **Kotlin** + **Jetpack Compose** (Material 3, adaptive NavigationBar / NavigationRail)
- **AccessibilityService** (`TYPE_ACCESSIBILITY_OVERLAY`, hardware-accelerated `View` + `Choreographer`)
- **Jetpack DataStore** (Preferences)
- **TileService** (Quick Settings integration)
- **JUnit 4** + **Robolectric**

### Project Structure
```text
app/src/main/java/com/example/
├── MainActivity.kt                   # Edge-to-edge host, in-app key listener, adaptive nav
├── data/
│   ├── BezelConfigCodec.kt           # JSON serializer for buttons, presets & config import/export
│   └── BezelPreferencesRepository.kt # DataStore persistence
├── engine/
│   ├── BezelGeometry.kt              # Shared C2-smooth cubic Bézier path builder
│   └── BezelPhysicsEngine.kt         # Sub-stepped spring solver, analyzer & runtime event bus
├── model/
│   └── BezelModels.kt                # Domain models, hardware key catalog & presets
├── service/
│   ├── BezelAccessibilityService.kt  # System-wide key listener & overlay host
│   ├── BezelKeepAliveService.kt      # START_STICKY foreground service with task revival
│   ├── BezelBootReceiver.kt          # Boot & package-update auto-start receiver
│   ├── BezelQuickTileService.kt      # Quick Settings tile toggle
│   └── BezelOverlayView.kt           # Zero-allocation VSYNC Canvas renderer
├── ui/
│   ├── components/                   # Edge calibration HUD, spring graph, color picker, dialogs
│   ├── screens/                      # Studio, Buttons, Style, Physics screens
│   └── theme/                        # Color tokens, bundled typography, M3 theme
├── util/
│   └── OemSupport.kt                 # Cross-OEM battery & auto-start intent resolver
└── viewmodel/
    └── BezelViewModel.kt             # UI state & DataStore coordination
```

### Building Locally

**Requirements:** JDK 21, Android SDK (API 36), Gradle 9.3.1 (or Android Studio).

```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug

# Build release APK
gradle :app:assembleRelease
```

### Contributing

Pull requests, bug reports, device layout presets, OEM compatibility improvements, and new animation styles are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.

### License

Released under the [MIT License](LICENSE).
