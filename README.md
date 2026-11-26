# Pump Tracker

Native Android app to log daily push-ups, pull-ups and squats, with a home-screen widget.

- **Today**: one-tap `+N` buttons per exercise, a custom amount, undo, and a log of the day's entries.
- **Stats**: per-exercise bar chart over the last 7 days, 30 days or 12 months (tap a bar for its value), plus daily average, current streak, best day and all-time total.
- **Widget** (4×3): today's totals with quick-add buttons; tapping a total opens the app.

Kotlin, Jetpack Compose, Room, Glance. Data stays on the device.

## Build

Requires JDK 17 and the Android SDK (`sdk.dir` in `local.properties`, or `ANDROID_HOME`).

```sh
./gradlew installDebug        # build and install on a connected device
./gradlew testDebugUnitTest   # unit tests
```

The quick-add amounts are defined per exercise in `app/src/main/java/com/lafarguem/pumptracker/data/Exercise.kt`.
