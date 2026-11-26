# Music App

Android music player built from a Figma design file (not committed here).

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

## From a folder to the end of playback

1. Complete language and permission setup. Reader setup is optional for local playback.
2. Use the folder dial on Home to rescan Android's indexed audio. Open Folders and select a folder to browse its tracks. This flow uses the device media library rather than copying an Apple Music catalog or importing an arbitrary cloud folder.
3. Select a song. Use the bottom mini-player to return to the full circular-dial player. Inspect the queue to see which song will play next.
4. Under Settings → Song Transitions, enable AutoMix for automatic transitions. It uses two playback decks, local beat estimates and a bounded incoming tempo adjustment. The planner now tries shorter whole-bar windows when the longest candidate fails alignment checks. Uncertain pairs use the existing fallback. The Mixing label includes deck preparation.
5. Pause to end listening immediately. For a timed ending, open the player menu and set Sleep Time. On expiry the app pauses playback and requests task closure after the exit countdown. Leaving the player screen during normal playback is a separate action.

### What the Apple Music comparison tells us

Apple documents importing local files or folders on Mac, adding songs to the queue and saving the queue on quit. Its AutoMix documentation limits the feature to eligible Apple Music catalog playback on supported devices. This Android app applies local analysis to device audio. Apple does not publish the complete transition algorithm. [Import music](https://support.apple.com/guide/music/import-items-already-on-your-computer-mus3081/mac) · [Queue and quit](https://support.apple.com/guide/music/musb1e6d1c76/mac) · [Song transitions](https://support.apple.com/guide/music/muse5e9ec085/mac)

### Remaining differences and verification

| Goal | Android implementation and remaining work |
| --- | --- |
| Continuous beat through the handoff | Beat-grid matching, downbeat preference when confident, level trim and shorter candidate windows. Run a listening comparison with the same song pair to verify the audible result. |
| Last lyric into the next first lyric | The planner still starts the incoming track at zero. Reliable word timestamps, vocal entrance/exit cues and shortening audible instrumental passages remain open. |
| Independent vocal and drum fades | Both decks currently mix full tracks. The reference engines illustrate stem separation and vocal ducking; these require additional processing and device-performance validation. |
| Resume after quitting | Apple documents queue restoration on Mac. Android cold-start restoration of the exact queue and playback position needs a dedicated end-to-end test before claiming parity. |

Reference implementations: [word-level handoffs and vocal ducking](https://github.com/allenbijo/auto-dj) and [cue selection and separate vocal, drum and bass tracks](https://github.com/VertexSelection/automix-engine).
