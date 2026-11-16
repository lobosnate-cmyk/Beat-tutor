# Beat Tutor

A tapping rhythm game built from any song you load. It listens to the track, finds its beats and notes, and drops them down four lanes for you to tap. Learn mode stops the music at every note until you hit it.

## Get the app (Android)

Every push to `main` builds a fresh APK on GitHub.

1. On your phone, open this repo's **Releases** page and download `BeatTutor.apk` from the newest release.
   Direct link: `https://github.com/lobosnate-cmyk/Beat-tutor/releases/latest/download/BeatTutor.apk`
2. Open the file. Android asks you to allow installs from your browser or file manager the first time. Allow it, then tap **Install**.
3. New builds install over the old one, so your settings and best scores stay.

Needs Android 8.0 or newer. The app has no internet permission: songs are analysed on the phone and never leave it.

## How it works

| Song type | What you get |
|---|---|
| MP3, WAV, M4A, OGG, FLAC | Taps detected from the recording: every drum hit, strum and note onset, plus a best guess at each note's pitch. |
| MIDI (`.mid`) | The song's exact notes, with note names. Pick which part to learn. |

- **Difficulty** thins the taps out: Easy follows the beat, Hard plays every note.
- **Play** scores your timing (Perfect, Great, Good, Miss) with combos.
- **Learn** pauses the song at each note until you tap the right lane.
- **Speed** runs at 50%, 75% or 100%.
- **Timing offset** lines the notes up with what you hear. The results screen can set it for you.

## Project layout

| Path | What it is |
|---|---|
| `web/game.html` | The whole game: analysis, charts, gameplay. Edit this. |
| `tools/build_web.py` | Wraps the game into `app/src/main/assets/index.html` for the app. |
| `app/` | Small Android shell (full-screen WebView and the song picker). |
| `.github/workflows/build-apk.yml` | Builds and releases the APK. |
| `keystore/debug.keystore` | Sideload signing key, kept here so updates install over each other. Not for a Play Store release. |

## Build it yourself

With Android Studio installed, open the folder and press Run. From a terminal with the Android SDK and Gradle 8.9:

```
python3 tools/build_web.py
gradle assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.
