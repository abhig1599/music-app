# Research log

Everything load-bearing, with sources. "Verified" = checked against the linked
official source on 2026-10-31 / 2026-11-01 IST. "Unverified" = do not rely on it.

## Download sources (his ask: free, playlist-capable, link-based)
- **No verified free + unlimited + mainstream-catalog download API exists** in
  bounded search. Verified free routes are CC/public-domain class or per-host:
  - Direct user-supplied file URLs - per-host feasibility, UNVERIFIED per host.
  - Jamendo API - free tier ~35k requests/month cap:
    https://developer.jamendo.com/v3.0 - CC catalog. Needs API key + implementation.
  - Internet Archive - per-item rights vary: https://archive.org/developers/
  - Openverse - discovery/metadata only, rate-limited: https://api.openverse.org/v1/
  - SoundCloud - registration REQUIRES paid Artist Pro subscription:
    https://developers.soundcloud.com/docs/api/register-app
    Rate limits: https://developers.soundcloud.com/docs/api/rate-limits
    (~100 req/min general; streams 1,500/24h; search 3,000/h; downloads 10/min).
    Excluded from free scope unless he accepts a paid dev account.
  - Spotify - policy forbids downloading content; playlist METADATA export is
    allowed: https://developer.spotify.com/policy and
    https://developer.spotify.com/documentation/web-api/reference/get-playlist
  - YouTube - ToS forbids downloading: https://www.youtube.com/static?template=terms
    API: https://developers.google.com/youtube/v3/docs/playlistItems/list and
    https://developers.google.com/youtube/terms/api-services-terms-of-service
  - Media3 downloading mechanics (for allowed sources):
    https://developer.android.com/media/media3/exoplayer/downloading-media
- YMusic (his reference) reviewed at https://ymusic.io - site only, not installed.

## Lark Player (his feature reference)
- Listing: https://play.google.com/store/apps/details?id=com.dywx.larkplayer&hl=en
- Help center: https://larkplayer.zendesk.com/hc/en-us
  - Hidden songs: /articles/27956645195415
  - Gapless/crossfade/play-together: /articles/27956707422359
  - Playing modes: /articles/27956581206039
  - Lark does NO downloads itself: /articles/27956495462935
- Feature parity scope per user 23:49: local playback, EQ, lyrics, timer, themes,
  video, ringtone maker, floating window, Google Drive backup, landscape/tablet.
  Live in-app settings NOT exhaustively inspected (no hands-on install).

## Android platform (permission/behavior verification)
See DECISIONS.md "Verified platform facts" for the full URL list. Key ones:
- Media permissions: https://developer.android.com/training/data-storage/shared/media
- Notification exemption: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- FGS type: https://developer.android.com/about/versions/14/changes/fgs-types-required#media-playback
- Bluetooth permissions (connected-device features):
  https://developer.android.com/develop/connectivity/bluetooth/bt-permissions

## Figma review (2026-10-31, read-only, no edits)
- ~66 top-level layers, ~45 authored screens/variants pixel-reviewed.
- Light theme only; dark theme = new variants.
- No prototype wiring anywhere. Queue screen not found. Font modal: Helvetica Neue.
- Placeholders in his copy ("Permission1/2", "[Playlist/Source]", "version 00.00.00")
  replaced or flagged in build.

## 2026-11-03 04:00 Read Doc. = speak-to-document (user clarification, design/prep only, nothing built)
User: the "Read Doc." tile is for recording yourself speaking and getting a document (checklists, notes, meeting notes). Options for the speech step, no code written, no permission added (manifest has no RECORD_AUDIO and no INTERNET today):
A. Android SpeechRecognizer (system service, free, no key). createOnDeviceSpeechRecognizer needs API 31+ and an installed on-device language pack; plain SpeechRecognizer may send audio to the phone vendor's cloud service. Short utterances, not long meeting recording. Source: developer.android.com/reference/android/speech/SpeechRecognizer.
B. Vosk (Apache-2.0, fully offline, ~50 MB small models, Indian English available, lower accuracy). Source: alphacephei.com/vosk/models.
C. whisper.cpp (MIT, offline, tiny/base models, better accuracy, heavier, slower on phones, model download needed).
D. His own Gemini/custom API key (the Figma "Enable Reader" fields): better text and structuring but audio leaves the phone and key storage is a vault/secret matter. Not assumed.
Text-to-document step (checklist/notes) is separate: plain local formatting rules work offline; "AI summary" needs D.
Open product decisions: engine and privacy (offline vs cloud), where recordings/docs live and whether audio is kept, doc format (checklist / notes / meeting notes), editor ("moti kind of thing" unresolved).
