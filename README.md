# Jenga Barcode Scanner — George's first native Android app

A real Android application written in Java, using CameraX, ZXing, native XML layouts and Room. It runs on Android 8.0 and later. It is not a website or a WebView.

## Start here

1. Extract this ZIP on your computer.
2. Install [Android Studio](https://developer.android.com/studio) and complete its setup wizard.
3. Choose **Open**, select the `JengaBarcodeScanner` folder containing `settings.gradle`, and wait for Gradle sync.
4. If asked, install Android SDK Platform 36 and Build Tools 35.0.0 from **Tools > SDK Manager**. Use JDK 17 for Gradle; Android Studio's compatible bundled JDK is suitable.
5. Enable Developer options and USB debugging on your phone, connect it by USB and approve the debugging prompt.
6. Select the phone in Android Studio and click the green **Run** triangle.
7. Allow camera access. Point at a barcode. Tap **Save to history** if you want to keep it. Tap **Done**, then **Resume** for another scan.

You do not need a Play developer account to run the app on your own phone. See `PLAY_STORE_GUIDE.md` when you are ready to distribute it.

## Features

- **Scanner:** single, continuous and QR-only modes; torch, camera switching, tap-to-focus, zoom, pause/resume, visual scan frame and image imports.
- **Results:** format/value/time, copy/share/search, explicit website/email/dialler/contact actions and Wi-Fi details. Dangerous or unsupported URI schemes are not opened.
- **Generate:** QR, Code 128, EAN-13/8, UPC-A/E, Code 39/93, ITF and Codabar. Validated input, quiet zones, adjustable PNG dimensions, export and sharing.
- **History:** local Room storage, search/filter/sort, notes, individual deletion, confirmation before clearing, CSV and JSON exports through Android's system document picker.
- **Settings:** sound, vibration, default camera/mode, duplicate cooldown and dark/light/system themes.

The scan overlay is a guide: decoding uses the whole camera frame to avoid preview/crop alignment mistakes. Continuous mode saves accepted scans automatically. Duplicate cooldown is per format and value; a code can be saved again after the selected time. Opening the last result pauses continuous scanning. Image import returns one detected code; it is not a multiple-code extractor. Wi-Fi information is displayed for manual connection, with no automatic network changes.

## Build from a terminal

On macOS/Linux:

```bash
chmod +x gradlew
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
./gradlew bundleRelease
```

On Windows use `gradlew.bat` instead of `./gradlew`.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release AAB: `app/build/outputs/bundle/release/app-release.aab`.
The command-line release AAB is unsigned. Use Android Studio **Generate Signed App Bundle / APK** with your own upload key for Play distribution. Never upload a debug APK as a production release.

Internet access is needed on the computer for the first dependency download. Camera decoding and generation run offline on the phone; the app has no INTERNET permission.

## Project map

`MainActivity` hosts the four native fragments. `scanner/CameraController` handles lifecycle-bound CameraX; `BarcodeDecoder` handles luma strides, rotation and ZXing; `ImageScanner` down-samples and handles EXIF; `ResultDialog` offers confirmed actions. `generator/BarcodeGenerator` owns encoding. `database/` contains Room schema and DAO. `utils/Tasks` runs database, export and image work off the main thread. SharedPreferences stores small settings.

Pinned build tools: Android Gradle Plugin 8.11.1, Gradle 8.13, JDK 17, compile/target SDK 36. Dependencies are pinned in `app/build.gradle`; older stable compatible versions are used deliberately rather than moving automatically to newer APIs.

## Privacy

Camera frames and selected images stay on-device. Saved values and notes remain inside local app storage. Android backup is disabled. Export and share destinations are chosen by the user and can retain copies outside the app. No account, adverts, analytics, tracking or product-information service is included. Publish `PRIVACY_POLICY.html` before a store submission.

## Before publishing

Read `BUILD_VERIFICATION.md` for what was actually checked. Run the physical-phone checklist in `TESTING.md`. Camera performance and behaviour vary by device; a successful build alone is not a real camera test. Create your own signing key, fill in your support contact, capture genuine screenshots and review Play Console declarations. The package name becomes permanent once registered on Play.

See `THIRD_PARTY_NOTICES.md` for dependency licences. Source code created for this project is provided for you to use, modify and publish; third-party dependency terms still apply.

## Supplied demo APK

The `builds/JengaBarcodeScanner-demo.apk` file is ready for a first phone test. Tap it on your Android phone, approve installation from your chosen browser/file manager if requested, then allow Camera in the app. Use **Scan again** after a result, or **Resume** after closing it.

The debug signing certificate is for development only. Moving from this demo to a differently signed Play build may require uninstalling the demo first; export any history you want to keep before doing that.
