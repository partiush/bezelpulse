# BezelPulse

A lightweight, open-source Android app that adds smooth, iOS-style bezel press animations whenever you click your phone's hardware buttons (Volume Up, Volume Down, Power, or any extra custom key).

BezelPulse renders directly onto a pre-attached, hardware-accelerated `Canvas` synced to your display's VSYNC (`Choreographer`), so animations respond on the very next frame at 60/90/120Hz and consume 0% CPU when idle.

---

## Features

- **Native-feel spring physics** — Real damped harmonic oscillator (`k` stiffness, `ζ` damping ratio, sustained hold swell, and minimum quick-tap hold) with a live response curve graph.
- **4 bezel curve styles** — *iOS Liquid* (smooth cubic Bézier bezel flex), *Tactile Pill*, *Laser Arc*, and *Ripple Wave*.
- **Full visual customization** — Adjust depth, shoulder spread, crown flatness, fill color/opacity, multi-pass GPU rim glow, optional micro-glyphs (`+`, `−`, `⏻`, etc.), and synchronized haptics.
- **Custom hardware buttons** — Add any extra physical button (Camera shutter, AI/Assist key, Action/Mute button, gaming shoulder triggers, headset key) using quick presets or live hardware `KeyEvent` capture.
- **Visual edge calibration** — Drag edge handles directly beside your phone's physical buttons with ruler guides and 1% fine-tune controls, or start from built-in device layouts (Pixel, Samsung, Xiaomi, OnePlus/Nothing, iPhone).
- **Quick Settings tile & landscape auto-pause** — Toggle animations from your notification shade and automatically pause during fullscreen landscape video or games.
- **Shareable configs** — Save local presets or copy/import full JSON configurations.
- **Always-on background reliability** — Optional silent keep-alive foreground service and boot receiver so animations keep working after clearing recent apps or rebooting.

---

## Download & Install

You do not need Android Studio to install BezelPulse.

### From GitHub Releases (easiest)
1. Open the **Releases** section of this repository on your Android phone.
2. Download the latest `BezelPulse-*.apk` file and tap it to install.

### From GitHub Actions (latest commit)
1. Open the **Actions** tab and click the latest **Build BezelPulse APK** workflow run.
2. Under **Artifacts**, download **BezelPulse-APK**, unzip it, and install the `.apk` file.

---

## Setup Guide

1. **Test right away** — Open BezelPulse and press your physical Volume keys (or press-and-hold the test pads on the Studio tab) to see the live bezel animation along your screen edge.
2. **Align to your phone's buttons** — Tap **Align Edges** and drag each handle until it lines up with your physical buttons, then tap **Done**.
3. **Enable system-wide animations** — Tap **Enable Service**, find **BezelPulse** in Android Accessibility Settings, and turn it on.

### Keeping BezelPulse Running in the Background

Stock Android (Pixel, Motorola, Nothing) usually keeps Accessibility and foreground services running with minimal setup. Some OEM Android skins aggressively stop background apps when you clear Recent Apps:

| Device / Skin | Recommended Settings |
| --- | --- |
| **Pixel / AOSP / Nothing** | Tap **Unrestrict Battery** → set BezelPulse to *Unrestricted*. |
| **Samsung (One UI)** | Battery → *Unrestricted*, and ensure BezelPulse is not in *Sleeping* or *Deep sleeping apps*. |
| **Xiaomi / Redmi / POCO (HyperOS / MIUI)** | Battery saver → *No restrictions*, enable **Auto-Start**, and optionally lock BezelPulse in Recents. |
| **OnePlus / OPPO / Realme (OxygenOS / ColorOS)** | Enable *Allow background activity* and *Allow auto-launch* in App Management. |
| **Vivo / iQOO / Huawei / Honor** | Enable *Autostart* / manual launch and disable battery optimization for BezelPulse. |

> **Note on the Power button:** Most Android ROMs handle short Power-off presses in the system framework before Accessibility services receive `KEYCODE_POWER`. BezelPulse handles this by listening for direct key events where supported and automatically triggering the Power animation on screen wake, screen lock, and long-press power menus.

---

## Privacy & Permissions

BezelPulse works completely offline, contains no ads or analytics, and never requests internet access.

| Permission | Why it is used |
| --- | --- |
| **Accessibility Service** (`flagRequestFilterKeyEvents`, `canRetrieveWindowContent="false"`) | Receives hardware key presses and draws a non-touchable `TYPE_ACCESSIBILITY_OVERLAY` on the screen edge. Screen content is **never** read and touches pass straight through. |
| `VIBRATE` | Optional haptic tick synchronized with button clicks. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the background listener alive when recent apps are cleared (can be toggled off in-app). |
| `RECEIVE_BOOT_COMPLETED` | Restores the background engine automatically after reboot (can be toggled off in-app). |
| `POST_NOTIFICATIONS` | Displays the low-priority silent notification required by Android for the foreground keep-alive service on Android 13+. |

---

## For Developers

### Tech Stack
- **Kotlin** + **Jetpack Compose** (Material 3)
- **Android AccessibilityService** (`TYPE_ACCESSIBILITY_OVERLAY`, hardware-accelerated `View` + `Choreographer`)
- **Jetpack DataStore** (Preferences)
- **TileService** (Quick Settings integration)
- **JUnit 4** + **Robolectric**

### Project Layout
```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Edge-to-edge host, in-app key listener, adaptive nav
├── data/
│   ├── BezelConfigCodec.kt          # JSON serializer for buttons, presets & config export/import
│   └── BezelPreferencesRepository.kt# DataStore persistence
├── engine/
│   ├── BezelGeometry.kt             # Shared C2-smooth cubic Bézier path builder
│   └── BezelPhysicsEngine.kt        # Sub-stepped spring solver, analyzer & runtime event bus
├── model/
│   └── BezelModels.kt               # Domain models, hardware key catalog & presets
├── service/
│   ├── BezelAccessibilityService.kt # System-wide key filter, power fallbacks & overlay host
│   ├── BezelKeepAliveService.kt     # START_STICKY foreground service with task revival
│   ├── BezelBootReceiver.kt         # Boot / update auto-start receiver
│   ├── BezelQuickTileService.kt     # Quick Settings tile toggle
│   └── BezelOverlayView.kt          # Zero-allocation VSYNC Canvas renderer
├── ui/
│   ├── components/                  # Calibration overlay, spring graph, color picker, dialogs
│   ├── screens/                     # Studio, Buttons, Style, Physics screens
│   └── theme/                       # Color tokens, bundled typography, M3 theme
├── util/
│   └── OemSupport.kt                # Cross-OEM battery & auto-start intent resolver
└── viewmodel/
    └── BezelViewModel.kt            # UI state & DataStore coordination
```

### Building Locally

**Requirements:** JDK 21, Android SDK (API 36), Gradle 9.3.1 (or Android Studio).

```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug
```

The built APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Signed Release Builds in GitHub Actions

To have CI also generate a signed `BezelPulse-release.apk`, configure these repository secrets under **Settings → Secrets and variables → Actions**:
- `KEYSTORE_BASE64` — Base64-encoded upload keystore (`base64 -w 0 my-upload-key.jks`)
- `STORE_PASSWORD` — Keystore password
- `KEY_PASSWORD` — Key password (alias: `upload`)

To publish a GitHub Release automatically, push a tag matching `v*` (e.g. `git tag v1.0.0 && git push origin v1.0.0`) or trigger **Build BezelPulse APK** manually with **Also publish APK as a GitHub Release** checked.

---

## Contributing

Contributions, device layout presets, OEM compatibility fixes, and new animation styles are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.

## License

Released under the [MIT License](LICENSE).
