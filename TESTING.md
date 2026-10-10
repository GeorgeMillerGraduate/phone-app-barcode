# Testing checklist

Read `BUILD_VERIFICATION.md` for completed checks. The list below is for you and your testers; unchecked items are not claimed to have passed.

## Automated checks

Run `./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease` (Windows: `gradlew.bat`). Unit tests verify rectangular-frame rotation, duplicate cooldown, every generation format's encode/decode round trip, invalid checksum/dimension rejection, URL/Wi-Fi classification and CSV/JSON escaping. These do not test a live camera.

## Physical phone / emulator

- [ ] Fresh install on API 26 and a recent Android version; launch/splash/navigation visible.
- [ ] Allow camera permission: preview starts and retail EAN-13 decodes.
- [ ] Deny permission: useful message, image import/generation remain usable.
- [ ] Deny permanently: Settings action opens Android app settings; grant then return.
- [ ] EAN-13 `5901234123457`, EAN-8 `96385074`, UPC-A `036000291452`, UPC-E `01234565` decode exactly.
- [ ] QR `https://jenga-code.com/` displays the link; nothing opens until confirmed.
- [ ] Code 128 `JENGA-001`, Code 39/93 `JENGA-001`, ITF `12345678`, Codabar `A123456B` decode (Codabar scanner may strip start/end letters).
- [ ] Data Matrix, PDF417 and Aztec sample images decode. These are scanning formats, not offered in the generator UI.
- [ ] Continuous mode: same code is suppressed during cooldown; two different codes are saved; code can be saved after cooldown expires.
- [ ] QR-only mode ignores a retail barcode.
- [ ] Torch toggles only on cameras with flash. Switching cameras updates torch support.
- [ ] Tap-to-focus and zoom work. No-camera/no-torch devices have usable recovery/disabled controls.
- [ ] Pause/Resume; tap Done on result then Resume; latest result stays accessible.
- [ ] Background/return, navigate away/back, rotate portrait/landscape; no stuck camera or leaked preview.
- [ ] Rapid detections create one dialog in single mode and no duplicate database inserts within cooldown in continuous mode.
- [ ] Save scan then restart app: history persists. Saved result actions and notes work.
- [ ] Search value/note, filter format and change sorting.
- [ ] Delete one record; cancel/confirm clearing history. Only confirmed deletion removes records.
- [ ] Export CSV/JSON to a chosen folder; inspect values/notes; cancel destination picker gracefully.
- [ ] CSV cells starting `=`, `+`, `-` or `@` are prefixed with an apostrophe to neutralise spreadsheet formulas. JSON preserves the exact original values.
- [ ] Generate every format with valid/invalid inputs; PNG scans in another barcode reader.
- [ ] Share PNG and text through a chosen app; URI permission works in the receiving app.
- [ ] Import PNG/JPEG, rotated EXIF JPEG, very large image, invalid image and unreadable barcode. Cancel picker gracefully.
- [ ] Wi-Fi/contact/email/telephone QR results show appropriate explicit actions. No network, call or contact action is automatic.
- [ ] Airplane mode: scan/generate/history/export all work; external search requires the chosen browser to have connectivity.
- [ ] Sound/vibration off; default mode/camera/theme/cooldown persist after restart.
- [ ] Font size enlarged, TalkBack, light/dark modes, small phone and landscape: controls readable and reachable.
- [ ] Release APK from the signed release bundle passes the same tests; inspect Play pre-launch report.

## Sample images

The `test-barcodes/` folder contains generated QR, EAN-13, Code 128, Data Matrix, PDF417 and Aztec samples. Display these on a second screen or print at sensible size without cropping the white quiet zone. Decode them through **Scan an image** on the same phone if needed. Never use a generated example as proof that real-camera focus/exposure works.

## Troubleshooting

Camera black: check permission, close other camera apps, return to Scanner and tap Retry. Scanning difficult: improve lighting, move back so the entire code fits and tap to focus. Barcode invalid: follow the selected format's numeric/character and checksum constraints; QR accepts ordinary text. History export failure: choose another destination and check free space. Report issues with Android version, phone model, selected mode and steps; avoid sending sensitive QR payloads.
