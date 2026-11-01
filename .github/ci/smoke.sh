#!/usr/bin/env bash
# One-shot emulator smoke run. Reports what happened; never edits the app. No user data (synthetic tone file only).
set -u
OUT=smoke-out; mkdir -p $OUT; R=$OUT/report.txt; : > $R
PKG=com.abhi.musicapp
say(){ echo "$*" | tee -a $R; }
adb wait-for-device; adb shell input keyevent 82
say "device sdk: $(adb shell getprop ro.build.version.sdk)"
# synthetic 6s stereo tone
python3 - <<'PY'
import wave,struct,math
w=wave.open('/tmp/smoke_tone.wav','wb'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(22050)
w.writeframes(b''.join(struct.pack('<hh',int(8000*math.sin(2*math.pi*440*i/22050)),int(8000*math.sin(2*math.pi*440*i/22050))) for i in range(22050*6)))
w.close()
PY
for i in $(seq 1 30); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && adb shell ls /sdcard >/dev/null 2>&1 && break; sleep 2; done
adb shell mkdir -p /sdcard/Music
TONE_OK=0
if adb push /tmp/smoke_tone.wav /sdcard/Music/smoke_tone.wav >$OUT/push1.txt 2>&1; then TONE_OK=1; say "tone push to /sdcard/Music: ok"
else say "tone push to /sdcard/Music FAILED: $(cat $OUT/push1.txt | head -2)"
  adb push /tmp/smoke_tone.wav /data/local/tmp/smoke_tone.wav >/dev/null 2>&1
  if adb shell cp /data/local/tmp/smoke_tone.wav /sdcard/Music/smoke_tone.wav >$OUT/push2.txt 2>&1; then TONE_OK=1; say "tone copy via /data/local/tmp: ok"; else say "tone copy via /data/local/tmp FAILED: $(head -2 $OUT/push2.txt)"; fi
fi
say "tone listing: $(adb shell ls -l /sdcard/Music 2>&1 | head -3 | tr '\n' ';')"
adb shell content call --uri content://media/ --method scan_volume --arg external_primary >/dev/null 2>&1
adb shell content call --uri content://media/ --method scan_file --arg /sdcard/Music/smoke_tone.wav >/dev/null 2>&1
sleep 3
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null && say "install: ok" || { say "install: FAILED"; exit 0; }
adb logcat -c
shot(){ adb exec-out screencap -p > $OUT/$1.png; }
dump(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml $OUT/$1.xml >/dev/null 2>&1; }
# prints "x y" center of first node whose text contains $1 (from latest dump), empty if none
center(){ python3 - "$1" <<'PY'
import sys,re,xml.etree.ElementTree as ET
t=sys.argv[1]
try: root=ET.parse('/tmp/ui.xml').getroot()
except Exception: sys.exit()
for n in root.iter('node'):
    if t in (n.get('text') or '') or t in (n.get('content-desc') or ''):
        a=list(map(int,re.findall(r'\d+',n.get('bounds'))));print((a[0]+a[2])//2,(a[1]+a[3])//2,a[2],a[3]);break
PY
}
refresh(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; cp /tmp/ui.xml $OUT/$1.xml 2>/dev/null; }
labels(){ say "UI[$1] pkg+texts: $(python3 - <<'PY2'
import xml.etree.ElementTree as ET
try:
    r=ET.parse('/tmp/ui.xml').getroot(); o=[]
    for n in r.iter('node'):
        t=(n.get('text') or n.get('content-desc') or '').strip()
        if t: o.append(t[:30])
    print(n.get('package') if False else '', ' | '.join(o[:25]))
except Exception as e: print('dump unreadable')
PY2
)"; }
tap_text(){ refresh "$2"; c=$(center "$1"); if [ -n "$c" ]; then set -- $c; adb shell input tap $1 $2; say "tap '$1': ok"; return 0; fi; say "tap '$1': NOT FOUND"; return 1; }
W=$(adb shell wm size | grep -o '[0-9]*x[0-9]*$' | cut -dx -f1)

adb shell am start -n $PKG/.MainActivity >/dev/null; sleep 6; shot 01_launch; refresh 01_launch
say "launch ui has 'Next': $(grep -c 'text="Next"' /tmp/ui.xml)"
tap_text "Next" s1; sleep 2; tap_text "Next" s2; sleep 2; shot 02_permissions; refresh 02_permissions; labels perms
# permission toggle is the box at the right of the 'Music and audio' row
c=$(center "Music and audio"); if [ -n "$c" ]; then set -- $c; adb shell input tap $((W-70)) $2; say "tapped audio toggle at y=$2"; fi
sleep 3; shot 03_after_audio_tap; refresh 03_after_audio_tap; labels after_toggle; say "foreground: $(adb shell dumpsys window | grep -m1 mCurrentFocus)"
say "system dialog present (Allow button): $(grep -c -i 'text="Allow"' /tmp/ui.xml)"
if grep -q -i 'text="Allow"' /tmp/ui.xml; then tap_text "Allow" 03b; sleep 2; fi
say "audio permission granted after in-app request: $(adb shell dumpsys package $PKG | grep -c 'READ_MEDIA_AUDIO: granted=true')"
if [ "$(adb shell dumpsys package $PKG | grep -c 'READ_MEDIA_AUDIO: granted=true')" = "0" ]; then say "FALLBACK: granting with pm grant (in-app dialog path NOT verified)"; adb shell pm grant $PKG android.permission.READ_MEDIA_AUDIO; fi
tap_text "Get Started" s3; sleep 4; shot 04_home; refresh 04_home; labels home
say "home shows smoke_tone: $(grep -c 'smoke_tone' /tmp/ui.xml)"
if tap_text "smoke_tone" s4; then sleep 5; shot 05_after_tap; refresh 05_after_tap
  say "media_session playing (state=3): $(adb shell dumpsys media_session | grep -c 'state=3')"
  say "dumpsys playback excerpt: $(adb shell dumpsys media_session | grep -m3 'PlaybackState')"
  tap_text "Pause" s5; sleep 2; say "after Pause tap, state=2 (paused) count: $(adb shell dumpsys media_session | grep -c 'state=2')"; shot 06_after_pause
  tap_text "Play" s6; sleep 2; say "after Play tap, state=3 count: $(adb shell dumpsys media_session | grep -c 'state=3')"
fi
adb shell input keyevent 4; sleep 1
adb logcat -d > $OUT/logcat.txt
NF=$(grep -c 'FATAL EXCEPTION' $OUT/logcat.txt)
say "FATAL EXCEPTION count: $NF"
if [ "$NF" != "0" ]; then say "--- FATAL block(s) (logcat, trimmed; no user data) ---"; grep -A14 'FATAL EXCEPTION' $OUT/logcat.txt | cut -c1-220 | head -60 | tee -a $R; fi
say "app process pids in logcat: $(grep -c "$PKG" $OUT/logcat.txt) lines mention $PKG"
say "--- permission-related logcat (first 8) ---"; grep -i -m8 'READ_MEDIA_AUDIO\|GrantPermissions\|permission.*audio' $OUT/logcat.txt | cut -c1-200 | tee -a $R
PLAYED=$(adb shell dumpsys media_session | grep -c 'state=3'); [ -z "${PLAYED:-}" ] && PLAYED=0
say "(synthetic tone only; notification and navigation to other screens not exercised)"
if [ "$TONE_OK" = "1" ] && [ "${PLAYED:-0}" -gt 0 ]; then say "SMOKE VERDICT: playing state observed in media_session (compiled+emulator evidence; not a physical-device test)"; exit 0
else say "SMOKE VERDICT: INCOMPLETE - checks did not all run or playback not observed; this is NOT a pass"; exit 1; fi
