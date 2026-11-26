# Recovery - how to resume this project after any reset

## Where things live
- Source + docs: THIS repository (public since 2026-11-25, abhig1599/music-app).
  Everything needed to continue is committed here. Because the repo is public,
  commit only public-safe content: no credentials or access routes, no private
  links, no approval/conversation records, no internal operating instructions.
- UI design: the owner's private Figma draft (ask the owner for access; treat it
  as read-only).
- Debug APK: GitHub Actions artifacts (14 days) + Releases when a build is
  published. Convention: one versioned debug APK per delivered change.

## State

### Current (2026-11-26)
- main is at the RECOVERY.md docs commit on top of be742d30 (bot screenshot
  push-back on merge 01620b90). CI GREEN on the merge: run 45776396722
  (build + unit suite via Roborazzi record + screenshots).
- Latest test build: release cand-0.1.9-test.1, built from 7f54cdec -
  versionCode 9 / versionName "0.1.9-test.1", package com.abhi.musicapp.test
  (installs beside any release-package app and updates prior test installs in
  place). Signed with the repo debug.keystore; the signing certificate was
  verified identical to cand-0.1.8's from the actual binaries. Asset +
  SHA256SUMS.txt are on the release page. On-device playback validation of
  this build is pending.
- PR #1 merged 2026-11-26: AutoMix tries shorter whole-bar overlaps
  (8->4->2->1 bars) when the longest candidate fails every existing gate,
  instead of falling straight back to plain crossfade. Strict-superset change:
  previously-accepted plans are unchanged; the index-locked residual gates and
  the bar-preference-never-rejects rule are preserved. Branch CI green
  (45774443205). This change is NOT in the 0.1.9-test.1 APK; shipping it needs
  a new build.
- Branch sweep 2026-11-26: combined-batch (fully merged into main),
  queue-sizing (only a bot screenshot commit ahead, behind 286),
  delivery-cand-0.1.6 (orphan, no common ancestor). Nothing outstanding to
  merge.
- MixQualityHarnessTest: the MIXQA / detector-imperfection tables are printed,
  not asserted; expect planning rates to shift upward after PR #1 (more
  short-window plans). The assertNull fallback contracts still hold.
- Open technical work: vocal-to-vocal handoff (the incoming track still starts
  at zero; no intro skip, no vocal cue), queue cold-start restore unverified,
  no on-device audible validation of any mix yet, android.yml cand-*
  auto-release notes template still says "Private repo release" (stale; fix
  when next editing the workflow).
- Transition diagnostics: adb logcat -s TransitionAnalysis:D during playback
  prints one mixdiag line per analysed pair (grid confidence, first-beat
  offset, bar evidence, planned-vs-fallback reason).

### 2026-11-23 (superseded by the 2026-11-26 entry)
- Feedback then: the transition "cuts the old song, plays the next" instead of
  overlapping; a "Mixing" gradient indicator was wanted in place of the "Local
  music" label while a transition runs. Addressed on main by boundary-aligned
  fades, the AutoMix/Crossfade gate and the Mixing indicator; the remaining
  bar is the device listening check above.
- Emulator evidence ceiling: PCM taps + logged gains and a reconstructed mix
  (mix_reconstructed.wav) prove the app's output stream, not the OS-mixed
  acoustic sum. Phone playback remains the final verdict.

### 2026-11-22: 0.1.7 released (historical)
- Published cand-0.1.7 at commit 8aa92b5, version 0.1.7/code 7. Asset
  music-app-cand-0.1.7-debug.apk (SHA-256 f22c6ebbd855b56ed1ca1f59c857966ffb392d487fcf7b72663457c77fe95d8a).
  The version-assertion fix that followed (SeparateTestAppTest) changed no
  playback code. Real-music audio quality was unverified at that point.

### 2026-11-21
- AutoMix became a real opt-in path ("AutoMix (beta)" under Settings > Song
  Transitions, default off, acts only while Crossfade is on): tempo-matched
  whole-bar overlap (no beat-phase alignment) + <=6% incoming tempo nudge.
  Unit suite + Robolectric tests green; not heard on a device.

### 2026-11-20
- main carried the combined November batch: queue row sizing/padding, centred
  clay-blob visual, output picker, right-rail buttons, overflow menu, Settings
  crossfade (real overlap, default Off), AutoMix research planner (unused),
  tempo prefetch gate (default off). cand-0.1.6 was the installed build.

### Historical: v1 milestone (2026-11-01)
- v1: design-faithful UI skeleton (onboarding, home, player, search, settings,
  downloads, folders, playlist detail) + Media3 service skeleton, demo data
  only. Pixel-fix loop and MediaStore wiring followed later.

## How to continue
1. Pull the repo. Read DECISIONS.md (what is decided/pending) and this file.
2. Build: ./gradlew :app:assembleDebug (JDK 17, SDK 34). Local builds on small
   boxes may OOM - use GitHub Actions (the established route).
3. Verify visuals by rendering screens (Roborazzi) and comparing against the
   design before claiming any screen done.
4. Phone test script for mixing: docs/MIXING_QA.md.
5. No paid services, releases or merges without the repository owner's
   explicit go-ahead.
