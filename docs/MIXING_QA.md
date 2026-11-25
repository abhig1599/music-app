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

## 2026-11-25 synthetic planner harness

MixQualityHarnessTest (unit test, JVM) sweeps made-up beat grids through MixPlanner.plan: 8 outgoing tempos
(90-150 bpm), 8 tempo relations (same, +/-2%, +5%, +7%, half, double, +20%), incoming grid first-beat
offsets 0..1 beat period, per-beat Gaussian jitter sigma 0/10 ms, 12 s fade cap, 25 trials per jittered
cell. It prints aggregate planned-vs-fallback rates to the unit-test log (lines prefixed MIXQA; CI shows
unit-test standard output) and asserts the plan contract on every returned plan: start on an outgoing
beat, end exactly at the detected content end, overlap within [500 ms, cap], max residual <= 45 ms, nudge
within +/-6%. Fixed anchors: ideal aligned grids always plan; a half-period grid offset never does; tempos
outside the 6% nudge cap or the half/double match window never do; identical inputs give identical plans.

SYNTHETIC ONLY: the printed rates measure the planner maths on made-up grids. They are NOT measured
fallback rates on the real library and say nothing about how any mix sounds. What the sweep does pin down:
with the index-locked matching (2026-11-25 patch), planning requires the incoming grid's first beat within
~45 ms of the song start (an offset beyond ~10% of a beat period always falls back), so real-world fallback
frequency depends directly on where the beat detector anchors its first beat - exactly what the phone test
must judge.

## 2026-11-25 synthetic planner harness - measured table

MixQualityHarnessTest (unit test, JVM) sweeps made-up beat grids through MixPlanner.plan: 8 outgoing tempos
(90-150 bpm), 8 tempo relations (same, +/-2%, +5%, +7%, half, double, +20%), incoming grid first-beat
offsets 0..1 beat period, per-beat Gaussian jitter sigma 0/10 ms, 12 s fade cap, 25 trials per jittered
cell. It prints the table below to the unit-test log (CI shows unit-test standard output) and asserts the
plan contract on every returned plan: start on an outgoing beat, end exactly at the detected content end,
overlap within [500 ms, cap], max residual <= 45 ms, nudge within +/-6%. Fixed anchors: ideal aligned grids
always plan; a half-period grid offset never does; tempos outside the 6% nudge cap or the half/double match
window never do; identical inputs give identical plans.

Measured on CI run https://github.com/abhig1599/music-app/actions/runs/45652253791 (commit 4f6a6c8, green):

- total planned 2565/9828 = 26.1%
- by incoming grid first-beat offset: 0.0P 988/1404 = 70.4%; 0.02P 976/1404 = 69.5%; 0.05P 589/1404 =
  42.0%; 0.1P 12/1404 = 0.9%; 0.25P/0.5P/1.0P = 0%
- by jitter: sigma=0 120/378 = 31.7%; sigma=10ms 2445/9450 = 25.9%

SYNTHETIC ONLY: these rates measure the planner maths on made-up grids. They are NOT measured fallback
rates on the real library and say nothing about how any mix sounds. What the sweep pins down: with
index-locked matching (2026-11-25 patch), planning requires the incoming grid's first beat within ~45 ms
of the song start (an offset beyond ~10% of a beat period always falls back), so real-world fallback
frequency depends directly on where the beat detector anchors its first beat - exactly what the phone test
must judge.

## 2026-11-25 bar-phase (downbeat) evidence

BarPhaseAnalyzer tags each beat grid with its bar phase from measured audio accents: low-band (120 Hz
one-pole) RMS rise per beat against a pre-beat baseline, averaged per bar position (index mod 4), 4/4
assumed, min 8 bars. The strong-pair margin over the odd positions gives the confidence; a p/p+2 tie is
reported as two-beat ambiguity at reduced confidence, and BeatWindow attaches NO bar position for an
ambiguous grid (MixPlanner abstains; the flag stays on BeatGrid for QA). Weak evidence returns null,
which keeps the beat-only behaviour everywhere. Beats are never moved and genuine onset delays stay visible - bar position
is a tag, not a relabeling of time. MixPlanner uses confident bar positions on BOTH grids only to PREFER
the passing candidate whose index-locked pairs share a bar position (downbeat on downbeat); it never
rejects a candidate for bar reasons, so the fallback rates above are unchanged by this addition. All
synthetic tests (kick-on-downbeat click tracks, ties, abstention, +/-30 ms grid-phase tolerance);
unverified on real songs until the phone test.

Meter scope (synthetic probe, exact-math mirror of the analyzer, click tracks): the estimator assumes
4/4 and has no meter detection. Measured 2026-11-25: period-3 accent patterns (3/4, three variants)
always abstain - a period-3 accent spreads uniformly over the 4 bar positions, so no position clears
the average; 6/8 with two accents per bar abstains; 6/8 with a single accent every 6 beats folds onto
two positions 2 beats apart and is caught by the two-beat-ambiguity flag, which BeatWindow treats as
abstain. So no tested non-4/4 pattern can steer the planner. This is 5 synthetic patterns, not meter
detection: unusual real content could still produce a false-positive tag, and true multi-meter support
remains future work.
