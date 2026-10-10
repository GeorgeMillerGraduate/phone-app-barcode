# Build verification — 10 October 2026

The delivered source was compiled with JDK 17, Gradle 8.13, Android Gradle Plugin 8.11.1 and Android SDK 36.

- `assembleDebug`: passed; installable debug-signed APK produced.
- `testDebugUnitTest`: 9 tests passed, 0 failures, 0 skipped. Includes rotation, duplicate suppression, all generator formats, Data Matrix/PDF417/Aztec decoding, QR-only exclusion, validation and export escaping.
- `lintDebug`: passed with 0 errors and 25 warnings. Non-blocking warnings concern localisation of Java UI text, compatibility-ignored accessibility attributes, newer dependency versions, adaptive icon guidance and RecyclerView refresh efficiency; see the included report.
- `bundleRelease`: passed, including R8 optimisation and release vital lint. The supplied release AAB is **unsigned**; create your own upload key in Android Studio before a Play upload.
- APK signing and alignment: checked separately with Android SDK tools.
- Manifest: minimum SDK 26, target SDK 36; no Internet, location, contacts, call, audio or broad storage permissions.
- All XML resource files parse; all Java-referenced view IDs exist.

Reports are in `build-reports/`. The app was not launched in an emulator or tested on a physical phone in this environment. Live-camera autofocus, exposure, speed, OEM behaviour, accessibility and portrait/landscape layouts still need the real-device checks in `TESTING.md`. A successful build and algorithm tests do not establish those device behaviours.

No Google Play account, upload key or production release was created. The privacy/support pages are supplied files and have not been published to your website.
