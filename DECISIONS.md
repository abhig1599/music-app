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
- 02:05: Hindi shown but disabled ('not available yet') until real localization exists; Step3 copy no longer repeats the 'free' API claim (pricing unverified).
- 02:05: onboarding vertical offsets (+11dp), bold mono button/permission titles after overlay vs Figma.

## 2026-11-01 02:20 - Home/player rebuilt from 1:1 overlay (crop of Figma frame rescaled to 390x844)
Home: header y~65 with mono "Up to date" pill; grey block with four white circle buttons; BARE scan dial (12 thick rays, orange fading cluster upper-left - exact cluster/alphas estimated) with folder icon; Read Doc. grey card + grey card with two circle buttons (his are brand-style Spotify/YouTube marks - NOT bundled, generic icons stand in); grey inactive chips with mono labels; rows with photo thumbs (not shipped; neutral tiles); mini-player with mono big time and orange colon.
Player: thicker/longer ticks (48 ticks, 6dp), longer centre tick, lighter 62dp control circles, bigger mono time, medium-weight 28sp title. Sleep-timer variants ("App exit in 0:10 S", "Time-up") remain UNBUILT requirements.

## 2026-11-01 02:40 - EXACT Figma Design-panel reads (read-only, no Dev Mode)
Player (390x844, Bg): header x26,y46 344x37 hug; dial group x48,y135 301x301; rays Rectangle 5x42 radius 6 Gray-light (inner r~108.5, outer 150.5), orange ticks at top; time Frame 7 x147,y252 104x68 gap1, "1:01" Geist Mono Regular 35.68, "M" Helvetica Neue 20, 1px Gray-light line 104w, "03:12" Geist Mono 15.51; title Frame 18 x23.5,y489 344x71 gap12: label HN Medium 16, title HN Medium 32 centred; controls x89,y659 205x63 gap 8, three 63 circles (White, r31.5, pad 20/19; centre orange pause); bottom circles 72x72 at x19,y726 and x298,y726, r36, fill Bg 2, pad 24; Queue row x154.5,y757 79x24 gap 6.
Home (36-12141, 390x844): header Frame x26,y46 344x37 hug (title + mono "Up to date" pill); circle block Frame 68 x26,y109 162x162 vertical, pad 12 gap 12, radius 24, fill Gray-light; dial Group 9 x223,y116 147x147; Read Doc. card Frame 70 x26,y287 162x162 radius 24 clip, fill D9D9D9; second card Frame 65 x208,y287 162x162 radius 24 Gray-light pad 12 gap 12; chip row Group 5 x20,y465 475x46 (overflows the 390 frame, scrolls horizontally), chips fill White, radius 10, stroke D3D3D1 1px inside; list Frame 41 x20,y527 351x306 vertical gap 24 field; rows have 1px borders and chevrons, playing variant rows show thumbs; mini-player Frame 70 (song-playing frame) x18,y757 355x78 hug horizontal pad 14/18 radius 14 fill Black.
Fonts: Geist Mono (OFL; licence check before bundling), Helvetica Neue (not bundled, licence unchecked).
Onboarding Step1 sheet Frame 43 x0,y339 390x569 radius 24 pad 20/20/20/142; Next Frame 44 x21,y770 351x48.
Sleep pill Frame 45: 344x272 hug vertical gap 24, three timeline strips + "App exit in 0:10 S x" pill (still unbuilt).
Caveat: chip text/inner paddings and row height not individually read; row pitch approximated from frame (306 hug for 4 rows).
- 02:52: home geometry moved to exact reads (header y46/37, blocks 162 r24 Gray-light, gap 12, dial 147, cards y287, white circles). player fixture now same state as Figma (Malcolm Todd - Earrings, 1:01/03:12 playing). Bot render at 0000c08 showed fixtures differing from Figma state ("Nothing playing", 0:00) - fixed.

## 2026-11-01 03:05 - More exact reads (Design panel, read-only)
- Chip row = Frame 4: white 475x46, r10, 1px D3D3D1 inside stroke, auto-layout horizontal gap 12, pad 6; ONE container, it overflows the 390 frame (Artists clipped in Figma; kept).
- Chips: "All" 65.56x36.95 hug, pad 6.97, r8.72, fill Orangs; "Folders" 108.56x36.95, fill Bg 2 (inactive chips are Bg 2, not white).
- List: Frame 41 x20,y527 351x306; Frame 32 rows vertical gap 6; row Frame 42 = 351x72 hug, pad 10, r10, fill White, stroke D3D3D1 1px inside; thumb+text group 156x50 gap 13 (thumb 50); chevron 24x24 frame rotated 90.
- Mini-player (song-playing frame): title + small "01:01" under it AND big mono "01:01" with orange colon; x18,y757 355x78 pad 14/18 r14 Black.
- NOT resolved: exact Bg 2 hex (panel shows style name only; pixel-sampling of the screenshot was unreliable). #F0F0F0 kept from earlier token record and stays UNVERIFIED.
- Home fixture now the same state as Figma mini-player (01:01 / 03:12).
