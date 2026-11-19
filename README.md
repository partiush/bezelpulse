# BezelPulse

Smooth, iOS-style bezel press animations for your Android phone's physical buttons — Volume Up, Volume Down, Power, and any extra custom hardware key.

BezelPulse renders directly on a hardware-accelerated `Canvas` synced to your display's VSYNC (`Choreographer`), so edge animations respond on the very next frame at 60/90/120Hz and use 0% CPU when idle.

<p align="left">
  <a href="../../releases/latest">
    <img src="https://img.shields.io/badge/Download_from_GitHub-Latest_Release_APK-0EA5E9?style=for-the-badge&logo=android&logoColor=white" alt="Download from GitHub" />
  </a>
  <a href="../../releases/latest/download/BezelPulse.apk">
    <img src="https://img.shields.io/badge/Direct_APK-One--Tap_Download-10B981?style=for-the-badge&logo=github&logoColor=white" alt="Direct APK Download" />
  </a>
  <a href="../../issues/new/choose">
    <img src="https://img.shields.io/badge/Report_a_Bug-GitHub_Issues-F59E0B?style=for-the-badge&logo=github&logoColor=white" alt="Report a Bug" />
  </a>
</p>

> **Current Version:** `v1.0.0` (Android 7.0+ / API 24–36) · Completely offline · No ads, no tracking, no internet permission.

---

## Install & Update

### First-Time Install
1. Tap the **Download from GitHub** button above (or **Direct APK** to grab `BezelPulse.apk` right away).
2. Open the downloaded `.apk` on your Android device and allow installing from your browser/files app if prompted.
3. Open **BezelPulse**, align the edge handles with your physical buttons using **Align Edges**, and tap **Enable Service** to turn on system-wide animations.

### Seamless Updates (Keep All Your Settings)
When a new version of BezelPulse is released (`v1.0.1`, `v1.1.0`, etc.):
- Download the latest release APK and tap it to **Update** directly over your existing installation.
- **Do not uninstall first.** Android updates BezelPulse in place and preserves all of your saved button positions, custom hardware keys, physics curves, and saved presets.

---

## What It Does

- **Native-feel spring physics** — Real damped harmonic oscillator (`k` stiffness, `ζ` damping ratio, sustained press-and-hold swell, and quick-tap minimum hold) with a live spring curve graph.
- **4 bezel curve styles** — *iOS Liquid* (smooth cubic Bézier bezel flex), *Tactile Pill*, *Laser Arc*, and *Ripple Wave*.
- **Full visual customization** — Tune depth, shoulder spread, peak flatness, fill color & opacity, multi-pass GPU rim glow, optional micro-glyphs (`+`, `−`, `⏻`, etc.), and synchronized haptics.
- **Custom hardware buttons** — Add extra physical keys (Camera shutter, AI/Assist key, Action/Mute button, gaming shoulder triggers, headset buttons) via presets or live hardware key capture.
- **Visual edge calibration** — Drag handles directly beside your physical buttons with 1% fine-tune nudges, or start from built-in device layouts.
- **Quick Settings tile & landscape pause** — Toggle animations from your notification shade and automatically pause during fullscreen landscape videos and games.
- **Always-on background reliability** — Optional silent keep-alive foreground service and boot auto-start so animations stay active after clearing recent apps or rebooting.
- **JSON config sharing** — Export or import your complete setup to back up or share styles with others.

---

## Setup Tips for Different Android Phones

Stock Android (Pixel, Motorola, Nothing) usually keeps Accessibility and background services active with minimal setup. Some Android skins aggressively stop background apps when you clear Recent Apps:

| Device / Android Skin | Recommended Settings |
| --- | --- |
| **Pixel / AOSP / Nothing / Motorola** | Tap **Battery Settings** in BezelPulse → set to *Unrestricted*. |
| **Samsung (One UI)** | Battery → *Unrestricted*, and make sure BezelPulse is not in *Sleeping* or *Deep sleeping apps*. |
| **Xiaomi / Redmi / POCO (HyperOS / MIUI)** | Battery saver → *No restrictions*, enable **Auto-Start**, and optionally lock BezelPulse in Recents. |
| **OnePlus / OPPO / Realme (OxygenOS / ColorOS)** | Enable *Allow background activity* and *Allow auto-launch* in App Management. |
| **Vivo / iQOO / Huawei / Honor** | Enable *Autostart* / manual launch and disable battery optimization for BezelPulse. |

### Note on the Power Button
- **Direct press & hold:** On devices that forward `KEYCODE_POWER` to Accessibility services, short clicks and continuous holds animate natively.
- **Long-press Power menu:** On devices where Android intercepts short Power clicks inside the system framework before apps see them, holding the Power button to open the Power menu triggers a sustained hold animation that stays expanded while the menu is open.
- **No false triggers on screen taps:** Screen wake/lock fallback is **off by default**, so tapping the screen to wake or unlocking your phone will not falsely trigger the Power animation. If you prefer an animation whenever the screen wakes or sleeps, you can turn on **Power Wake / Lock Fallback** in the Studio tab.

---

## Early Release Notice & Bug Reports

`v1.0.0` is the first public release of BezelPulse. Android hardware button handling, display cutouts, refresh rates, and background policies vary significantly across manufacturers and Android versions (Android 7 through Android 16/17), so **bugs, animation glitches, or device-specific quirks may still occur**.

If you run into any issue — an animation glitch, misaligned edge, Power button quirk, or background kill on your phone — please **[open a bug report](../../issues/new/choose)** with:
- Your phone model and Android version
- What happened and how to reproduce it
- Any relevant settings (style, custom buttons, or toggles)

Every reported issue helps improve BezelPulse and will be addressed in upcoming releases.

---

## Privacy & Permissions

BezelPulse works **100% offline**, contains no analytics or ads, and never requests internet or storage permissions.

| Permission | Purpose |
| --- | --- |
| **Accessibility Service** (`flagRequestFilterKeyEvents`, `canRetrieveWindowContent="false"`) | Receives hardware key events and renders a non-touchable `TYPE_ACCESSIBILITY_OVERLAY` along the screen edge. Screen content is **never** read and normal button/touch behavior is never blocked. |
| `VIBRATE` | Optional tactile haptic click synchronized with button presses. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Optional silent keep-alive service so Android does not unload the overlay when recent apps are cleared. |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start after device reboot. |
| `POST_NOTIFICATIONS` | Required by Android 13+ to display the low-priority silent notification while the keep-alive service is enabled. |

---

## For Developers

### Tech Stack
- **Kotlin** + **Jetpack Compose** (Material 3, adaptive NavigationBar / NavigationRail)
- **AccessibilityService** (`TYPE_ACCESSIBILITY_OVERLAY`, hardware-accelerated custom `View` + `Choreographer` VSYNC loop)
- **Jetpack DataStore** (Preferences, schema-versioned for safe upgrades)
- **TileService** (Quick Settings integration)
- **JUnit 4** + **Robolectric**

### Project Structure
```text
app/src/main/java/com/example/
├── MainActivity.kt                   # Edge-to-edge host, in-app key listener, adaptive nav
├── data/
│   ├── BezelConfigCodec.kt           # JSON codec for buttons, presets & config export/import
│   └── BezelPreferencesRepository.kt # Schema-versioned DataStore persistence
├── engine/
│   ├── BezelGeometry.kt              # Shared C2-smooth cubic Bézier path builder
│   └── BezelPhysicsEngine.kt         # Sub-stepped spring solver, analyzer & runtime event bus
├── model/
│   └── BezelModels.kt                # Domain models, hardware key catalog & presets
├── service/
│   ├── BezelAccessibilityService.kt  # System-wide key filter, Power hold hooks & overlay host
│   ├── BezelKeepAliveService.kt      # START_STICKY foreground service with task revival
│   ├── BezelBootReceiver.kt          # Direct-boot-aware reboot & package-update receiver
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
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug

# Build signed release APK (requires keystore env vars)
VERSION_NAME=1.0.0 VERSION_CODE=10000 \
STORE_PASSWORD=... KEY_PASSWORD=... \
gradle :app:assembleRelease
```

### Publishing a New Release

BezelPulse uses semantic tags (`vMAJOR.MINOR.PATCH`) to automatically compute a strictly increasing `versionCode` (`MAJOR * 10000 + MINOR * 100 + PATCH`), run the full test suite, build a signed **Release APK**, and publish it to **GitHub Releases**:

```bash
git tag v1.0.0
git push origin v1.0.0
```

You can also publish from the GitHub UI by running the **Release BezelPulse APK** workflow with your desired version tag (e.g. `v1.0.0`, `v1.0.1`, `v1.1.0`).

#### Keeping Updates Seamless (Release Signing)
Android only allows installing a newer APK over an existing app (preserving user data) if both APKs share the **same signing certificate** and a **higher `versionCode`**.

- **Recommended for public distribution:** Generate a private upload keystore once (`keytool -genkeypair -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`) and store it in **Repository Settings → Secrets and variables → Actions** BEFORE publishing `v1.0.0`:
  - `KEYSTORE_BASE64` — `base64 -w 0 my-upload-key.jks`
  - `STORE_PASSWORD` — Keystore password
  - `KEY_PASSWORD` — Key password (alias `upload`)
- **Zero-config fallback:** If those secrets are not configured, the workflow signs releases with the repository's deterministic keystore so subsequent releases still update cleanly on top of each other. *(Note: if you switch from the fallback key to a private keystore later, existing users will need to uninstall once before installing the newly signed release.)*

---

## Contributing

Bug reports, pull requests, device layout presets, OEM compatibility improvements, and new bezel styles are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.

## License

Released under the [MIT License](LICENSE).
