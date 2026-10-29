# QuietRingtones

Change your Android ringtone, notification sound and alarm sound in one tap, import your own audio, and switch between saved sets of sounds called profiles.

> Spanish documentation for the design decisions lives in [`docs/`](docs/) and [`GLOSSARY.md`](GLOSSARY.md).

## What it does

- Shows the sound that is active right now for calls, notifications and alarms, with a waveform "fingerprint" for each sound.
- Lets you play any available sound and apply it as the system default.
- Imports your own audio files (mp3, m4a, ogg, wav) as a ringtone, notification or alarm sound.
- Saves your three current sounds as a **profile** and applies a profile in one tap.
- Keeps an **Original state** profile (read-only) with the sounds you had before using the app.
- Themes: Obsidian (default), Midnight, Sunset, Neon, Paper, Sky, Wallpaper (Android 12+, uses your wallpaper colors) and up to 12 palettes of your own.
- Languages: English (default) and Spanish.
- Volume bar and warnings: each tab shows the volume of that sound type and warns when you would not hear it (volume off or low, silent or vibrate, Do Not Disturb).
- Online catalog: classical clips from the [QuietRingtones-catalog](https://github.com/Quiet115/QuietRingtones-catalog) repository. Play one to hear a preview (it downloads to a cache you can clear in Settings), and tap Use to keep it. Your own imported audio stays at the top of each list.
- Trim your own audio: when you import a file you choose where it starts and how long it lasts (a few seconds or the whole file).
- Search: filter any list and the online catalog by name.
- Sound per contact (off by default): give a contact their own ringtone. Turning it on explains what it needs; contacts are read from your phone when shown and never kept or sent.
- Share a profile with another phone: *Share* on any profile creates a `.qrprofile` file that carries the audio of its sounds; on the other phone, open the file (tap it in WhatsApp, Files, Drive or Gmail and choose QuietRingtones, or use *Import a profile*) and confirm. Its sounds and the profile are added (it never overwrites one you already have). The other phone needs QuietRingtones installed. You choose how to send the file; the app sends nothing itself.
- Other sounds: turn touch sounds, dial-pad tones and lock sounds on or off, with shortcuts to the keyboard and sound settings of the phone. Android only allows switching these on or off, not replacing them.
- Updates: checks GitHub Releases when you open the app (on by default, can be turned off, or tap *Check for updates*). New versions download over Wi-Fi and you confirm the install. Older versions stay available on the [releases page](https://github.com/Quiet115/QuietRingtones/releases).

Everything stays on your device. There is no account, no server and no analytics. The only network use is checking for and downloading updates, and the online catalog, both from GitHub. No data about you is sent.

## Platform limits

- **Android:** the app needs the special *Modify system settings* permission, which you grant yourself in Android settings. It can set the **default** ringtone, notification and alarm sounds. It cannot change the sound that each *other* app uses for its own notification channels, nor replace system sounds such as the keyboard, dial-pad, lock or charging sound (the files are read-only and, on Samsung, the charging sound follows the phone's sound theme).
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

## Releasing

Releases are signed APKs attached to GitHub releases. Every release must be signed with the same key, or installed copies cannot update.

1. Put the signing details in `keystore.properties` (not committed): `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Back up the keystore file; if it is lost, nobody can update.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, commit and push.
3. Run `./scripts/release.ps1 -Version X.Y.Z -Notes "What changed"`. It runs the tests, builds the release APK and creates the `vX.Y.Z` release on GitHub.

Debug builds use a different signature and cannot be updated from a release: uninstall the debug build first.

## Project layout

- `core/` — pure Kotlin domain logic (profiles, original state, fallback rules) behind two ports, `SoundSystem` and `Store`. It has no Android dependencies and is tested on the JVM.
- `app/` — Android app (Kotlin, Jetpack Compose): the Android adapter for `SoundSystem` (RingtoneManager, Settings.System, MediaStore), local storage, theming and screens.
- `docs/` — specification and architecture decision records.

## Support the project

If you like it, you can [buy me a kebab](https://ko-fi.com/quiet115).

## License

[MIT](LICENSE).
