# Your first phone app: from Java project to Google Play

Prepared 10 October 2026. The official policy pages below were checked when this project was created; check them again at submission because requirements change. Store acceptance cannot be guaranteed by a successful build.

## 1. Install and open Android Studio

Download from https://developer.android.com/studio on Windows, macOS or Linux. Run the installer and setup wizard, accepting the Android SDK components. Extract the project ZIP to a normal writable folder; do not try to open the ZIP itself as a project. Choose **Open** and select `JengaBarcodeScanner`.

Gradle sync downloads the pinned plugin and dependencies from Google Maven and Maven Central. Allow it to finish. In **Tools > SDK Manager**, install **Android 16 / API 36**, **Android SDK Build-Tools 35.0.0** and **Android SDK Platform-Tools**. Configure Gradle to use JDK 17 in Settings > Build, Execution, Deployment > Build Tools > Gradle. Android Studio can create `local.properties` with your own SDK location; the supplied archive omits the builder's machine-specific path.

If sync fails, verify computer internet connectivity, SDK licences and JDK selection. Do not replace dependency versions randomly. The supplied wrapper uses Gradle 8.13; Android Gradle Plugin 8.11 supports API 36 and requires this Gradle version and JDK 17:
https://developer.android.com/build/releases/agp-8-11-0-release-notes

## 2. Run on your own phone

In Android Settings > About phone, tap **Build number** seven times (location varies by manufacturer). Open Developer options and enable **USB debugging**. Connect a USB data cable, unlock the phone and accept the debugging fingerprint prompt. Windows may need the manufacturer's USB driver. Select the device in Android Studio's run-device selector and click **Run**.

Grant camera permission, scan a real retail barcode, save it and check History. Try Generate. You can do this without paying for Play Console or publishing anything. This is the best first milestone.

An emulator is useful for generation, exports, permissions and navigation. Use **Tools > Device Manager > Create device**, choose a phone and API 36 system image, then start it. A virtual camera does not prove that real-phone focusing and scanning work.

## 3. Build a debug APK

Use **Build > Build App Bundle(s) / APK(s) > Build APK(s)** (wording varies by Studio version) or run `gradlew.bat assembleDebug` on Windows, `./gradlew assembleDebug` on macOS/Linux. The file is `app/build/outputs/apk/debug/app-debug.apk`.

For a supplied debug APK downloaded on your phone, tap it in Downloads and approve Android's **Install unknown apps** permission for that browser/file manager if requested. This is a development build. Android Studio installation by USB avoids that manual installer route.

## 4. Create your upload key and signed AAB

Select **Build > Generate Signed App Bundle / APK > Android App Bundle**, choose the `app` module and **Create new** keystore. Save the keystore somewhere secure outside the project folder, choose a strong unique password, key alias and long validity period. Enter your real developer identity as appropriate. Keep at least two secure backups of the keystore, alias and passwords, using an encrypted backup/password manager. Do not put these in Git, this ZIP, public storage or screenshots.

Choose the **release** variant and finish. Studio signs the bundle with your key. The release task supplied in this project does not embed signing credentials; a plain `bundleRelease` produces an unsigned AAB for verification only. A Play upload must be signed with your upload key.

Use **Play App Signing** during the first upload. Google manages the distribution signing key while you use an upload key to submit future releases. Understand the difference and record the certificate fingerprints. Official instructions:
https://developer.android.com/studio/publish/app-signing

## 5. Register in Play Console

Go to https://play.google.com/console/signup and choose the correct personal or organisation account type. Complete the current registration fee, identity/contact checks and any device-verification steps shown by Google. Read what developer identity/contact details will be public before completing the account. Use your own account; no Play account is created by this project.

## 6. Create the app listing

Choose **Create app**. Use **Jenga Barcode Scanner**, English (United Kingdom), app rather than game, and free unless you deliberately choose otherwise. Select the appropriate category, such as Tools. Confirm the package `com.jengacode.barcodescanner` is available and that you are happy to retain it for future updates. Change it consistently in `app/build.gradle` before the first upload if required.

Use `STORE_LISTING.md` for editable text. Set a real support email/contact you monitor and website. Capture genuine screenshots on a phone/emulator; do not submit conceptual artwork as if it were the running app. Prepare a 512 × 512 Play icon and 1024 × 500 feature graphic using the specification in that file and check current asset requirements in Console.

## 7. Privacy policy

Review `PRIVACY_POLICY.html`. Add your working support email or support URL before publication. Upload it to your own website, for example `https://jenga-code.com/barcode-scanner-privacy.html`, and check it is publicly accessible on a phone without a login. Put the exact published URL in Play Console. The supplied file has not been uploaded to your website.

Ensure the policy continues to match the released app. Adding adverts, analytics, cloud sync or product lookups later would require new disclosures and a revised policy.

## 8. Complete App Content and Data Safety

This version has no Internet permission, accounts, ads, analytics or remote collection. All camera decoding and Room history storage are local. The app asks for Camera and Vibration only. User-selected document access needs no broad photo/storage permissions. Camera access is part of the visible core scanning feature.

Review every Data Safety question against the actual final bundle and all dependencies. On-device processing by itself is not remote collection; the current version's intended declaration is **no data collected or shared by the developer**. User-initiated browser/search/share/export actions send selected content to the chosen external destination; explain this accurately, and check Google's applicable definitions/exemptions rather than mechanically copying answers. Do not claim encryption for a server service that does not exist. There is no app account to delete. Users can clear local history or Android app storage.

Complete the ads declaration (no ads in version 1), content-rating questionnaire, target-audience declaration, app-access declaration (no login), permissions and any other cards Console shows. Choose the actual intended audience rather than claiming this is a children's app. This app is not a financial/health product or a product-price database.

Data Safety guidance: https://support.google.com/googleplay/android-developer/answer/10787469

## 9. Internal testing first

In **Test and release > Testing > Internal testing**, create a release, enable Play App Signing as requested and upload your signed release AAB. Add release notes from `STORE_LISTING.md`. Resolve all errors and inspect warnings. Add tester emails/list, share the opt-in link and install from Google Play. Test the *release build*, including scanner, Room storage and export. Check the pre-launch report. Internal testing is a useful private demonstration but does not replace an applicable closed test.

## 10. Closed testing and production access

For personal developer accounts created after 13 November 2023, Google's checked policy requires **at least 12 testers opted in continuously for at least 14 days** in a closed test before applying for production access. Ask real testers to use the app and record feedback; satisfying the count/time alone does not guarantee approval. Internal-test installs do not meet the closed-test requirement.

Complete app setup, create a closed-testing track, add testers, publish the test and give them the opt-in link. Keep them opted in for the required continuous period, fix reported problems and retain notes about what was tested. Then apply for production access and answer Google's questions honestly about testers, feedback and readiness.

Official current instructions: https://support.google.com/googleplay/android-developer/answer/14151465

## 11. Target API and production release

The checked policy states that new Android phone apps and updates must target **Android 16 / API 36 or higher** from 31 August 2026. This project targets API 36. The minimum supported version remains API 26; target SDK does not mean older phones are excluded.

Official policy: https://support.google.com/googleplay/android-developer/answer/11926878

Once production access is granted and all setup cards are complete, create a production release using your tested signed AAB, choose countries/availability, review the listing and declarations, then submit for review. Fix any requested changes. Keep the first production rollout controlled so you can respond to crashes/feedback. This project does not submit anything to Google on your behalf.

## 12. Updates

Retain the package and signing arrangement. Increase `versionCode` for every new upload and update `versionName`, release notes and any changed privacy disclosures. Run `testDebugUnitTest`, `lintDebug`, a release build and real-device tests. If changing the database schema, increase its version and write a Room migration to preserve history; do not use destructive migration for a released app. Send updates through internal/closed testing before production.
