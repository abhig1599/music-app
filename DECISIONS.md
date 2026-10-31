# Decision ledger

Labels: DECIDED (user or verified evidence) / PROPOSED (our design, not approved) /
PENDING (waiting on the user) / STUB (visible in app, intentionally not functional).

## Product decisions
- Android first (user, 2026-10-31 23:43). iPhone later, not now.
- Local files only; no streaming service, no ads (user, 23:37).
- Every Lark Player feature is reference scope (user, 23:49). Inclusion is not
  re-asked; account/paid/system-permission gates are still his decision.
- First version follows the Figma design faithfully (user, 2026-11-01 00:09).
- GitHub backup of source + all research, private repo (standing preference +
  user 00:09). No paid GitHub usage.
- v1 has a working local player core; download/import is a STUB - no download
  source chosen yet (PENDING: which source v1 ships with).
- Onboarding Step 3 "Read Now" reader: kept as an optional OFF toggle, no fake
  working AI. Reader behavior PENDING.
- Onboarding Step 1 language picker contains ONLY languages. (His Figma frame
  wrongly also lists Audio Sort options; observed mismatch, fixed in build,
  flagged in review.)

## Verified platform facts (official docs)
- READ_MEDIA_AUDIO (Android 13+) / READ_EXTERNAL_STORAGE (<=12) required to read
  other apps' audio: https://developer.android.com/training/data-storage/shared/media#storage-permissions
- Media-session notifications are exempt from POST_NOTIFICATIONS:
  https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Background playback needs mediaPlayback foreground service type:
  https://developer.android.com/about/versions/14/changes/fgs-types-required#media-playback
- Adding app-owned audio to the shared library: MediaStore.Audio insert with
  DISPLAY_NAME + RELATIVE_PATH + IS_PENDING; no storage permission for own files:
  https://developer.android.com/training/data-storage/shared/media#add-item
- Modifying other apps' files needs per-item consent
  (RecoverableSecurityException) or createWriteRequest() batch consent (11+):
  https://developer.android.com/training/data-storage/shared/media#update-other-apps-file
- System trash supports trash AND untrash (createTrashRequest(boolean)):
  https://developer.android.com/reference/android/provider/MediaStore
- Files an app contributes to shared MediaStore collections survive uninstall;
  after reinstall the app needs the read permission again (ownership reverts).
- Floating player needs overlay special access:
  https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION
- Ringtone setting needs write-settings special access:
  https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_WRITE_SETTINGS
- EncryptedSharedPreferences is deprecated:
  https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences

## PENDING product questions (already asked via WhatsApp)
1. Delete semantics: remove-from-library (app-hide) vs system file trash.
2. Download v1 sources (candidates: direct URLs, Jamendo, Internet Archive,
   Openverse discovery; SoundCloud requires paid Artist Pro - excluded unless he
   accepts cost; Spotify/YouTube: metadata-match only, no content download).
3. Telegram playlist-download bot/channel: exact link never supplied.
4. Backup scope: audio files vs playlists/settings metadata.
5. "Read Now" reader behavior.
6. Focus mode meaning + third bottom-right glyph on his player screen.
7. "Play last one to end" timer semantics + whether it counts while paused.
8. Hidden-folder songs appearing in playlists: play or skip.
9. Whether he already has T&C / privacy documents (none seen; needed for store).

## NOT verified / do not claim
- Queue screen: not found in the Figma file (searched twice) - reported as
  "not found", not "nonexistent".
- image 22 hidden layer contents unknown (file left untouched).
- No prototype wiring exists in the Figma file; every transition in the app is
  PROPOSED, matching standard Android navigation.
- Media3 download queue persistence across process death/reboot: primitives
  exist, real behavior must be tested on device.
- SoundCloud "downloadable" flag mechanics: current docs fetch failed once;
  only rate-limit page verified.

## 2026-11-01 01:15 IST - User steering: "Replicate the UI 100%"

- The Figma file is the VISUAL SOURCE OF TRUTH for every screen it contains: layout, spacing, type scale, colors (sampled tokens), icons, and the tick-dial. No reinterpretation of his authored screens.
- Lark and other references inform BEHAVIOR only, never replacement design.
- Features/screens the Figma does not contain (dark variant, themes picker, ringtone maker, backup, etc.) stay visibly labeled as PROPOSED additions; they must not silently blend into his authored screens.
- Verification bar: compare app renders at 390x844 against exact Figma node captures; measure spacing from the captures, don't eyeball from memory.
- Font: the exact typeface and its licensing for Android bundling must be identified from the file and flagged if it can't ship free. Until then renders use the closest system fallback and "100%" claims stay withheld.
- Android system constraints (status/nav bars, OEM font scaling, screen sizes) mean cross-device pixel-identity can't be promised; "100%" applies to the 390x844 reference canvas and must be pixel-inspected before any such claim.

## 2026-11-01 01:20 - Corrections after Main review
- Removed false claim "Nothing runs in the background without these" (notifications optional; media access exempt via manual file picking).
- Reader step: no invented function and no "nothing uploaded" promise; his Gemini/custom API fields kept as disabled/pending placeholders. Reader semantics still the user's open decision.
- T&C line no longer asserts consent; documents unpublished.
- Player: header "Playlist" + 3-dot, monospaced time, removed placeholder artist line, smaller play circle. Still NOT exact: tick geometry, control circles with light backgrounds, per-layer font weight (Helvetica Neue identified in Figma; embedding licence unchecked - not bundled).
- Fixtures prove render state only, not MediaController runtime integration.

## 2026-11-01 01:45 - Fidelity batch 3
- Player re-laid from measured Figma frame (68% zoom capture / 0.678): dial 296dp thin ticks, orange pointer cluster at top (behaviour vs progress UNVERIFIED), light 58dp control circles, 72dp bottom circles.
- Chip counts raised/small; mini-player has time under title plus large orange time. Reader setup fields shown as two disabled labelled inputs (Step 3 Figma frame not yet located in captures; structure approximate).
- Developer wording removed from reader subtitle.

## 2026-11-01 02:00 - Original onboarding frames recovered (Figma nodes 59-2742 Step 1, 59-2872 Step 2, 59-2916 Step3)
Earlier onboarding was a full-screen redesign; his is a rounded bottom sheet on grey Bg, bold title, monospaced body, orange Next button. Rebuilt to match (positions estimated from 68%-zoom captures).
Deviations kept deliberately: his language list contains placeholder sort labels (only English/Hindi kept); his "Permission1/2" are placeholders (real permission names in his toggle-card layout); Step 3 Gemini fields kept but disabled; his T&C consent sentence replaced because no documents exist; link "Create Google Gemini API" is unlinked text. Monospace font family unidentified (system Monospace used).
