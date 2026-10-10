# Third-party notices

Application dependencies include AndroidX AppCompat, Activity, Fragment, RecyclerView, CameraX, Room and ExifInterface; Material Components for Android; ZXing Core; and their transitive dependencies. AndroidX, Material Components and ZXing are distributed under Apache License 2.0. JUnit 4 is a development/test dependency under Eclipse Public License 1.0. Gradle is distributed under Apache License 2.0; the Android SDK is governed by Google's SDK terms. This ZIP does not redistribute the SDK.

Consult the actual dependency artefacts and upstream repositories for full licence/NOTICE terms before distribution. Dependency versions are pinned in `app/build.gradle`; `./gradlew app:dependencies` prints the complete transitive graph.

- AndroidX: https://android.googlesource.com/platform/frameworks/support/
- Material: https://github.com/material-components/material-components-android
- ZXing: https://github.com/zxing/zxing
- Gradle: https://github.com/gradle/gradle
- Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0
