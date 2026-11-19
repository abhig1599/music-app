# AutoMix research (independent of Apple, not built yet)

Status: research and scope only. Nothing here is implemented or heard on a device. This is NOT Apple's AutoMix and makes no parity claim.
The Apple page (support.apple.com/en-in/105067) says AutoMix works only with Apple Music subscription content, so this app cannot offer it. This note covers an independent local-file approximation.

## Scope of a first prototype (analysis only, no audio change)
1. Decode a local file to mono PCM with MediaExtractor + MediaCodec (platform APIs, no dependency).
2. Compute an onset-strength envelope (spectral flux on short frames) and estimate tempo with autocorrelation of that envelope (range 70-180 BPM, with half/double-tempo handling).
3. Store per-song results locally: bpm, bpm confidence, analysed-at, file id + size/modified time as cache key. Nothing is uploaded.
4. Show "BPM" only when confidence is above a threshold. Otherwise show "not detected". No made-up values.
5. Verify on synthetic click tracks at known tempos in unit tests (the only CI-possible check), then on real songs by the user.

## Later steps (need device testing)
- Pick transition points near the end/start of songs, and nudge the next song's playback speed a small amount toward the current tempo (limited to a few percent) during the existing crossfade.
- Musical key: harder and less reliable. Leave out until BPM works.

## Dependency check
- TarsosDSP has onset/beat code but is GPL v3 (https://github.com/JorenSix/TarsosDSP/blob/master/LICENSE). Linking it would put the app under GPL. Do not add it. Write the small analysis code in Kotlin instead.

## Boundary
- Current batch: plain Crossfade only (Off by default).
- AutoMix prototype: separate batch, after the user has heard Crossfade on a phone.
