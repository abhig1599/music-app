# Music App

Android music player built from Abhishek's Figma file
(https://www.figma.com/design/qZQCBqzvSKC9gJ46TzEkz2/Music-App).

- **Platform:** Android first (Kotlin + Jetpack Compose, minSdk 26, targetSdk 34)
- **Core:** local on-device audio playback (Media3/ExoPlayer + media session +
  mediaPlayback foreground service). No streaming service, no ads.
- **Design system:** exact Figma color styles - Black `#1E1E1E`, Bg `#EAEAE8`,
  Bg 2 `#F0F0F0`, White `#FFFFFF`, Orangs `#FF6603`, Gray-light `#D9D9D9`.
  Signature circular tick-dial progress.

## Build

CI builds the debug APK on every push (`.github/workflows/android.yml`), artifact:
`app-debug-apk`. Local build needs JDK 17 + Android SDK 34:

```
./gradlew :app:assembleDebug
```

## Docs

- `RESEARCH.md` - platform/source research with citations and what is NOT verified
- `DECISIONS.md` - decision ledger (what is decided vs pending, and why)
- `RECOVERY.md` - how to pick this project up after a reset
