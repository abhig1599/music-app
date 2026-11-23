
## 2026-11-23 Apple published guidance (primary pages, fetched 2026-11-23) vs the user-supplied reference text
Sources: https://support.apple.com/en-us/105067 , https://support.apple.com/guide/iphone/transition-songs-iphadf2fe1f4/ios , https://support.apple.com/guide/music/transition-songs-muse5e9ec085/mac
Confirmed by Apple's pages:
- Crossfade is a set number of seconds, user slider 1 to 12 s (our slider matches). Both Crossfade and AutoMix are unavailable with Hi-Res Lossless.
- AutoMix is for Apple Music subscription catalog content on iOS/iPadOS/macOS 26 or later, and "automatically selects the best transition, depending on the music" (for example removing silence at track starts/ends, or a simple crossfade when appropriate). Apple's page lists no AutoMix for: albums played in sequential order, incompatible genres/tempos, content uploaded from a library, iTunes-purchased downloads. "Albums and some genres play without transitions."
- AutoMix is NOT available on Android or the Apple Music app for Windows.
Consequences for this app: local files are outside Apple's own AutoMix scope, so any "resemblance" is to Apple's observable behaviors only (overlap, boundary-aligned fade, tempo matching), never a claim to match Apple's internal engine, which Apple does not publish.
Not found on the fetched pages (treat as unverified in the user-supplied text): an "Automatic" smart-duration mode on Android, a fixed silence threshold, a universal linear-fade loudness dip, and specific track examples. We use equal-power fades (no mid-point dip) regardless.
Open question for a later release decision (not implemented, not promised): Apple skips transitions between sequential album tracks; this app does not, and whether it should is the user's call.
