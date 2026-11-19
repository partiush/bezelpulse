# Contributing to BezelPulse

Thanks for taking an interest in improving BezelPulse. Bug reports, device-specific OEM fixes, new bezel curve styles, and performance improvements are all welcome.

## Reporting Issues

When filing a bug, please include:
- Your phone model, Android version, and OEM skin (e.g., Pixel 9 / Android 15, Galaxy S24 / One UI 7, Xiaomi 14 / HyperOS 2).
- Whether Accessibility Service and Battery Unrestricted are enabled.
- Clear steps to reproduce.

## Development Workflow

1. Fork the repository and create a feature branch (`git checkout -b fix/my-change`).
2. Keep changes focused and modular. Avoid allocations inside `BezelOverlayView.onDraw` or `Choreographer.FrameCallback.doFrame` — the render loop must stay allocation-free for 120Hz displays.
3. Run the unit test suite before opening a PR:
   ```bash
   gradle :app:testDebugUnitTest
   gradle :app:assembleDebug
   ```
4. Open a pull request describing what changed and why.

## Code Guidelines

- Prefer Kotlin idioms and Jetpack Compose M3 components.
- Never request invasive permissions (internet, storage, or `canRetrieveWindowContent="true"`).
- Always return `false` from `AccessibilityService.onKeyEvent` so native hardware key handling is never delayed or blocked.
