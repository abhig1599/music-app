# Crossfade hand-over

Earlier design (replaced): after the ramp, the active player seeked into the next song at the incoming player's position and the two swapped
over a short window. That replays the same song on two players, so the copies could be offset (phasing or a step). Shortening the swap or
guessing the seek latency only reduced it.

Current design (experimental, not heard on a phone): the active player `main` owns the queue and the media session. Near the end a second
player, built the same way (same audio-level tap) and holding the same queue starting at the next song, plays on the same audio session
with equal-power gains. When the ramp ends, `MediaSession.setPlayer` points the session at the incoming player and the old player is
released. There is no seek and no second copy of a song. The incoming player keeps its own position, so the song never restarts.

What this relies on and nobody has heard or tested on a device: the session and its notification keep showing the right song and controls
after setPlayer, the shared audio session keeps the equaliser attached, and the visualiser keeps getting levels from the new player. Edge
cases cancel the crossfade instead of swapping: pause, seek, next/previous, queue edits, repeat-one, shuffle on, sleep "finish song".
Mute and speed changes during the ramp are carried to the incoming player.
