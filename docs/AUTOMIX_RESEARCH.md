# AutoMix research (independent of Apple)

Status (2026-11-21): WIRED as an opt-in audible path. "AutoMix (beta)" lives in Settings > Song Transitions (default off) and only
acts while Crossfade is on. When both songs' tempos are cached and close enough, the Crossfader times the overlap to whole bars and
runs the incoming player nudged (max 6%) to the playing song's tempo, easing back to the user's speed over ~1.5 s after the swap.
Fallback on any unknown is the plain crossfade. Still NOT heard on a device: the maths and lifecycle are unit-tested, the sound is
not. QA, ranked risks and the phone test script: docs/MIXING_QA.md. This is NOT Apple's AutoMix and makes no parity claim.
The Apple page (support.apple.com/en-in/105067) says AutoMix works only with Apple Music subscription content, so this app cannot offer it. This note covers an independent local-file approximation.

How it is wired: PlaybackService analyses the playing song, then the next queued one, into TempoCache (background, one min-priority
thread, skipped under battery saver). The Crossfader only reads the cache at fade time (plannedTransition), so analysis latency can
never stall playback. The nudge lives only on the incoming player; every exit path (swap, cancel, error) settles at the user's speed.

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
- AutoMix is shipped as an opt-in beta on top of Crossfade per the user's 2026-11-21 go-ahead ("Do it" on the mixing status).
- Still pending: the user's ears. Ranked risks and the phone test script are in docs/MIXING_QA.md; feedback decides whether the
  beta stays, is tuned (downbeat alignment, ease curve) or is reverted to plain Crossfade.

## Planner (experimental, unused)
`analysis/AutoMixPlanner.kt` maps two tempo estimates to a plan: both known and within 8% (also half/double) means a whole-bar fade inside the user's 1-12 s limit and an incoming speed nudge of at most 6%; anything else falls back to a plain crossfade. Unit-tested on made-up tempos only. Nothing in playback or the UI calls it. Before it can be wired to an opt-in setting a phone test must show: the tempo decode runs on real files without stalling playback, the speed nudge sounds acceptable, and the overlap lands on the beat.
Crossfade hardening: the fade is abandoned if the incoming player errors, is not ready within 4 s, or if the outgoing song ends before the incoming one is playing.

## Tempo prefetch (experimental, default off)
`TempoPrefetcher` decodes the next queue item and stores its tempo estimate in `TempoCache`. It runs only when the hidden preference
`tempo/prefetch_on` is true (default false, no UI), and it is skipped while the system battery saver is on. Cancel never caches an
"unknown" result, the decoder is always released, and cancellation is checked inside the decode loop. Nothing audible comes from this:
it only fills the cache the unused planner would read.

## Beat-aligned overlaps (2026-11-25)

New `analysis/MixPlanner.kt` (pure maths, unit-tested on synthetic grids): when both songs have
high-confidence beat grids and tempos match (half/double allowed, existing 6% nudge cap), the transition
overlap is a whole number of bars (8/4/2/1, largest inside the user's 1-12 s cap), starts on an outgoing
beat, ends exactly at the detected content end, requires the two beat trains to overlay within 45 ms across
the whole overlap, and RMS-trims the incoming level (+/-6 dB). Anything less falls back to today's
crossfade. Wired through `TransitionAnalysis` (background worker, cached) into the Crossfader behind the
same AutoMix opt-in; all cancel/fallback paths unchanged. Scope boundary honoured: the user authorised
silent-tail removal only, so no audible intro/outro content is skipped and the incoming song starts at 0.
Sources labelling: Apple documents (support.apple.com/en-us/105067, Apple Newsroom 2025-06-09) establish
time-stretching, beat matching, silence removal and per-pair style choice for THEIR subscription engine;
whole-bar overlays, beat-phase alignment and level matching here are standard published auto-DJ practice
(see github.com/saksham-45/automix, 10.1186/s13636-018-0134-8), not Apple documentation. Audible quality
and grid reliability on real songs remain phone-test hypotheses (docs/MIXING_QA.md).
