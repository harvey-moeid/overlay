#!/usr/bin/env bash
set -euo pipefail
mkdir -p runtime-device-results

collect_diagnostics() {
  adb logcat -d -v time > runtime-device-results/device-logcat.txt || true
  adb shell dumpsys activity activities > runtime-device-results/activities.txt || true
  adb exec-out screencap -p > runtime-device-results/final-screen.png || true
  adb shell dumpsys gfxinfo com.byga.smartoverlay framestats > runtime-device-results/gfxinfo-final.txt || true
  adb shell uiautomator dump /sdcard/byga-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/byga-window.xml runtime-device-results/window.xml >/dev/null 2>&1 || true
  adb logcat -d -b crash > runtime-device-results/crash.txt || true
  adb logcat -d -v time -s AndroidRuntime TestRunner > runtime-device-results/app-logcat.txt || true
  if adb shell test -d /sdcard/Android/data/com.byga.smartoverlay/files; then
    adb pull /sdcard/Android/data/com.byga.smartoverlay/files/ runtime-device-results/device-files || true
  fi
  python3 - <<'PY'
from pathlib import Path
import json, re
root=Path("runtime-device-results")
for p in root.rglob("performance-smoke.json"):
    print("PERFORMANCE_REPORT", p.read_text())
for p in [root/"os-ready.xml",root/"window.xml"]:
    if p.exists():
        texts=re.findall(r'text="([^"]*)"',p.read_text(errors="replace"))
        print("UI_HEALTH",p.name,[s for s in texts if "responding" in s.lower()])
log=root/"device-logcat.txt"
if log.exists():
    lines=log.read_text(errors="replace").splitlines()
    selected=[s for s in lines if ("ANR in" in s or "FATAL EXCEPTION" in s or "Untrusted touch" in s or
        ("ActivityTaskManager" in s and "com.byga.smartoverlay" in s) or
        ("Killing" in s and ("com.byga.smartoverlay" in s or "com.android.systemui" in s)))]
    print("DEVICE_DIAGNOSTICS\n"+"\n".join(selected[-50:]))
for p in root.rglob("bubble-window-state.txt"):
    lines=p.read_text(errors="replace").splitlines()
    print("BUBBLE_WINDOW", "\n".join(s for s in lines if "com.byga" in s or "Exclusion" in s or "mCurrentFocus" in s))
PY
}
trap collect_diagnostics EXIT
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_HOME
for attempt in 1 2 3 4 5 6; do
  timeout 8s adb shell uiautomator dump /sdcard/byga-os-ready.xml || true
  if adb shell test -s /sdcard/byga-os-ready.xml; then break; fi
  echo "Waiting for emulator UI root ($attempt/6)"
  sleep 2
done
adb pull /sdcard/byga-os-ready.xml runtime-device-results/os-ready.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse("runtime-device-results/os-ready.xml").getroot()
text=" ".join(node.get("text","") for node in root.iter()).casefold()
if "isn't responding" in text or "not responding" in text:
    raise SystemExit("Emulator OS is unresponsive before APK installation; runtime tests have not started")
PY
sha256sum runtime-apks/optimized/app-optimized.apk
adb install -r runtime-apks/optimized/app-optimized.apk
adb install -r runtime-apks/androidTest/optimized/app-optimized-androidTest.apk
adb logcat -c
adb shell am instrument -w -r com.byga.smartoverlay.test/androidx.test.runner.AndroidJUnitRunner > runtime-device-results/instrumentation.txt
collect_diagnostics
trap - EXIT
python3 scripts/check-instrumentation-result.py runtime-device-results/instrumentation.txt
