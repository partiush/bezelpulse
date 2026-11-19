# BezelPulse

Zero-latency, iOS-style bezel press animations for Android hardware Volume and Power buttons with real-time spring physics and full customization.

## Download APK via GitHub Actions

This repository includes an automated GitHub Actions workflow (`.github/workflows/build-apk.yml`) that builds an installable Android APK on every push to `main`/`master` or on demand.

### Option 1: Download from the Actions Tab (Automatic on every push)
1. Push this project to your GitHub repository.
2. Open the **Actions** tab in GitHub and select the latest **Build BezelPulse APK** run.
3. Scroll to **Artifacts** at the bottom of the run summary and click **BezelPulse-APK** to download it.
4. Unzip the file and open `BezelPulse-debug.apk` on your Android phone to install.

### Option 2: Direct `.apk` Download via GitHub Releases (No Unzipping)
1. Go to **Actions → Build BezelPulse APK → Run workflow**.
2. Check **Also publish APK as a GitHub Release** (optionally enter a tag such as `v1.0.0`) and click **Run workflow**.
   *(Alternatively, push a git tag like `git tag v1.0.0 && git push origin v1.0.0`.)*
3. Open your repository's **Releases** page on your Android phone and tap `BezelPulse-debug.apk` to download and install directly.

### Optional: Signed Release APKs
If you want the workflow to also build `BezelPulse-release.apk`, add these three repository secrets under **Settings → Secrets and variables → Actions**:
- `KEYSTORE_BASE64`: Base64-encoded `.jks` upload keystore (`base64 -w 0 my-upload-key.jks`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password (alias: `upload`)
