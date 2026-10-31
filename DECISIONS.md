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
