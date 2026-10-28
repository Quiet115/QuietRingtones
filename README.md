# QuietRingtones

Change your Android ringtone, notification sound and alarm sound in one tap, import your own audio, and switch between saved sets of sounds called profiles.

> Spanish documentation for the design decisions lives in [`docs/`](docs/) and [`GLOSSARY.md`](GLOSSARY.md).

## What it does

- Shows the sound that is active right now for calls, notifications and alarms, with a waveform "fingerprint" for each sound.
- Lets you play any available sound and apply it as the system default.
- Imports your own audio files (mp3, m4a, ogg, wav) as a ringtone, notification or alarm sound.
- Saves your three current sounds as a **profile** and applies a profile in one tap.
- Keeps an **Original state** profile (read-only) with the sounds you had before using the app.
- Themes: Obsidian (default, dark), Felt (light) and a custom palette with your own accent and background.
- Languages: English (default) and Spanish.

Everything stays on your device. There is no account, no server and no analytics.

## Platform limits

- **Android:** the app needs the special *Modify system settings* permission, which you grant yourself in Android settings. It can set the **default** ringtone, notification and alarm sounds. It cannot change the sound that each *other* app uses for its own notification channels.
- **iPhone:** iOS does not allow any third-party app to change the system ringtone or notification sounds, so this app cannot be ported as-is. A future iOS companion could only export `.m4r` tones and guide you through installing them.

## Build

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 34.0.0).

1. Create `local.properties` with the path to your SDK (this file is not committed):

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

2. Build and test:

   ```bash
   ./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
   ```

   On Windows use `gradlew.bat`. The debug APK is written to `app/build/outputs/apk/debug/`.

## Project layout

- `core/` — pure Kotlin domain logic (profiles, original state, fallback rules) behind two ports, `SoundSystem` and `Store`. It has no Android dependencies and is tested on the JVM.
- `app/` — Android app (Kotlin, Jetpack Compose): the Android adapter for `SoundSystem` (RingtoneManager, Settings.System, MediaStore), local storage, theming and screens.
- `docs/` — specification and architecture decision records.

## Support the project

If you like it, you can [buy me a buñuelo](https://ko-fi.com/quiet115).

## License

[MIT](LICENSE).
