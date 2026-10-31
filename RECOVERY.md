# Recovery - how to resume this project after any reset

## Where things live
- Source + docs: THIS repo (private, abhig1599/music-app). Everything needed to
  continue is committed here - never rely on agent-local storage.
- Figma design: https://www.figma.com/design/qZQCBqzvSKC9gJ46TzEkz2/Music-App
  (Abhishek's Draft, read-only for us - never edit his file).
- Debug APK: GitHub Actions artifacts (14 days) + Releases when a build is
  published. Convention from Inkline: versioned APK on Releases per change.

## State
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
