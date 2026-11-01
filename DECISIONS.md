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
- 03:06 Geist Mono bundled: OFL 1.1 verified at github.com/vercel/geist-font LICENSE.txt and google/fonts ofl/geistmono/OFL.txt; licence copy in licenses/GeistMono-OFL.txt. Helvetica Neue still NOT bundled (proprietary, unchecked).

## 2026-11-01 03:30 - Bg 2 and sleep pill reads
- Bg 2 colour style definition (Edit color style dialog, opened read-only, not edited): **#E3E3E1**, 100%. Earlier F0F0F0 was wrong. Theme.kt updated. Other styles unchanged.
- Frame 45 (22-10030): 344.13x271.79 hug, vertical, gap 24. Strips 344x45.95 r10 white, stroke D3D3D1 1px inside; strip with overlapping orange "1:01 M" chip is 344.13x61.95 (gap -8). Strip 3 has orange "Time-up" pill on right, no centre cluster. "App exit in 0:10 S x" pill 239.51x45.95 r10 white, stroke D3D3D1.
- ESTIMATES (not read): chip padding/size, tick pitch/height/count, orange cluster position/alpha, font sizes in pills. SleepPill.kt is render-only, no timer behaviour.
- Mini-player small time now zero-padded "01:01" to match the design.
- Inset fixtures (home/player) paint 24dp/48dp translucent bars: inset fixture, not device validation.
- Typography: Helvetica Neue is proprietary and not bundled; Roboto substitutes (title tracking -0.8sp). Not an exact match.

## 2026-11-01 03:46 - Player responsive layout
Per Main: nav-bar overlap in the inset fixture is a layout bug. Player now uses BoxWithConstraints: identical to Figma at 844dp; below that the title/controls/bottom group anchors to the bottom, header stays at top, dial group scales (min 0.5) only to avoid collision. Horizontal axis still assumes 390dp width (not yet responsive). Verified only by inset fixture render, not device.

## 2026-11-01 04:12 - Sleep strip inner reads (Frame 45, strip 2)
- Strip: horizontal auto-layout, pad 6, gap 12, r10, White, 344.13x45.95 hug.
- Time chip (Frame 1): 65.56x33.95 hug, pad 6.97, gap 2.62, r8.72, fill Bg 2. "1:01" Geist Mono Regular 15.51 (38x20); "M" Inter Regular 12.21 (11x15). Note: Inter, not Helvetica.
- Tick frame (Frame 3): 168x23 hug, gap 7; Lines rotated, length 23, stroke D3D3D1 2px center => 25 ticks (168/7+1); layout width of a line is 0, so drawn pitch is 7 (2dp stroke + 5). Orange cluster still estimated.

## 2026-11-01 04:36 - Settings frame (node 47-14503, 390x1508, fill Bg)
Reads (Design panel): content column Frame 81 x22 y41, 351 wide, vertical gap 24: Frame 11 = 44x44 back circle (white, pad ~14/13), Frame 6 = title row 351x37, Frame 80 = cards, gap 6. Cards: white, r16, pad L14 T16 R14 B10, gap 13 inside. Heights: play time 82 (horizontal, gap 13, pad 14/10), Permission 166, Check for updates 221, Visibility collapsed 58, Visibility expanded 441, Read Now 58, Recycle Bin 58.
Content in the design: "07H 27M / Play Time", Permission1/2 with toggles (on/off), Current Version 00.00.100v, grey "Up To Date" + black "Update Availabe" buttons (two states shown), Visibility folder list (Recording, Download, Tunes, Call Recording, Y-music) with Save buttons.
My earlier 7-category Settings was my own proposal, not Figma. Replaced; old Equaliser/Theme/etc. entries kept under a labelled "More (proposed, not in the design)" section so those screens stay reachable. Inner font sizes are estimates. Play-time stat, toggles and update buttons are render-only (no listening tracker, permission wiring or updater exist). Sample text "Permission1/2" replaced with real permission names; "Up To Date" not asserted (no updater).

## 2026-11-01 05:35 - Settings: proposed block removed from the matched screen
Per Main 5:29: no labelled-proposed UI inside the faithful Figma screen. Equaliser/Theme/Download-location/Terms rows removed from SettingsScreen; their routes still exist in AppNav but have no entry point until a non-Figma home for them is decided (open decision). Fixture visuals now follow Figma (black "Update available", second Save enabled-looking); the buttons remain non-interactive (no updater/save logic) - appearance and behaviour are separate.

## 2026-11-01 05:45 - Recycle bin (47-14803) and Read Now (59-2982), both 390x844 fill Bg
Recycle bin reads: same header as Settings (Frame 81 x22 y41 gap 24; back 44 circle). Title row Frame 82 351 Fill x 37 Hug, horizontal, gap 24, "Recycle Bin" bold + "Empty Bin" mono pill right. List Frame 80 y129, 351 x 228 Hug, vertical gap 6. Row 351 x 72, horizontal, pad 10, radius 10, fill White. Left group 292x50, gap 13 (photo thumb ~50 + text "[songs name ]" sans, "2 days left" mono), undo icon 24x24 right.
Read Now (visual read at 68% zoom only, NOT Design-panel exact): two white r16 cards: "API Collection" with Gemini API row (name, "...hello", trash icon) and grey "Add API" button (~166 high); second card empty state "No API added yet" with black "Add API" (~265 high); explanatory note in mono under the cards (reproduced as drawn). Sizes/fonts estimated.
Implementation: BinReadScreens.kt. Real app: Recycle bin has no rows (delete semantics undecided) and Empty Bin is dimmed; Read Now shows only the empty-state card with a disabled Add API (Reader undefined, no key accepted or stored, no AI). Gemini sample row + bottom-sheet variants appear only as fixtures/not built (sheet "Add API Key" and "Custom API" variants not built; their API Name/Key fields would have to stay disabled). Photo thumbs replaced by neutral tiles. Settings Read Now / Recycle Bin rows now navigate to these screens.

Fix 05:57: Settings content width was 346 (22dp both sides); Figma content is 351 wide at x22, so end padding is 17 (390 reference). Horizontal axis still assumes 390dp.

## 2026-11-01 06:10 - Read Now exact reads (Design panel, node 59-2982)
Frame 81: 351 x 692.52 Hug, vertical gap 24, children: Frame 11 (back), Frame 82 (title row), Frame 42 (API card), Frame 47 (empty card), and a TEXT layer (the note) - all children of the same auto-layout, so the note and both cards are in-frame content, not annotation layers. (Whether the note is a designer's to-self remains unknown; runtime shows the real state: empty card only, Add API disabled. The note text is the designer's wording "Now I want to add a line..." and is carried in the fixture/screen as drawn, flagged here.)
Frame 42: 351x168 Hug, White, r16, pad L14 T16 R14 B10, gap 13; Frame 77 title row 105x20 (gap 6); Frame 78 API row 321 Fill x 46 Hug (bottom pad 4); Frame 32 button area 321 x 48 (vertical, gap 6). Frame 47: 351 x 267.52 Hug, same pad/radius. Note: Geist Mono Medium 12, 351 Fill x 80 Hug, line height Auto.
Correction: earlier estimates (cards 166/265, note 9sp, buttons 40) replaced with these. Estimated sizes elsewhere (Settings text, Recycle pill) probably under-read too: re-read next.
Open (tracked): Equaliser/Theme/Ringtone/Backup/Download-location screens have no entry point from the Figma Settings; routes kept, entry point undecided - do not drop, do not invent navigation.

## Sort sheet (node 47-14214) read 6:19
- Sheet Frame 43: 390x569 pad 20/20/20/142 gap 10 radius 24, fill White. Content column 351 gap 6; row frames 351x72 White r10 pad10 (same as Recycle rows) - hierarchy overlaps visually, only partly read.
- Visible: title "Audio Sort" bold ~22, options Newest(orange, sort icon right)/Most Played/A to Z/Artist/Long Duration/Sort Duration in Geist Mono ~12, pitch ~39 (estimate). Previous labels/Done button were NOT from Figma; replaced.
- Underlying screen is "Playlist" Home with orange "Update available" pill (Mono) - separate Home variant, not yet built.
- Selection visual only, not wired to list order.

## Download playlist flow (Figma 59-888/59-1056/59-1147/Download Detail 59-1730 neighbour) read 6:32
- Existing Downloads screen (All/Pending/Downloaded tabs + link stub) was MY proposal and did not match. Replaced.
- Figma "Download playlist page": Frame 81 x22 y41 351 gap 24; back 44x44 (White r22); title row 351x37; chips row 351x63 gap 12 (black "Spotify" pill with mono label, round YouTube chip); input row 344x48 gap 8 (field "Add playlist link..." + download button).
- Other frames: add-link (link typed, "www.youtube/playlist"), loading ("Loading Playlist / found 10 songs"), show all songs ("+ add all Songs" bar + song rows), downloading ("Downloading started" toast, X per row), "Download add in the home page" (Home with orange ring on download tile).
- Figma title typo "Downlaod Playlist": built as "Download Playlist" - flagged to Main.
- Spotify/YouTube as sources implies a download source decision (still open; legal/ToS). Real app: no network, button disabled. Chips are not functional selectors.
- "Downloads" list screen (All 20 / Pending 02 / Downloaded, Pause, rows with "Playlist Name") seen in frame 59-1730 neighbour: NOT yet built or measured.

- Main 6:44: fixtures keep source typo "Downlaod Playlist" (recorded as source typo), runtime says "Download Playlist". YouTube chip asset missing = not matched (blank circle). Sort/download controls render-only; source/ToS question stays open.

## Search frames (47-13151 Search, 47-13667 past records, 47-13936 recommendations) read 6:55
- Old Search was my proposal (back arrow, leading icon). Figma: no back button; header "Search..." + "Up to date" pill; field + orange search button; rows with photo thumb/title/mono subtitle; past records = X per row + "Remove all"; recommendations (query "Hello") = chevrons. Android keyboard drawn in the frames (system UI, not built).
- Rebuilt from 68% estimates (not Design-panel reads). Past records fixture-only (no history stored). Photo thumbs neutral tiles.
## Downloads list ("Download Detail", 59-2079)
- "Downloads" + "Pause all" mono pill; chips All 20 (orange)/Pending 02/Downloaded 06/...; rows with per-state icon (error !, progress ring, check, X). Seen, estimated row 72 gap 6 - not yet built.
- Download Detail built 7:17 as DownloadListFixture (fixture only, no route: no download engine, source/ToS open). Estimated geometry; state icons approximated (Material check/close, indicator ring); photos not shipped.
- Plain Search frame 47-13151 read 7:38 (fresh lease): empty body (field + orange button + keyboard). Real Search now shows no rows until typing. Past-records thumbs in Figma are photos (not shipped).
- 7:38 Equaliser/Themes frames: not located. Layers list visible top-level frames contain none; Settings frame (47-14503) has no EQ/Theme entries. Treated as NOT in Figma until a full layer survey proves otherwise; EQ/Theme screens in the app remain my proposals, unreachable from matched screens. Lease released.
- 7:50 Full canvas frame inventory done (no Equaliser/Themes labels found; labels read at 15%, some truncated; not absolute proof). Extra Figma frames to compare: Add to playlist x3, Speed sheet, Delete confirm, Lyrics x2, Queue. Home update-available pill built as render-only fixture (home_update_fixture); sync/downloading toasts and download progress ring NOT built (geometry unread).
## Player sub-frames (read 8:11, 50% canvas view; NOT Design-panel reads)
- Figma has Add to playlist (+ New playlist dialog), Speed (0.5/0.75x/1.0 (Default) checked orange/1.25x/1.5x/2.0x), Delete song confirm ("thi song will be move to "Recently delete" and will be deleted after 30 days." + "Deleat" red outline), Lyrics (empty: "No lyrics" + search/+ buttons; with lines, one orange), Queue (drag handle, photo, title, state glyph: check / playing / chevron). None existed in the app.
- Built as FIXTURES ONLY (PlayerSheetFixtures.kt), no wiring: no playlist store, speed control, delete/recycle semantics (open), lyrics source, queue editing. Source typos kept in fixture. Third-party sample lyrics NOT reproduced (placeholder lines). Delete dialog mark, lyrics leaf and photos are not shipped. The three-dots menu frame ("Song Pa...") not yet read.
## Player menu + focus/device frames (read 8:36, 50-103% canvas view)
- "Song Page - menu": items Sleep Time / Add to playlist / Share / Speed / Delete / Equaliser (drawn GREYED OUT = disabled in the design) / Lyrics. So Equaliser DOES have a (disabled) entry point in Figma; no Themes entry here. Built as fixture `Menu` (glyphs not matched, neutral squares).
- "Song focus mode": bottom-right button becomes a black X with a pill above it holding volume-off and moon buttons (fixture FocusOpen). "Song Focus connected device": headset button turns black, pill above with bluetooth / phone / speaker buttons (fixture DeviceOpen). Icon choices approximated with Material icons.
- Figma focus-mode closed glyph is a circular face-like icon, not Material SelfImprovement: still unmatched.
## Home "Download add in the home page" (59-1730, read 9:05 at 68-176%)
- Differences from our Home: Download button has an orange arc (~12 o'clock clockwise ~100deg, ~3dp) = fixture `downloadRingFixture`; sync ring centre icon is a folder-with-sync-arrows (ours: plain folder); source tile shows Spotify + YouTube outline marks (brand marks not shipped, placeholders kept); Read Doc button glyph is a captions+sparkle icon (ours: videocam stand-in). "Downloading started" black-outlined toast sits BELOW the frame edge in the canvas (outside the 390x844 frame), so not built into Home. "240 New Songs added" toast not yet located.
## Player overlays wired (9:20)
- Three-dots opens the Figma menu; Equaliser disabled (no EQ exists); other entries currently only close the menu (their sheets exist as fixtures, not wired: Speed/Add-to-playlist/Delete/Lyrics need real behaviour first). Focus button and headset button open the Figma popups; X/headset closes; scrim tap closes. Popup buttons (volume-off, moon, bluetooth, phone, speaker) are VISUAL ONLY: no device list, audio-route or focus capability implemented. Implemented = compiled + CI render; not executed on a device.
## Search exact Design-panel reads (10:17-10:19, read-only, layers panel selection)
- Header Frame 6: X26 Y46 W344 H37. Input row: X26 Y109 W344 H48 gap 8. Field: W288 fill, H48, pad L12/T14/R56/B14, radius 10, fill EAEAE8 (= Bg), placeholder Helvetica Neue Regular 18 / line 20 at (12,14). Button: 48x48, radius 10, Orangs.
- Past-records list Frame 71: X26 Y173 W344 H279, vertical gap 4. Row (Frame 43..): W344 H72 hug, pad 10, radius 10, fill White; content Frame 51 292x50 gap 13. "Remove all" frame: 344x51, vertical pad 16.
- Applied to SearchScreen: row 48/radius 10/18sp placeholder, rows 72 white pad 10 gap 4, list start 173, Remove-all 51. NOT read: field stroke (1dp D3D3D1 inferred from render, not a read), row title/subtitle font sizes (still 14/10 estimates), thumb radius, X icon size.
- Thumb-to-text gap 10 (Frame 51), action icon frame 24x24 (applied). Text font sizes of rows still unread.

## Download Detail exact Design-panel reads (59-2079)
Frame 81 at (22,41), 351 wide, hug, vertical gap 24. Back button 44x44 (padding 13.97/13.27, white fill). Header 344x37 hug. Chip group 475x46 (scrolls past frame edge; "more" chip still unread). Row list 351x306 hug, gap 6 => four 72px rows. Figma shows exactly 4 rows: error, progress, check, X (we had 6; fixed to 4). Section gap 24 applied (was 12).
Still unread: chip radius/padding/fonts, row text sizes, state-icon glyph sizes, Pause all pill metrics, back button radius. Photos not shipped.

## Download Detail exact reads, part 2 (59-2079)
Title "Downloads": Helvetica Neue Bold 30 (shared FigmaPage default stays 28 for other screens; Download Detail overrides to 30). "Pause all" pill: 111.95x36.95 hug, padding 6.97, radius 8.72, fill Bg 2 (#E3E3E1), auto-layout. Chip row Frame 4: 475x46, gap 12, padding 6, radius 10 (its white fill is not visible in the render, not applied). Chip: 65.56x36.95 hug, padding 6.97, radius 8.72, inner gap 2.62 (label to superscript), selected fill Orangs. Unselected chips assumed Bg 2 like the pill (not individually read).
Still unread: chip/pill text sizes, "more" chip, row text sizes, state-icon sizes, back button radius.

## Download Detail exact reads, part 3 (59-2079)
Row: 351x72, padding 10, radius 10, white. Left group 292x50: thumb 50, gap 10, text frame 153x43 vertical gap 4. Title Helvetica Neue Regular 18 (h21). Subtitle Geist Mono Medium 14 (h18, 110 wide). Status icon slot 32x32 (clip), glyph group 26.67 with 2px black round stroke (error "!" row read; check/progress/X glyph sizes not read individually). Applied: title 18 Regular, subtitle 14 Medium mono, gap 10/4, slot 32, error ring 26.67/2.
Still unread: chip label and superscript sizes, back chevron glyph, check/progress/X glyph sizes, Search row text sizes. Back icon is a chevron in Figma (ours arrow).

## Search row text (47-13667) - partly measured
Measured from hug boxes in the Design panel overlay: row title text box 93-113 x 21 hug (Helvetica Neue 18, same as Download Detail), placeholder 239x20, "Remove all" 344 fill x 19 hug.
INFERRED, not read from the font field: "Remove all" 16 (19px hug), subtitle mono 12 (visible width ~57px for "10 songs" = 7.2/char), header title 30 (header hug height 37 equals Download Detail's 30pt title). Applied these; font fields not individually opened.
Figma Search frames also contain an Android keyboard instance and "File or folder name" subtitle on row 1 (we render no keyboard; system IME at runtime).

## Download Detail part 4 (Design-panel reads, 59-2079, 11:50-11:52 IST)
- Back: vector chevron, 12x6 rotated -90 (6 wide x 12 tall), stroke 2, color token "Gray" (hex NOT read; 0xFF6B6B6B is an INFERENCE). Applied in FigmaPage for all screens (only read on this frame).
- Chip: auto-layout H, padding 6.97, radius 8.72, gap 2.62, Bg 2. Label Geist Mono Regular 18 (76x23 hug for "Pending"). Superscript Inter Regular 12.21 (16x15), top-aligned. Chip 94.62 wide. Superscript color not read (we reuse label color).
- Check: 24x24 Black filled vector in the 32 slot (inner check size not read).
- X: frame 24x24, vector 12x12 at (6,6), stroke Black 2 round, drawn with Canvas.
- Progress glyph size still unread. Roboto stands in for Helvetica Neue and Inter.

- Correction (Main, 12:24): chevron back is applied ONLY on Download Detail (chevronBack=true), where read. Other screens keep the earlier arrow, unverified against their Figma frames. Chip-row horizontalScroll is a fixture/runtime adaptation of Figma overflow (source shows clipping only; scrolling is not source-grounded).

## Download Detail part 5 (reads 12:41-12:43 IST)
- "Pause all": Geist Mono 18, hug 98x23 (read). Applied (was 13). Chip label and superscript line heights set to 23 / 15 to match read hug heights (chip 36.95).
- Row: Fill "White" (opacity not read), stroke D9D9D9 width 1 Inside (read). Composite fill SAMPLED from the 68% reference capture at F0F0F0 over Bg EAEAE8: that hex is a sample, not a panel read. Back circle also samples F0F0F0 (applied on Download Detail only).
- Not read yet: progress glyph (faint dotted track + two-bar pause in Figma), chip-row backdrop band, cause of the ~3-4dp vertical offset (line heights are the first suspect).

## Download Detail part 6 (reads 13:00 IST)
- Group 5 (chip row band): 475x46 at x0, corner radius 10, fill White (opacity unread; same F0F0F0 sample used). Chips centered vertically (36.95 in 46). Band runs past the frame edge and is clipped; ours is clipped at content width 351 (small difference). Left inset ~5 is from the capture, not read.
- Progress slot: 32x32 frame, inner Group 26.67x26.67 at (2.67,2.67), stroke 2 round, pause vector in the middle. Pause bar size/gap (2x8, gap 3) INFERRED. Figma track looks faint/dotted; ours is a plain faint ring.

## Search screen reads (13:28 IST, node 47-13151)
- Title "Search...": Helvetica Neue Bold 30, 126x37 hug, line height auto (was inferred, now READ).
- Search field: 288 wide (Fill) x 48 hug, auto layout, padding L12 T14 R56 B14, radius 10, fill EAEAE8, stroke token "Bg 2" (E3E3E1) 1px Outside. Placeholder text: Helvetica Neue Regular 18, line height 20, 239 hug, color token text/text-tertiary (hex NOT read). Applied: fill and stroke E3E3E1 (previous D3D3D1 was wrong). Stroke drawn inside, not outside (small adaptation).
- Search screen frame is 390x798 content above a 390x244 Android keyboard component (Figma library). Ours uses the system IME, not matched.
- Still unread: "Up to date" chip numbers on Search, thumb radius, Remove all font.

## Search chip (read 13:57 IST)
- "Up to date" on Search: Frame 2 auto layout, 121x36.95 hug, padding 6.97, gap 2.62, radius 8.72, fill token Bg 2 (E3E3E1). Text Geist Mono Regular 18, 108x23. Applied (was inferred 14sp/GrayLight).
- Search past-records frame (thumb radius, Remove all font) still unread.

## Search content width (14:23 IST)
- Figma shows the title/field rows as 344 wide (selection badge 344x48). In the capture the content spans x=26.5..370dp, so right inset is about 20, not 26. Ours was symmetric 26 (338 wide), which caused the chip to sit ~6dp left. Changed Search to start 26 / end 20. Left/right insets are DERIVED from the capture, not exact reads. Other screens untouched, unverified.
- Chip width (121) is consistent with Geist Mono 18 x 10 chars + 2x6.97 padding, so the chip itself was not the cause.

## Mono text width check (14:49 IST) - UNRESOLVED
- Bundled geist_mono.ttf is a variable font (fvar present), unitsPerEm 1000, advance 600 (=10.8dp at 18sp). Expected chip widths: "Up to date" 121.4, "Pause all" 111.
- Measured in CI renders (pixel scan of docs/screenshots): Search chip about 130, Download "Pause all" chip about 119. About 7-8dp wider than expected in both. Cause unknown (candidates: Robolectric text metrics, variable-font instance, rounding). Not fixed. Mono text widths are unverified vs Figma.

## Search past-records reads (14:50 IST, node 47-13667)
- Rows (Frame 43/44/45): 344 Fill x 72 hug, auto layout padding 10, radius 10, fill White (opacity unread). Content 322x50; text column 292 Fill x 50. Matches our row geometry (72, pad 10, radius 10).
- "Remove all" (Frame 72): 344 Fill x 51 hug, padding V16 H0, gap 10, NO fill, NO stroke. Matches our 51dp plain text. Font size (16 in ours) still not read.
- Row X: Vector with 2px Black stroke (same as Download Detail X). Ours uses Material Close icon: NOT yet replaced (non-match).
- Thumb radius: not read (panel selection got confused). Still inferred 8.
- Row stack gap between rows not read.
- 15:02 Applied: past-records X now 2px round Black Canvas stroke (12dp glyph in 24dp box; glyph size ASSUMED from Download Detail).
- 15:17 Hypothesis test (Main): theme-level ProvideTextStyle letterSpacing 0.sp to test whether M3 default letterSpacing (0.5sp) explains mono width delta (+0.5*chars ~ 5dp for 10 chars). Font sizes unchanged. Result pending CI.
- 15:47 RESULT (CI green 626dc64): letterSpacing 0 reduced mono chip widths: Search chip ~130->124 (Figma 121), Pause-all ~119->113 (Figma 111). M3 default letterSpacing explained most of the delta; residual ~2-3dp unexplained (pixel-scan of fill, anti-aliasing/padding not isolated). Still not exact.

## Past-records row interior reads (15:58 IST, node 47-13667)
- Thumb Frame 50: 50x50, radius 10, Image fill (photo asset not shipped: non-match), padding 16/16 in its own frame (unapplied, inner).
- Row gap thumb->text 10 (Frame 51 gap 10). Text column: hug 160x43, vertical, gap 4, padding 0. Applied: thumb radius 10 (was 8 inferred), column gap 4.
- Still unread: title/sub font sizes (ours 18 / Geist Mono 12, inferred), Remove-all font size, search magnifier glyph, row stack gap (ours 4, inferred).

## Past-records row reads, part 2 (16:27 IST, node 47-13667)
- Row Frame 43: fill White, stroke D9D9D9 100% 1px Inside, radius 10, no effects. Applied (1dp border; our border sits inside via padding after border = Inside).
- Row title: Helvetica Neue Regular 18, letter spacing 0%, line height Auto, 113x21 hug, fill Black. Confirms size 18 and letterSpacing 0 (for this text).
- X: Frame 24x24, clip content, Vector inside (vector size still not read directly; 12 assumed).
- Subtitle font/size NOT read (panel read failed; mono 12 still inferred). Remove-all font and magnifier glyph unread.
- Why rows looked greyer in the capture: stroke D9D9D9 plus canvas scaling; not a different fill.

## Past-records subtitle (16:43 IST) - DERIVED, not a panel read
- Figma text column hug width = 160 and "File or folder name" (19 chars) is the widest line; Geist Mono advance 0.6em => 160/(19*0.6) = 14.0. So subtitle size 14 (was inferred 12). Applied, marked derived.
- Overlay (CI e.g. a44c260): row fill still looks greyer in Figma than our White (fill opacity unread, stroke D9D9D9 applied but faint); title/subtitle spacing in ours is looser (line-height Auto vs Roboto metrics). Not changed.

## Row fill sampled (16:58 IST)
- Figma panel says row fill = style "White", but the rendered Figma capture samples F0F0F0 in row interiors (same value as the Download Detail chip band F0F0F0). So the "White" style likely is not FFFFFF (hex not read from the styles panel). SAMPLED, not read. Applied F0F0F0 to the Search past-records rows only. NOT applied globally: other screens that use "White" (PureWhite in code) need their own check.

## "White" style hex READ (17:09 IST)
- Figma Edit color style "White" (view only, closed without edits): F0F0F0, 100%. Reconciles the 16:58 sampled row fill: Search past-records row F0F0F0 is now source-grounded (row fill = style White).
- Theme.kt has `PureWhite = FFFFFF // "White"`: token mismatch with Figma. NOT changed globally yet. PureWhite is used in ~14 files; each use must be checked as (a) Figma style "White" -> F0F0F0 or (b) literal white (e.g. glyph on orange, keyboard) -> keep. Inventory: Common 6, SleepPill 4, Settings 4, BinRead 5, Downloads 3, DownloadList 2, Player 3, PlayerSheetFixtures 2, Themes 2, Backup/Eq/Onboarding/Ringtone/Search 1 each, Theme.kt 3.

## FigmaWhite token applied (17:21 IST) - per-use, source-grounded
- Added FigmaWhite = F0F0F0 (style read). PureWhite stays literal FFFFFF.
- Switched ONLY: Recycle bin rows (BinReadScreens:82, Figma "row ... White"), Read Now cards (109, 124; Frame 42/47 "White"), Home list row (Common:112; Home row "fill White"). 
- Not switched (no per-use Figma evidence yet): IconTile (Common:67), Settings, Sheets, Themes, Player, etc. Literal white uses (glyph on dark/orange) stay PureWhite.
- Row-only Search change is now grounded. Compiled change, CI pending; no overlay yet for Recycle/Read Now/Home.
- Recycle bin (47-14803) READ 17:49: title 'Recycle Bin' Helvetica Neue Bold 30, 169x37 hug, ls 0%, Black style; row title Helvetica Neue Regular 18, 113x21 hug; Frame 81 x22 y41 w351 gap24. Subtitle font still unread.
- APPLIED 17:50: Recycle title 30 Bold, chevron back (Figma shows chevron; glyph hex inferred as in Download Detail), row title 18, subtitle 14 (derived). Read Now sizes still unread, unchanged.
- APPLIED 18:06: Recycle rows get 1px D9D9D9 stroke (read earlier on other frames; Recycle-frame stroke itself visible in ref, hex assumed same, not panel-read on this frame). 6% width diff is an estimate, not measured.
- Read Now (59-2982) READ 18:13 (view-only): "API Collection" Helvetica Neue Medium 16 Black (105x20 hug); "...hello" Helvetica Neue Regular 14 Gray (42x17); "Gemini API" Geist Mono Medium Black (size not confirmed); "No API added yet" Helvetica Neue Regular 14 centered Black (275x17); note Geist Mono Medium 12 Gray 351x80 hug; Add API button 321x48 r10 pad 12/12 gap10 fill Black, text White style. Top card (has API) button is GREY, bottom card (empty) button is DARK: dark = the enabled-looking one. Figma also has frames 'Read Now - Add Geminu API' (typo) and 'Read Now - Add Custom API' (sheets), not implemented.
- APPLIED 18:15: those type values. Dark Add API now drawn at full opacity (Figma look) but remains NON-FUNCTIONAL (Reader undefined, no key stored). Visual says enabled, behaviour is inert: flagged to Main as a decision.
- 18:17 (Main direction): shipped Read Now must not show an inert enabled-looking action. ReadNowScreen(designComparison=false) default: Add API at 50% alpha, label "Add API (unavailable)", disabled. designComparison=true (screenshot fixture read_now_fixture only) draws the Figma dark button at full opacity for measuring. Added read_now_fixture_shipped. Prototype wiring of Figma not verified.
- 18:24 CORRECTION (Main): the "design fixture" (fixtureApiName=Gemini API, '...hello', troubleshooting note) is DESIGN-COMPARISON ONLY. Shipped state = read_now_empty: no saved API, Add API unavailable, note replaced by an honest 'not available yet' line. The Figma note text (delete and re-add API to fix the feature) renders only with designComparison=true. Removed read_now_fixture_shipped.
- PROVENANCE CORRECTION: row stroke D9D9D9 1px Inside was panel-read on Download Detail / Search rows, NOT on the Recycle frame. On Recycle it is ASSUMED from those frames (border visible in the reference image); batch10 wording 'known read' was too strong for this frame.
- Read Now READ 18:22: title Helvetica Neue Bold 30 (148x37 hug); 'Gemini API' Geist Mono Medium 16 Black (96x21); Add API label Geist Mono Medium 16, line height 24 (68x24 hug), Gray when disabled; trash is an outlined vector 18x20, stroke Gray 1.5 Round, no fill. APPLIED 18:25: title 30, mono name 16, button label 16 Medium, outlined trash (Material Outlined.Delete = approximation), chevron back (hex inferred). Unavailable copy shortened per Main: 'Read Now is not available yet. No API key is stored for this feature.' (earlier 'nothing sent anywhere' removed: not established).
- 18:33 Core: Search (shipped path) now filters real MediaStore songs by title/artist and tap plays via PlayerManager; Folders screen groups MediaStore RELATIVE_PATH (API 29+ only; empty state otherwise). Compiled by CI only, NOT runtime-tested. Home Folders/Playlist chips still DemoData (to fix). Seek is not wired to any UI control (Figma player shows time only; no seek control read). No file deletion/bin mutation added.
- Emulator CI allowance: cannot be checked with the repo-scoped deploy key (no billing API access). BLOCKER stated, no minutes invented.
- 18:34 Folder-row tap = immediate play of the folder is MY ASSUMPTION, not grounded in Figma (no folder-detail frame found in my reads; DECISIONS lists none) or the brief. Marked ungrounded/provisional; replace if a folder-open flow is found. RELATIVE_PATH is guarded by SDK_INT>=29 (hasRel); below that the projection omits it, folders list is empty with a message (query does not fail). minSdk=26, so API 26-28 devices get no folder grouping (fallback would need the deprecated DATA column; not implemented).
- 18:40 Home READ (36-12141, view-only): left block Frame 68 = 162x162 hug, vertical autolayout pad 12 gap 12, radius 24, fill Gray-light. Search circle (Frame 12) = 63x63, radius 31.5, fill style "White" (=F0F0F0 per earlier style read), NOT literal FFFFFF. APPLIED to circleBg (all Home circles). Sort/Settings/Download/Lyrics/Video circles NOT individually read: ASSUMED same style. Base frame has no mini-player (song-playing is separate frame "Home page - song playing"). Line ~215 white background and tile glyphs still unread.
