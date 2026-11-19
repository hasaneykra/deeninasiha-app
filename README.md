# দ্বীনি নসিহা — Android App

Native Android shell (WebView) for https://deeninasiha.com.
Every post, page and tool published on the site appears in the app automatically — nothing is duplicated.

## Features
- Native bottom navigation: Home, Tools, Prayer times, Search, Share
- Pull-to-refresh, thin progress bar, splash screen, adaptive icon
- Offline: previously visited pages open from cache; friendly Bengali offline page with retry
- Location permission for prayer times / qibla; compass sensors work inside the pages
- Links to deeninasiha.com open in-app, other links open in the system browser
- Opens deeninasiha.com links shared from other apps
- HTTPS only, no analytics, no trackers

## Build the APK without installing anything (GitHub)
1. Create a GitHub repository and upload this folder's contents.
2. Open the **Actions** tab → **Build Android APK** → **Run workflow**.
3. Download `DeeniNasiha-apk` from the finished run. Install `app-release.apk` on your phone.
Without secrets the APK is signed with a debug key (fine for personal install/testing).

## Release signing (needed for Play Store updates)
```
keytool -genkeypair -v -keystore release.jks -alias deeni -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks
```
Add repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Keep the keystore safe; losing it means you cannot update the app.

## Build locally
Open in Android Studio (Koala or newer) and run, or: `gradle assembleRelease` (JDK 17 + Android SDK 35).

## Customising
- Site address: `HOST` in `MainActivity.java` and the intent filter in `AndroidManifest.xml`
- Version: `versionCode` / `versionName` in `app/build.gradle.kts`
- Colours / icon: `res/values/colors.xml`, `res/drawable/ic_logo_foreground.xml`
