# Crossfade hand-over: what is guessed and what would remove the guess

Current design (experimental, not heard on a phone): `main` plays the queue; near the end a second ExoPlayer plays the next song on the same
audio session with equal-power gains. When the ramp ends, `main` seeks into the next song at the incoming player's position and the two swap
over a short 120 ms window, then the incoming player is released.

Known weakness: for those 120 ms two copies of the same song are audible and they may be offset by the time `main` needed to become ready
after its seek (phasing or a small step). The code seeks ahead by a learned latency (starts at 150 ms) and stores the measured offset
(Settings > Song Transitions shows "last hand-over offset") so a phone test gives a number, not only an impression. This does not prove aligned
playback. CI cannot play audio.

Design that avoids two copies: let the incoming player BECOME the active player. The session and UI talk to a thin wrapper that forwards to
whichever ExoPlayer is current; after the ramp the roles swap and the old player is released. Nothing is replayed, so nothing can be offset.
Cost: the media session, queue state, equaliser, visualiser and mute all have to follow the swap, which touches PlayerManager and the service.
That is a larger change than this batch's "no mass refactor", so it is documented here and not started. Decide after a phone test of the
current design shows whether the offset is audible.
