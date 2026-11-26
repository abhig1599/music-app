# Recovery - how to resume this project after any reset

## Where things live
- Source + docs: THIS repo (public since 2026-11-25, abhig1599/music-app). Everything needed to
  continue is committed here - never rely on agent-local storage.
- Figma design: https://www.figma.com/design/qZQCBqzvSKC9gJ46TzEkz2/Music-App
  (Abhishek's Draft, read-only for us - never edit his file).
- Debug APK: GitHub Actions artifacts (14 days) + Releases when a build is
  published. Convention from Inkline: versioned APK on Releases per change.

## State

### Current (2026-11-26, ~13:15 IST)
- Repo is PUBLIC (since 2026-11-25). The android.yml cand-* auto-release notes
  template still says "Private repo release" - stale text, fix when next
  editing the workflow.
- main is at be742d30 (bot screenshot push-back on top of merge 01620b90).
  CI GREEN on the merge commit: run 45776396722 (build + unit suite via
  Roborazzi record + screenshots pushed back).
- Test APK DELIVERED 2026-11-26 ~12:01 IST via GitHub Releases (user approved
  11:47 IST): https://github.com/abhig1599/music-app/releases/tag/cand-0.1.9-test.1
  - asset music-app-cand-0.1.9-test.1-debug.apk + SHA256SUMS.txt, built from
  7f54cdec, versionCode 9 / versionName "0.1.9-test.1", package
  com.abhi.musicapp.test (installs BESIDE any release-package app and updates
  prior test installs in place; no uninstall needed). Signed with the repo
  debug.keystore; cert SHA-256 verified equal to the cand-0.1.8 APK's cert
  from the actual binaries (55:9B:E8:D6:AC:B1:D8:F5:5E:49:B4:79:B6:2F:D2:F4:
  DC:06:61:04:9A:1A:C5:95:09:0A:CA:3F:5B:B7:D1:15).
  AWAITING his device feedback: do repeated pauses still happen, does the
  blend sound continuous; optional evidence via adb logcat -s
  TransitionAnalysis:D (mixdiag lines).
- PR #1 (codex/automix-shorter-bars) MERGED 2026-11-26 ~13:05 IST per his
  voice instruction: AutoMix now tries shorter whole-bar overlaps (8->4->2->1
  bars) when the longest candidate fails every existing gate, instead of
  falling straight back to plain crossfade. Strict-superset change:
  previously-accepted plans are unchanged; index-locked residual gates and
  bar-preference-never-rejects rule preserved. Branch CI green (45774443205).
  GitHub blocks approving your own PR, so the review was recorded as a PR
  comment and the PR merged. The merged change is NOT in the 0.1.9-test.1
  APK - any new APK needs a fresh go-ahead.
- Branch sweep 2026-11-26: combined-batch (0 ahead of main - already merged),
  queue-sizing (only a bot screenshot commit ahead, behind 286),
  delivery-cand-0.1.6 (orphan, no common ancestor). Nothing else to merge;
  branch deletion NOT authorized.
- MixQualityHarnessTest: the MIXQA / detector-imperfection tables are PRINTED,
  not asserted; expect planning rates to shift upward after PR #1 (more
  short-window plans). assertNull fallback contracts still hold (CI green).
- Open product gaps (unchanged): vocal-to-vocal handoff (incoming track still
  starts at zero; no intro skip, no vocal cue), queue cold-start restore
  unverified, no audible validation of any mix on a real phone yet.
- Access: deploy key 'instinct-agent-2026-11-23' was REVOKED by design
  (2026-11-25). All repo writes go through the GitHub web UI in the signed-in
  browser session. Public-repo API reads (runs, artifact lists, releases) work
  unauthenticated; artifact/log downloads need auth (browser session, or
  nightly.link + run-page digest check for artifacts).
- Authorization snapshot: the 0.1.9-test.1 delivery and the PR #1 merge were
  individually approved by the user (11:47 IST text "Yes"; 13:01 IST voice
  note). Silent-tail-only content authorization (2026-11-23 voice note) still
  stands: never skip musical intro/outro content. NOTHING beyond this is
  pre-authorized - each new APK, release or merge needs its own go-ahead.

### 2026-11-23, ~16:15 IST (superseded by the 2026-11-26 entry)
- Post-0.1.7 feedback (his voice notes + brief, 15:24-15:35 IST): transition
  "cuts the old song, plays the next" - not the intended overlap; he wants a
  "Mixing" indicator (gradient, Apple Music reference, cycling dots) in place of
  the "Local music" label while a transition runs; bar = real-song real-sound QA.
- Working branch state: fixes on top of 4ba44c7 (main) -
  boundary-aligned fades (ramp ends at the natural end; no early chop),
  AutoMix/Crossfade switch coherence (and gate), "Mixing" gradient indicator
  from the service-side state, XFADE diagnostics, PCM tap (PcmTap, -PpcmTap=true builds only),
  emulator transition smoke (workflow 'Transition smoke', manual dispatch).
  NO release built or published; a 0.1.8 candidate still needs his go-ahead.
  His actual 0.1.7 switch states are unconfirmed.
- Emulator evidence is an emulator ceiling: PCM taps + logged gains and a
  reconstructed mix (mix_reconstructed.wav) prove the app's output stream, not
  the OS-mixed acoustic sum. His phone test remains the final verdict.
- Access: repo deploy key titled 'instinct-agent-2026-11-23' (ed25519,
  read+write, this repo only) added under Settings > Deploy keys. The key can be
  deleted from the repo any time; repo backups are unaffected (it is
  transport-only). If write access is needed again afterwards, generate a FRESH
  key pair and add its public half as a new deploy key (GitHub takes the public
  key). The matching private key is also stored in the user's Instinct vault
  ('music-app GitHub deploy key', login kind) as a write-only backup: saved
  secrets cannot be read or copied back out, so it is a safe-deposit record, not
  a restore source - fresh-pair rotation is the restore path.
- The 2026-11-22 state below describes the 0.1.7 release, which stands; nothing
  about it changed. The 0.1.7 'user approved publishing' entry is a snapshot
  claim inside this file, not standing release authority for anything new.

### 2026-11-22, 13:52 IST: 0.1.7 released (historical)
- User approved publishing the private test APK at 13:29 IST in a direct reply
  to the publication question. Published cand-0.1.7 at commit
  8aa92b541f753e717103b2e12625d9f66bc42686, version 0.1.7/code 7.
- Verified release page and attached APK (18.9 MB):
  https://github.com/abhig1599/music-app/releases/tag/cand-0.1.7
  Asset: music-app-cand-0.1.7-debug.apk
  GitHub asset SHA-256: f22c6ebbd855b56ed1ca1f59c857966ffb392d487fcf7b72663457c77fe95d8a
- Main CI success record names 8aa92b5. The initial version-bump run failed
  because SeparateTestAppTest still expected 0.1.6; corrected assertion to
  0.1.7, then verification passed. No playback code changed in that fix.
- Release link and inspected release screenshot returned for user delivery.
  GitHub sign-in required. Select the APK under Assets, not source archives.
- Prior releases untouched, same signing configuration. No billing settings
  changed. Do not assert remaining free minutes or zero charges without a
  billing check; no paid usage was authorized.
- Real-music audio quality remains unverified. Prior emulator smoke covered
  basic playback health on older source, not audible mixing or beat phase.
- Earlier held-candidate instructions below are historical, superseded by
  this publication. No further release/build authorized by this closeout.

### Historical held candidate (2026-11-22, morning)
- Held candidate awaiting the user's explicit APK approval (asked 2026-11-21 ~18:48 IST,
  re-asked 2026-11-22 morning; NO approval yet - do not release without it).
- Candidate head: e6d428a on origin/main = code 551c93e + CI screenshot push-back.
  Carries: AutoMix beta (32a027a), QA wording + keyed tempo cache (974636b),
  never-persist-no-metadata-keys cache policy (340ba57), docs (ca944e7),
  CI workflow hardening (551c93e: rebase-retry on push-back, artifact quota non-blocking).
- Verification evidence:
  * CI GREEN on 551c93e: docs/ci-last-success.txt names 551c93e5992376c304a52ce526e433fc3b835e94
    (build + full unit suite + Roborazzi renders; screenshots pushed back as e6d428a).
  * Emulator smoke run #10 (manual dispatch, main@974636b):
    https://github.com/abhig1599/music-app/actions/runs/44906508974
    Verdict from raw job log: "SMOKE VERDICT: our MediaSession PLAYING, PAUSED, PLAYING
    observed; no FATAL (emulator evidence only)"; UI pause/resume taps 1/1; synthetic tone,
    API 34 emulator, no real music files. The run's red X is ONLY the artifact upload
    hitting the free storage quota - upload-only, not a build or smoke failure
    (upload steps now continue-on-error so quota cannot mask a verdict again).
  * Earlier CI #210 "failure" was the same infra-only class (push-back race + quota);
    its build/tests were BUILD SUCCESSFUL.
- Settings renders inspected: docs/screenshots/settings_transitions.png (AutoMix off),
  settings_transitions_automix_on.png (on) - copy honestly says tempo-matched,
  beats NOT aligned.
- EXACT RELEASE STEPS once he says yes (and only then):
  1. Bump app/build.gradle.kts to versionName "0.1.7" / versionCode 7 (pattern:
     "Bump to 0.1.6 (code 6) for test APK of latest batch"). Commit + push to main.
  2. Tag that commit cand-0.1.7 and push the tag. NEVER reuse/overwrite cand-0.1.6.
  3. android.yml's cand-* path builds and publishes the private pre-release
     music-app-cand-0.1.7-debug.apk with the honest experimental notes.
  4. APK is signed with the committed debug.keystore like every cand-* build, so it
     installs as an update over his 0.1.6 (no key change, no reinstall).
  5. Verify the release exists (gh release view cand-0.1.7 or the Releases page),
     then send him the link via WhatsApp; include docs/MIXING_QA.md's 7-step phone
     test script. If the artifact-upload step quota-fails, the RELEASE is unaffected
     (releases attach via gh, not upload-artifact).
  6. Deliveries to him: Telegram Saved Messages or Google Drive as fallback
     (never Gmail).

### 2026-11-21
- On top of 5d10844: AutoMix is now a real opt-in path ("AutoMix (beta)" under Settings > Song
  Transitions, default off, acts only while Crossfade is on): tempo-matched whole-bar overlap (no beat-phase alignment) +
  <=6% incoming tempo nudge + 1.5 s ease-back, plain-crossfade fallback. Code: analysis/AutoMix.kt,
  Crossfader (plannedTransition, restore ease), PlaybackService.analyseForAutoMix.
- Verified locally this time: JDK 17 + SDK 34 toolchain, FULL unit suite 205 tests green incl. new
  AutoMixTransitionTest and CrossfaderSwapTest nudge/ease cases. Still NOT heard on a device;
  ranked risks + phone test script in docs/MIXING_QA.md.

### 2026-11-20
- main is at 5d10844 (screenshots commit on top of code commit 718a383, the
  last CI-green code). This head carries the combined November batch: queue
  row sizing/padding, centred clay-blob visual, output picker, right-rail
  buttons, overflow menu, Settings crossfade (real overlap, default Off),
  AutoMix research planner (unused), tempo prefetch gate (default off).
- Latest candidate release: cand-0.1.6 (tag 5f4101e), the build the user
  installed. No APK or release is approved beyond it.
- Batch QA limits and the accepted visual direction are in DECISIONS.md under
  "2026-11-19/20 - Combined batch". NOTHING in the batch ran on a phone.
- Open user choices (Delete semantics, moon button, APK go-ahead) are listed
  in DECISIONS.md "PENDING product questions".

### Historical: v1 milestone (2026-11-01, kept for the record)
- v1 milestone: design-faithful UI skeleton (onboarding 3 steps, home, player,
  search, settings, downloads with his All/Pending/Downloaded tabs, folders,
  playlist detail) + Media3 service skeleton. Demo data only - MediaStore
  library query not wired yet. Download/import is an intentional stub.
- Next: (1) CI green check, (2) screenshot verification of screens vs Figma,
  (3) MediaStore library layer + real playback through the service,
  (4) pixel-fix loop, (5) then user go-ahead for APK delivery.

## How to continue
1. Pull the repo. Read DECISIONS.md (what's decided/pending) and this file.
2. Build: ./gradlew :app:assembleDebug (JDK 17, SDK 34). Local builds on small
   boxes may OOM - use GitHub Actions (established route).
3. Verify visuals by rendering screens and comparing against the Figma
   screenshots before claiming any screen done.
4. Deliver per the working style: screenshots + summary first, wait for his
   explicit go-ahead, then share the APK. Zips/APKs reach him via Telegram
   Saved Messages or Google Drive (never Gmail).
5. Do not touch his other repos or his Figma file. Do not spend money
   (no paid GitHub usage, no paid APIs).

## User decisions still pending
See DECISIONS.md "PENDING product questions" - isolate them; don't block
unrelated work on them.

## Update 2026-11-01
- Latest reads (Bg 2 = #E3E3E1, Frame 45 geometry, chip pill, rows, mini-player) are in DECISIONS.md.
- Testing limits: Roborazzi renders on Robolectric at w390dp-h844dp. Inset fixtures are not device validation; fixtures prove render state only, not MediaController runtime. No emulator CI (Actions allowance unverified).
