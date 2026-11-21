# Mixing QA: Crossfade + AutoMix (2026-11-21)

What changed today: AutoMix is now a real, opt-in audible path on top of Crossfade (Settings > Song
Transitions > "AutoMix (beta)", default off). When both songs have a detected tempo close enough to match,
the overlap is timed to whole bars and the incoming song runs nudged (max 6%) to the playing song's tempo,
then eases back to the user's speed over ~1.5 s after the swap. Anything unknown falls back to exactly the
plain crossfade. This is tempo-rate matching ONLY: no beat-phase or downbeat alignment, so the overlap is not
beat-matched, and a wrong tempo read can sound worse than the plain crossfade (see risk 3). Crossfade itself
is unchanged in behavior. The tempo cache key includes file size + modified time, so a replaced file that
keeps its MediaStore id is re-analysed instead of inheriting a stale tempo.

## Verification done here (CI-class, no device)
- Unit tests: planner edges, transition planning (fallbacks, caps, 6% nudge bound), speed-ease maths,
  swap/cancel lifecycle with a nudged incoming player (Robolectric, real ExoPlayer objects, no audio).
- Compile + full unit suite + Roborazzi renders via GitHub Actions (see docs/ci-last-success.txt).

## Ranked remaining risks (what a phone test must judge, in order)
1. Session hand-over on a real phone: notification, lock-screen controls and the queue must keep showing the
   right song after MediaSession.setPlayer. Compile-checked only.
2. No beat-phase alignment exists: the fade start is a wall-clock window (polled every 250 ms), not snapped
   to a detected beat grid, so even a perfect tempo match can overlap off-beat by any fraction of a beat.
   Onset-phase estimation with beat-grid-snapped starts is the top candidate for the next iteration. Tempo
   analysis itself is tested on synthetic click tracks only, never on real songs.
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
