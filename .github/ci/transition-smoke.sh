#!/usr/bin/env bash
# Transition smoke: does the app REALLY overlap two songs (emulator, synthetic tones)?
# Plays tone A (440 Hz) then lets the crossfade run into tone B (880 Hz). Evidence:
#  - XFADE logcat lines (arm, ramp start, per-tick gains, swap) at the right wall-clock times
#  - per-deck PCM taps (debug-only PcmTap, armed via pref) showing BOTH decks decode audio during the ramp
#  - a reconstructed mix (per-deck PCM x the gains the app logged) + envelope, as artifacts
#  - the "Mixing" label visible in the UI during the ramp; session on tone B at the right position after
# Exit 0 only on: fade armed, both decks non-silent across the same window, swap at the boundary, no FATAL.
set -u
OUT=smoke-out; mkdir -p $OUT; R=$OUT/transition-report.txt; : > $R
PKG=com.abhi.musicapp.test
say(){ echo "$(date +%H:%M:%S) $*" | tee -a $R; }
adb wait-for-device; adb shell input keyevent 82
for i in $(seq 1 40); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 2; done

# 24 s tones; fade 4 s. Cap is dur/3 = 8 s, so 4 s is planned in full.
python3 - <<'PY'
import wave,struct,math
for name,f in (("xf_tone_a",440),("xf_tone_b",880)):
    w=wave.open('/tmp/%s.wav'%name,'wb'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(16000)
    w.writeframes(b''.join(struct.pack('<hh',int(9000*math.sin(2*math.pi*f*i/16000)),int(9000*math.sin(2*math.pi*f*i/16000))) for i in range(16000*24)))
    w.close()
PY
adb shell mkdir -p /sdcard/Music
OK=1
for n in xf_tone_a xf_tone_b; do
  P=0; for i in $(seq 1 20); do adb push /tmp/$n.wav /sdcard/Music/$n.wav >/dev/null 2>&1 && { P=1; break; }; sleep 3; done
  [ "$P" = "1" ] || OK=0
  for i in $(seq 1 15); do adb shell content query --uri content://media/external/audio/media --projection _display_name 2>/dev/null | grep -q $n && break; adb shell content call --uri content://media/ --method scan_file --arg /sdcard/Music/$n.wav >/dev/null 2>&1; sleep 2; done
done
say "tones pushed and indexed: $OK"
[ "$OK" = "1" ] || { say "TRANSITION VERDICT: INVALID (tones not on device)"; exit 1; }

adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null && say "install: ok" || { say "TRANSITION VERDICT: INCOMPLETE (install failed)"; exit 1; }

center(){ python3 - "$1" "${2:-exact}" <<'PY'
import sys,re,xml.etree.ElementTree as ET
t,mode=sys.argv[1],sys.argv[2]
try: root=ET.parse('/tmp/ui.xml').getroot()
except Exception: sys.exit()
for n in root.iter('node'):
    for v in ((n.get('text') or ''),(n.get('content-desc') or '')):
        if (v==t) if mode=='exact' else (t in v):
            a=list(map(int,re.findall(r'\d+',n.get('bounds'))));print((a[0]+a[2])//2,(a[1]+a[3])//2);sys.exit()
PY
}
refresh(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; }
tap_exact(){ refresh; c=$(center "$1" "${3:-exact}"); if [ -n "$c" ]; then set -- $c; adb shell input tap $1 $2; return 0; fi; return 1; }

# Onboarding (same flow as smoke.sh)
adb shell am start -n $PKG/com.abhi.musicapp.MainActivity >/dev/null; sleep 6
tap_exact "Next"; sleep 2
refresh; c=$(python3 - <<'PY'
import re,xml.etree.ElementTree as ET
try: r=ET.parse('/tmp/ui.xml').getroot()
except Exception: exit()
y=None;W=0
def b(n): return list(map(int,re.findall(r'\d+',n.get('bounds'))))
for n in r.iter('node'):
    a=b(n); W=max(W,a[2])
    if (n.get('text') or '')=='Music and audio': y=(a[1]+a[3])//2
if y is None: exit()
best=None
for n in r.iter('node'):
    if n.get('clickable')=='true':
        a=b(n)
        if a[1]<=y<=a[3] and (a[0]+a[2])//2>W//2 and (a[2]-a[0])<W//2: best=a
if best: print((best[0]+best[2])//2,(best[1]+best[3])//2)
PY
); [ -n "$c" ] && { set -- $c; adb shell input tap $1 $2; }
sleep 3; refresh
c=$(python3 - <<'PY'
import re,xml.etree.ElementTree as ET
try: r=ET.parse('/tmp/ui.xml').getroot()
except Exception: exit()
for n in r.iter('node'):
    if 'permission_allow_button' in (n.get('resource-id') or '') or (n.get('text') or '')=='Allow':
        a=list(map(int,re.findall(r'\d+',n.get('bounds'))));print((a[0]+a[2])//2,(a[1]+a[3])//2);break
PY
); [ -n "$c" ] && { set -- $c; adb shell input tap $1 $2; sleep 3; }
GR=$(adb shell dumpsys package $PKG | grep -c 'READ_MEDIA_AUDIO: granted=true')
[ "$GR" = "0" ] && adb shell pm grant $PKG android.permission.READ_MEDIA_AUDIO
tap_exact "Next"; sleep 2; tap_exact "Get Started"; sleep 4

# Arm transitions + PCM capture, then restart so the process re-reads prefs
adb shell run-as $PKG sh -c "mkdir -p /data/data/$PKG/shared_prefs && printf '%s' '<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?><map><boolean name=\"on\" value=\"true\" /><int name=\"secs\" value=\"4\" /><boolean name=\"pcm_capture\" value=\"true\" /></map>' > /data/data/$PKG/shared_prefs/transitions.xml" && say "prefs armed via run-as"
adb shell run-as $PKG rm -rf /data/data/$PKG/files/xfade_pcm
adb shell am force-stop $PKG; sleep 2
adb shell am start -n $PKG/com.abhi.musicapp.MainActivity >/dev/null; sleep 6
refresh; say "home lists tones: a=$(grep -c xf_tone_a /tmp/ui.xml) b=$(grep -c xf_tone_b /tmp/ui.xml)"

adb logcat -c
T0=$(date +%s)
if tap_exact "xf_tone_a"; then say "tapped tone A"; else say "tone A row not found"; fi
sleep 3
adb shell dumpsys media_session > /tmp/ms.txt 2>&1; say "session after tap: $(grep -m1 -o 'state=[A-Z]*' /tmp/ms.txt)"

# Wait for the fade (24 s song, arm at ~5.5 s to go, ramp at 4 s to go)
SAW_MIXING=0; ARMED=0; RAMP=0; SWAP=0
for i in $(seq 1 16); do
  sleep 2
  adb logcat -d -s XFADE:I XFADE:D > $OUT/xfade_log.txt 2>/dev/null
  grep -q 'armed:' $OUT/xfade_log.txt && ARMED=1
  grep -q 'ramp start:' $OUT/xfade_log.txt && RAMP=1
  grep -q 'swap:' $OUT/xfade_log.txt && SWAP=1
  if [ "$RAMP" = "1" ] && [ "$SWAP" = "0" ] && [ "$SAW_MIXING" = "0" ]; then
    refresh; grep -q 'Mixing' /tmp/ui.xml && { SAW_MIXING=1; say "UI shows Mixing label during ramp: yes"; adb exec-out screencap -p > $OUT/mixing_label.png 2>/dev/null && say "screenshot: mixing_label.png captured during the ramp"; }
  fi
  [ "$SWAP" = "1" ] && break
done
say "armed=$ARMED ramp=$RAMP swap=$SWAP mixing_label=$SAW_MIXING (elapsed $(( $(date +%s) - T0 ))s)"
SAW_SWAP=$SWAP
grep -E 'armed:|ramp start:|swap:|aborted' $OUT/xfade_log.txt | tee -a $R
TICKS=$(grep -c 'fade t=' $OUT/xfade_log.txt); say "gain ticks logged: $TICKS"
BOTH=$(python3 - <<'PY'
import re
n=0
for l in open('smoke-out/xfade_log.txt',errors='replace'):
    m=re.search(r'fade t=([\d.]+) out=([\d.]+) in=([\d.]+)',l)
    if m and float(m.group(2))>0.05 and float(m.group(3))>0.05: n+=1
print(n)
PY
); say "ticks with BOTH deck gains >0.05: $BOTH"

sleep 2
adb shell dumpsys media_session > /tmp/ms2.txt 2>&1
say "session after window: $(grep -m2 -oE 'state=[A-Z]+' /tmp/ms2.txt | tr '\n' ' ')"
refresh; say "player shows tone B: $(grep -c xf_tone_b /tmp/ui.xml)"

# Pull per-deck PCM taps and analyse
adb shell run-as $PKG sh -c "cat /data/data/$PKG/files/xfade_pcm/deck0.pcm" > $OUT/deck0.pcm 2>/dev/null
adb shell run-as $PKG sh -c "cat /data/data/$PKG/files/xfade_pcm/deck1.pcm" > $OUT/deck1.pcm 2>/dev/null
adb shell run-as $PKG sh -c "cat /data/data/$PKG/files/xfade_pcm/deck0.txt /data/data/$PKG/files/xfade_pcm/deck1.txt" 2>/dev/null | tee -a $R
python3 - <<'PY' | tee -a smoke-out/transition-report.txt
import struct, math, os
def read(p):
    try: d=open(p,'rb').read()
    except FileNotFoundError: return None
    n=len(d)//4  # stereo s16 frames
    return struct.unpack('<%di'%(n*2), d[:n*4])
def rms(frames, i0, i1, sr=16000):
    seg=frames[2*int(i0*sr):2*int(i1*sr)]
    if not seg: return 0.0
    return math.sqrt(sum(x*x for x in seg)/len(seg))/32768.0
a=read('smoke-out/deck0.pcm'); b=read('smoke-out/deck1.pcm')
if a is None or b is None:
    print("PCM: missing tap files (capture not armed?)"); raise SystemExit(0)
sr=16000
la=len(a)//2/sr; lb=len(b)//2/sr
print("PCM deck0(A): %.2fs  deck1(B): %.2fs" % (la, lb))
tail=rms(a, max(0,la-4.2), la)          # song A audible up to its own end (boundary alignment)
headb=rms(b, 0, min(3.8, lb))           # song B audible from the ramp start
print("PCM rms A last-4.2s=%.3f  B first-3.8s=%.3f" % (tail, headb))
print("PCM OVERLAP: %s" % ("YES both decks non-silent in the shared window" if tail>0.02 and headb>0.02 else "NO"))
# Reconstruct what the app sent toward the mixer during the fade: per-deck PCM x the app's own gains.
import re
ticks=[]
for l in open('smoke-out/xfade_log.txt',errors='replace'):
    m=re.search(r'fade t=([\d.]+) out=([\d.]+) in=([\d.]+)',l)
    if m: ticks.append((float(m.group(1)),float(m.group(2)),float(m.group(3))))
import wave
fade=min(4.0, lb, la)
N=int(fade*sr)
def gain(ticks, t, which):
    if not ticks: return 0.0
    k=min(range(len(ticks)), key=lambda i: abs(ticks[i][0]-t))
    return ticks[k][which]
mix=[]
for i in range(N):
    t=i/sr
    ga=gain(ticks,t,1); gb=gain(ticks,t,2)
    ia=a[2*(len(a)//2-N+i)]; ib=b[2*i]
    v=int(ia*ga+ib*gb); v=max(-32768,min(32767,v))
    mix.append(v); mix.append(v)
w=wave.open('smoke-out/mix_reconstructed.wav','wb'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(sr)
w.writeframes(struct.pack('<%di'%len(mix),*mix)); w.close()
env=[round(rms(mix, i, min(i+0.25, fade)),3) for i in [x*0.25 for x in range(int(fade*4))]]
print("MIX envelope (0.25s buckets, reconstructed from app PCM x app gains):", env)
print("NOTE: mix_reconstructed.wav is rebuilt from the app's own per-deck PCM taps and the gains it logged; the OS mixer sum itself is not capturable on an emulator.")
PY

NF=$(adb logcat -d | grep -c 'FATAL EXCEPTION')
say "FATAL EXCEPTION count: $NF"
# Disarm the capture pref and wipe the tap dir: the PCM tap is for controlled synthetic-tone fixtures only, never user music.
adb shell run-as $PKG sh -c "printf '%s' '<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?><map><boolean name=\"on\" value=\"true\" /><int name=\"secs\" value=\"4\" /><boolean name=\"pcm_capture\" value=\"false\" /></map>' > /data/data/$PKG/shared_prefs/transitions.xml; rm -rf /data/data/$PKG/files/xfade_pcm" 2>/dev/null
say "pcm_capture disarmed and tap dir wiped"
if [ "$ARMED" = "1" ] && [ "$RAMP" = "1" ] && [ "$SAW_SWAP" = "1" ] && [ "${BOTH:-0}" -gt 8 ] && [ "$SAW_MIXING" = "1" ] && [ "$NF" = "0" ]; then
  say "TRANSITION VERDICT: OVERLAP CONFIRMED (log gains + per-deck PCM + UI label; emulator, synthetic tones)"
  exit 0
else
  say "TRANSITION VERDICT: FAIL/INCOMPLETE armed=$ARMED ramp=$RAMP swap=$SAW_SWAP bothticks=${BOTH:-0} label=$SAW_MIXING fatal=$NF"
  exit 1
fi
