# QuietRingtones

Change your Android ringtone, notification sound and alarm sound in one tap, import your own audio, and switch between saved sets of sounds called profiles.

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
- Backup: *Back up all profiles* saves every profile of yours, with its audio, in one `.qrprofile` file (each audio only once) that you can keep or open on another phone. Importing a backup never overwrites a profile you already have.
- Automatic changes: schedule a profile for a time and the days you choose (for example a silent one at night). The change is made by the phone's alarm clock service at the set time, also after a restart; it needs no network and sends nothing. Deep sleep can delay it by a few minutes.
- Other sounds: turn touch sounds, dial-pad tones and lock sounds on or off, with shortcuts to the keyboard and sound settings of the phone. Android only allows switching these on or off, not replacing them. Some phones also block individual switches: on a Samsung with One UI 9 (Android 17) the lock sound cannot be changed by apps, and the app says so instead of changing it.
- Updates: checks GitHub Releases when you open the app (on by default, can be turned off, or tap *Check for updates*). New versions download over Wi-Fi and you confirm the install. Older versions stay available on the [releases page](https://github.com/Quiet115/QuietRingtones/releases).

Everything stays on your device. There is no account, no server and no analytics. The only network use is checking for and downloading updates, and the online catalog, both from GitHub. No data about you is sent.

## What you can count on

- **No ads, no credits, no purchases, no accounts.** The only money link is the optional *Buy me a kebab* button, which opens a web page and unlocks nothing.
- **No forced updates.** The app never stops working until you update. The update check can be turned off, and you can skip a version.
- **Works without internet.** Only the online catalog and the update check need a connection. Profiles, your own audio, trimming, schedules, sound per contact and the volume warnings all work offline.
- **Your sounds stay yours.** Audio you import, and sounds you add from the catalog with *Use*, are saved in the phone's own Ringtones, Notifications and Alarms folders, so they stay on the phone if you uninstall the app. Your profiles, schedules and settings live inside the app and are removed with it, so use *Back up all profiles* first if you want to keep them.
- **Screen reader support.** Buttons, switches and sliders are labelled for TalkBack, including which sound each *Play* and *Use* button belongs to.

## Platform limits

- **Android:** the app needs the special *Modify system settings* permission, which you grant yourself in Android settings. It can set the **default** ringtone, notification and alarm sounds. It cannot change the sound that each *other* app uses for its own notification channels, nor replace system sounds such as the keyboard, dial-pad, lock or charging sound (the files are read-only and, on Samsung, the charging sound follows the phone's sound theme).
- **iPhone:** iOS does not allow any third-party app to change the system ringtone or notification sounds, so this app cannot be ported as-is. A future iOS companion could only export `.m4r` tones and guide you through installing them.

## Install

1. Open the [latest release](https://github.com/Quiet115/QuietRingtones/releases/latest) and download the `.apk` file.
2. Open it on your phone and allow the install when Android asks.
3. After that the app checks for new versions by itself (you can turn that off in Settings).

Every older version stays on the [releases page](https://github.com/Quiet115/QuietRingtones/releases).

## Report a problem or suggest something

Use the *Feedback* tab inside the app (Settings), or open an [issue](https://github.com/Quiet115/QuietRingtones/issues/new) here. The app only adds the app version, Android version, phone model and language, and you can edit everything before sending. Please do not post personal data.

## Terms of use

QuietRingtones is free to use. Copyright (c) 2026 Quiet115. All rights reserved. The source code is not public.

- You may install and use the app for free and share the link to this page.
- You may not sell it, redistribute modified or repackaged copies, or reverse engineer it, except where the law allows it.
- The app is provided "as is", without warranty of any kind.
- It uses open-source components, each under its own license.
