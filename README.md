# FitnessTracker

A privacy-first, offline-capable Android fitness app built with Kotlin and Jetpack Compose.
All data stays on your device in a local Room database - no account, no cloud, no ads.

## Features

- **Workout planner & logger**: weekly routine per day, set/rep/weight logging with previous-session prefill, personal best detection, rest timer with haptic + notification alerts, progress charts
- **GPS activity tracking**: running, walking, and cycling with live distance, pace, and calorie tracking via a foreground service (fused location provider, GPS-jump filtering)
- **Nutrition tracking**: food database with search, meal logging (breakfast/lunch/snack/dinner), saved meals, natural-language food entry, water and weight logging, macro targets computed from your profile (Mifflin-St Jeor BMR)
- **AI Nutrition Coach**: chat-based coach powered by Google Gemini (bring your own API key, stored encrypted on-device) with a fully offline fallback mode
- **Data export**: share your workouts, nutrition, and cardio logs as CSV from the Profile screen
- **Metric / Imperial** unit support

## Project layout

```
FitnessTracker/
  app/src/main/java/com/example/fitnesstracker/
    data/       Room database, entities, DAOs
    service/    Foreground GPS tracking service
    ui/         ViewModels and Compose screens
    util/       Unit conversion, CSV export, encrypted storage
```

## Building

1. Open the `FitnessTracker/` directory in Android Studio (or run `./gradlew assembleDebug` inside it).
2. Min SDK 24, target SDK 36, JDK 17.

## Optional: AI Coach setup

The AI Coach works offline out of the box. For dynamic AI responses, paste a
[Google AI Studio](https://aistudio.google.com/) API key into **AI Coach > Settings**.
The key is stored in `EncryptedSharedPreferences` on-device and is never written to the database or backed up.

## Privacy

- All logs are stored locally (Room/SQLite).
- `allowBackup` is disabled so app data is not uploaded to device cloud backups.
- The only network calls are to the Gemini API, and only if you configure a key.
