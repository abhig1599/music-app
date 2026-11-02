#!/usr/bin/env bash
# One-shot emulator smoke. Reports what happened; never edits the app. Synthetic tone only, no user data.
# Exit 0 only if our package's MediaSession was seen PLAYING, then PAUSED, then PLAYING again, with no FATAL.
set -u
OUT=smoke-out; mkdir -p $OUT; R=$OUT/report.txt; : > $R
PKG=com.abhi.musicapp
say(){ echo "$(date +%H:%M:%S) $*" | tee -a $R; }
adb wait-for-device; adb shell input keyevent 82
for i in $(seq 1 40); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && adb shell ls /sdcard >/dev/null 2>&1 && break; sleep 2; done
say "device sdk: $(adb shell getprop ro.build.version.sdk | tr -d '\r')"
focus(){ say "  [$1] pid=$(adb shell pidof $PKG | tr -d '\r') focus=$(adb shell dumpsys window | grep -m1 mCurrentFocus | sed 's/.*u0 //')"; }
# 90 s stereo tone so the session stays active long enough to observe play, pause, resume
python3 - <<'PY'
import wave,struct,math
w=wave.open('/tmp/smoke_tone.wav','wb'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(16000)
w.writeframes(b''.join(struct.pack('<hh',int(6000*math.sin(2*math.pi*440*i/16000)),int(6000*math.sin(2*math.pi*440*i/16000))) for i in range(16000*90)))
w.close()
PY
adb shell mkdir -p /sdcard/Music
TONE_OK=0
if adb push /tmp/smoke_tone.wav /sdcard/Music/smoke_tone.wav >$OUT/push1.txt 2>&1; then TONE_OK=1; say "tone push (90s) ok"; else say "tone push FAILED: $(head -2 $OUT/push1.txt)"; fi
adb shell content call --uri content://media/ --method scan_file --arg /sdcard/Music/smoke_tone.wav >/dev/null 2>&1
sleep 3
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null && say "install: ok" || { say "install: FAILED"; say "SMOKE VERDICT: INCOMPLETE (install failed)"; exit 1; }
adb logcat -c
shot(){ adb exec-out screencap -p > $OUT/$1.png; }
refresh(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; cp /tmp/ui.xml $OUT/$1.xml 2>/dev/null; }
# nodes: print "x y" of the first node whose text/desc EQUALS arg1 (exact), or contains it when arg2=sub
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
# clickable node on the same row as the node whose text equals arg1, right half of the screen
row_toggle(){ python3 - "$1" <<'PY'
import sys,re,xml.etree.ElementTree as ET
t=sys.argv[1]
try: root=ET.parse('/tmp/ui.xml').getroot()
except Exception: sys.exit()
def b(n): return list(map(int,re.findall(r'\d+',n.get('bounds'))))
y=None;W=0
for n in root.iter('node'):
    a=b(n); W=max(W,a[2])
    if (n.get('text') or '')==t: y=(a[1]+a[3])//2
if y is None: sys.exit()
best=None
for n in root.iter('node'):
    if n.get('clickable')=='true':
        a=b(n)
        if a[1]<=y<=a[3] and (a[0]+a[2])//2>W//2 and (a[2]-a[0])<W//2:
            best=a
if best: print((best[0]+best[2])//2,(best[1]+best[3])//2)
PY
}
labels(){ say "UI[$1] texts: $(python3 - <<'PY2'
import xml.etree.ElementTree as ET
try:
    r=ET.parse('/tmp/ui.xml').getroot(); o=[]
    for n in r.iter('node'):
        t=(n.get('text') or n.get('content-desc') or '').strip()
        if t: o.append(t[:140])
    print(' | '.join(o[:40]))
except Exception: print('dump unreadable')
PY2
)"; }
tap_exact(){ refresh "$2"; c=$(center "$1" exact); if [ -n "$c" ]; then set -- $c; adb shell input tap $1 $2; say "tap exact label: ok"; return 0; fi; say "tap exact label: NOT FOUND"; return 1; }
# our package's MediaSession PlaybackState lines, with the owning package
pbs(){ adb shell dumpsys media_session > /tmp/ms.txt 2>&1; python3 - "$PKG" <<'PY'
import re,sys
pkg=sys.argv[1]; cur=None; out=[]
for l in open('/tmp/ms.txt',errors='replace'):
    m=re.search(r'package=(\S+)',l)
    if m: cur=m.group(1).rstrip(',')
    s=re.search(r'state=PlaybackState \{state=(\w+)\((\d+)\), position=(\d+)',l)
    if s: out.append((cur,s.group(1),s.group(3)))
mine=[o for o in out if o[0]==pkg]
print('OURS:' + (';'.join(f'{a}/{b}/pos{c}' for a,b,c in mine) if mine else 'none') + ' ALL_OWNERS:' + ','.join(sorted(set(str(o[0]) for o in out))))
PY
}
adb shell am start -n $PKG/.MainActivity >/dev/null; sleep 6; shot 01_launch; refresh 01_launch; focus launched; labels language
tap_exact "Next" s1; sleep 2; shot 02_permissions; refresh 02_permissions; focus after_next1; labels permissions
c=$(row_toggle "Music and audio"); if [ -n "$c" ]; then set -- $c; say "audio toggle node found by clickable-in-row"; adb shell input tap $1 $2; else say "audio toggle node NOT found by row"; fi
sleep 3; shot 03_after_audio_tap; refresh 03_after_audio_tap; labels after_toggle; focus after_toggle
DLG=$(grep -c -E 'permission_allow_button|text="Allow"|text="While using the app"' /tmp/ui.xml); say "system permission dialog buttons present: $DLG"
if [ "$DLG" != "0" ]; then
  c=$(python3 - <<'PY'
import re,xml.etree.ElementTree as ET
r=ET.parse('/tmp/ui.xml').getroot()
for n in r.iter('node'):
    if 'permission_allow_button' in (n.get('resource-id') or '') or (n.get('text') or '')=='Allow':
        a=list(map(int,re.findall(r'\d+',n.get('bounds'))));print((a[0]+a[2])//2,(a[1]+a[3])//2);break
PY
); [ -n "$c" ] && { set -- $c; adb shell input tap $1 $2; say "tapped system Allow"; sleep 3; }
fi
GR=$(adb shell dumpsys package $PKG | grep -c 'READ_MEDIA_AUDIO: granted=true'); say "audio permission granted via in-app flow: $GR"
if [ "$GR" = "0" ]; then say "FALLBACK: pm grant (in-app dialog path NOT verified)"; adb shell pm grant $PKG android.permission.READ_MEDIA_AUDIO; fi
tap_exact "Next" s2; sleep 2; shot 04_reader; refresh 04_reader; labels reader_page; focus reader_page
tap_exact "Get Started" s3; sleep 4; shot 05_home; refresh 05_home; labels home
say "home shows smoke_tone: $(grep -c 'smoke_tone' /tmp/ui.xml)"
SEEN_PLAY=0; SEEN_PAUSE=0; SEEN_RESUME=0
if tap_exact "smoke_tone" s4; then
  sleep 4; shot 06_after_tap; say "session after tap: $(pbs)"; focus playing; r=$(pbs); case "$r" in *"/PLAYING/"*) SEEN_PLAY=1;; esac
  sleep 5; say "session +5s: $(pbs)"
  UI_PAUSE=0; UI_RESUME=0
  refresh 06b_home_playing; labels home_playing
  # Tapping a song opens the player screen directly (run 5 evidence), so tap the player's Pause, then Play.
  if tap_exact "Pause" 06e_player_pause; then sleep 2; r=$(pbs); say "after UI tap on player Pause: $r"; case "$r" in *"/PAUSED/"*) UI_PAUSE=1;; esac; shot 06f_ui_paused; refresh 06f_ui_paused; labels after_ui_pause
    if tap_exact "Play" 06g_player_play; then sleep 3; r=$(pbs); say "after UI tap on player Play: $r"; case "$r" in *"/PLAYING/"*) UI_RESUME=1;; esac; shot 06h_ui_resumed; fi
  fi
  say "UI pause observed: $UI_PAUSE; UI resume observed: $UI_RESUME (taps on in-app controls, emulator, synthetic tone)"
  adb shell input keyevent 127; sleep 2; r=$(pbs); say "after MEDIA_PAUSE key (not a UI tap): $r"; case "$r" in *"/PAUSED/"*) SEEN_PAUSE=1;; esac
  adb shell input keyevent 126; sleep 3; r=$(pbs); say "after MEDIA_PLAY key (not a UI tap): $r"; case "$r" in *"/PLAYING/"*) SEEN_RESUME=1;; esac
  shot 07_after_resume
fi
say "notifications from app: $(adb shell dumpsys notification --noredact 2>/dev/null | grep -c "pkg=$PKG") ; POST_NOTIFICATIONS: $(adb shell dumpsys package $PKG | grep -m1 'POST_NOTIFICATIONS')"
adb shell input keyevent 4; sleep 1
adb logcat -d > $OUT/logcat.txt
NF=$(grep -c 'FATAL EXCEPTION' $OUT/logcat.txt)
say "FATAL EXCEPTION count: $NF"
if [ "$NF" != "0" ]; then
  L1=$(grep -n -m1 'FATAL EXCEPTION' $OUT/logcat.txt | cut -d: -f1); say "--- 50 logcat lines before first FATAL (line $L1) ---"; sed -n "$((L1>50?L1-50:1)),$((L1-1))p" $OUT/logcat.txt | cut -c1-200 | tee -a $R
  say "--- FATAL block ---"; grep -A14 'FATAL EXCEPTION' $OUT/logcat.txt | cut -c1-220 | head -60 | tee -a $R
fi
say "(synthetic tone only; UI pause/resume result is in the lines above; media keys also used; no real music files; emulator only)"
if [ "$TONE_OK" = "1" ] && [ "$SEEN_PLAY" = "1" ] && [ "$SEEN_PAUSE" = "1" ] && [ "$SEEN_RESUME" = "1" ] && [ "$NF" = "0" ]; then say "SMOKE VERDICT: our MediaSession PLAYING, PAUSED, PLAYING observed; no FATAL (emulator evidence only)"; exit 0
else say "SMOKE VERDICT: INCOMPLETE play=$SEEN_PLAY pause=$SEEN_PAUSE resume=$SEEN_RESUME fatal=$NF - NOT a pass"; exit 1; fi
