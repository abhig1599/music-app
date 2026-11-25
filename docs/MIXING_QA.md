# Mixing QA: Crossfade + AutoMix (2026-11-21)

What changed today: AutoMix is now a real, opt-in audible path on top of Crossfade (Settings > Song
Transitions > "AutoMix (beta)", default off). When both songs have a detected tempo close enough to match,
the overlap is timed to whole bars and the incoming song runs nudged (max 6%) to the playing song's tempo,
then eases back to the user's speed over ~1.5 s after the swap. Anything unknown falls back to exactly the
plain crossfade. This is tempo-rate matching ONLY: no beat-phase or downbeat alignment, so the overlap is not
beat-matched, and a wrong tempo read can sound worse than the plain crossfade (see risk 3). Crossfade itself
is unchanged in behavior. The tempo cache key is mediaId + file size + modified time, so a replaced file that
keeps its MediaStore id is re-analysed. Conservative policy: when that metadata cannot be read (dead/odd
uri), results live in memory for the session only and are NEVER written to disk, so no stale tempo can
survive a file replacement.

## Verification done here (CI-class, no device)
- Unit tests: planner edges, transition planning (fallbacks, caps, 6% nudge bound), speed-ease maths,
  swap/cancel lifecycle with a nudged incoming player (Robolectric, real ExoPlayer objects, no audio).
- Compile + full unit suite + Roborazzi renders via GitHub Actions (see docs/ci-last-success.txt).

## Ranked remaining risks (what a phone test must judge, in order)
1. Session hand-over on a real phone: notification, lock-screen controls and the queue must keep showing the
   right song after MediaSession.setPlayer. Compile-checked only.
2. Beat-phase alignment (2026-11-25, MixPlanner): when both songs have high-confidence beat grids and a
   matching tempo (half/double allowed), the overlap is a whole number of bars (8/4/2/1, largest inside the
   user's 1-12 s cap) starting ON an outgoing beat, the incoming deck runs nudged to the outgoing tempo, the
   two beat trains must overlay within 45 ms across the whole overlap (else plain crossfade), and the
   incoming level is RMS-trimmed (+/-6 dB) to the outgoing tail. The overlap still ends exactly at the
   detected content end and the incoming song starts at 0: nothing audible is skipped (the user authorised
   silent-tail removal only). UNVERIFIED on real songs: grid reliability decides how often a plan exists at
   all; synthetic-grid unit tests cover only the maths. Tradeoff: when beat start and the user's cap
   conflict (no whole-bar overlap fits both), the pair falls back to the plain crossfade rather than
   breaking the cap or the content boundary. Tempo analysis itself is tested on synthetic click tracks
   only, never on real songs.
3. Tempo misdetection on real songs (live drums, rubato, intros): wrong BPM -> wrong nudge. Mitigations:
   confidence gate, half/double-tempo handling, 6% cap, plain-crossfade fallback. A bad read sounds like a
   plain crossfade with a slightly off-speed incoming song for a few seconds.
4. Equaliser and visualiser continuity across the swap (shared audio session; new TeeAudioProcessor).
5. The 1.5 s speed ease-back is a linear ramp; on very steady tracks a pitch-drift-free ramp is audible as a
   slight tempo settle. Deliberate, tunable.
6. Analysis cost: decode of up to 60 s per song on one min-priority thread, cached per file. Skipped under
   battery saver. Battery/thermal effect unmeasured.

## Edge cases handled in code
- Pause, seek, next/previous, queue edits, repeat-one, shuffle, sleep "finish song": cancel/skip, no fade.
- Incoming player errors or not ready in 4 s: fade abandoned, main keeps playing untouched.
- Fade capped at 1/3 of the playing song AND 1/3 of the incoming song; under 500 ms: skipped.
- Unknown incoming duration: planned fade kept (documented choice).
- Outgoing song ends before the incoming one is playing: playback carries on, no swap into silence.
- User changes playback speed mid-ramp: incoming follows (userSpeed x nudge). Mid-ease: the ease stops, the
  user's speed wins.
- Mute/unmute during a fade: applies to both players within one 40 ms tick.
- AutoMix on but Crossfade off: nothing audible happens (AutoMix only acts on top of Crossfade).
- AutoMix on but tempo missing for either song at fade time: plain crossfade, no blocking, no waiting.
- Short/late analysis on the first song after enabling: that one transition may be plain; later ones use cache.
- Cached "unknown" tempo is not re-decoded every song (cache stores the unknown result).
- Cancel/collapse of a fade never leaves the incoming player's nudged speed behind: the nudge only ever lives
  on the incoming player, and the ease-back settles at the user's speed on every exit path.

## Manual phone test script (for the user, ~15 min)
1. Settings > Song Transitions: Crossfade on, 6 s. Play an album; listen to two transitions: smooth overlap,
   no restart, no volume jump, notification shows the new song and next/previous work right after.
2. Turn AutoMix (beta) on. Pick two songs with a steady, similar beat: the overlap should land on the beat and
   the new song should not sound sped up or slowed after the transition settles.
3. Pair with very different tempos (e.g. ballad into dance): expect a plain crossfade, no weird tempo shift.
4. During a fade: pause -> both stop, resume -> normal. Seek, skip next, toggle repeat-one: fade aborts cleanly.
5. Speed test: set 1.25x, let a transition happen, confirm the speed is still 1.25x after the new song settles.
6. Battery saver on: transitions still work (analysis is skipped; plain crossfade).
7. First song after enabling AutoMix may crossfade plainly (tempo not analysed yet) - expected, not a bug.

Report back which step number misbehaves and what you heard; that maps to the ranked list above.

## 2026-11-23 update: boundary-aligned fades, coherent switches, "Mixing" indicator, emulator overlap evidence

Why: his 0.1.7 phone test heard "the old song cut, then the next", not an overlap. Code review found (a) the fade completed up to
~1.5 s BEFORE the old song's natural end (early release = audible chop), and (b) AutoMix could be ON while Crossfade was OFF, a
state that mixes nothing at all (his actual switch states are unconfirmed; the trap is removed either way).

What changed (Crossfader.kt, SettingsScreen.kt, PlayerScreen.kt, PlayerManager.kt, playback/PcmTap.kt):
- Incoming deck is prepared paused ahead of time; the ramp starts so it ENDS exactly at the natural track end (fadeWindow maths).
- Settings: AutoMix on forces Crossfade on; Crossfade off drops AutoMix; engine gate also honours the saved 0.1.7 state.
- Player shows gradient "Mixing." -> "Mixing.." -> "Mixing..." only while a transition is genuinely armed/fading.
- XFADE logcat diagnostics; DEBUG-ONLY per-deck PCM tap (PcmTap, pref-armed), present in the audio chain only in -PpcmTap=true builds (the smoke), never in candidates.

Verification here:
- Unit/Robolectric: fadeWindow timing maths, transitionsAllowed gate incl. the 0.1.7 state, incoming-deck queue/start mirror,
  settings switch coherence (UI click test), Mixing label on/off render tests, plus the pre-existing suite.
- Emulator transition smoke (.github/ci/transition-smoke.sh, manual workflow "Transition smoke"): two 24 s tones (440/880 Hz),
  crossfade 4 s. Passes only if: fade armed at the right wall-clock window, >8 logged gain ticks with BOTH decks >0.05,
  per-deck PCM taps both non-silent across the shared window, swap lands at the boundary, "Mixing" visible during the ramp,
  no FATAL. Artifacts include mix_reconstructed.wav (per-deck PCM x the gains the app logged - a reconstruction, NOT a bus
  capture; the OS mixer sum is not capturable on an emulator) and an RMS envelope.

Still NOT proven: real-song sound on his phone (the final verdict), session/notification continuity on his device, AutoMix tempo
reads on real music. His manual phone script above still applies; step 1 should now show the "Mixing" label exactly during overlaps.

## 2026-11-23 selected visualizer

User chose size-matched D after comparing D with E. CentreDisc now calls DropletBlob with its default reactive=false. No E sensitivity boost or separated droplets are active. Timer, flip gesture, dial size, colors and light gradient Mixing label are unchanged. D motion was inspected with synthetic input only; phone frame pacing and real-song sensitivity/jitter remain unverified. Version/release unchanged. Audio smoke 8 remains incomplete, not a pass; media-session ERROR cannot be attributed from its unscoped dump summary. No more smoke retries.

## 2026-11-25 additions to judge on the phone
- Whether beat-aligned overlaps are actually on the beat across genres (the 45 ms gate is a math bound, not
  an audible verdict), and how often pairs fall back to plain crossfade (grid confidence is the limiter).
- Whether the +/-6 dB RMS level trim makes overlaps level-matched without pumping; RMS is not BS.1770.
- Whether the whole-bar overlap length (up to 8 bars within the 12 s cap) feels right vs the fixed seconds.
- Deferred: bass-swap EQ handover between decks (needs per-deck DSP, device-risky), musical key, true
  two-deck time-stretching, and any cue-point skipping of musical intros/outros (out of authorised scope).
