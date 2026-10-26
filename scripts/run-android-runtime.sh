#!/usr/bin/env bash
set -euo pipefail
mkdir -p runtime-device-results

collect_diagnostics() {
  adb shell dumpsys gfxinfo com.byga.smartoverlay framestats > runtime-device-results/gfxinfo-final.txt || true
  adb shell uiautomator dump /sdcard/byga-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/byga-window.xml runtime-device-results/window.xml >/dev/null 2>&1 || true
  adb logcat -d -b crash > runtime-device-results/crash.txt || true
  adb logcat -d -v time -s AndroidRuntime TestRunner > runtime-device-results/app-logcat.txt || true
  if adb shell test -d /sdcard/Android/data/com.byga.smartoverlay/files; then
    adb pull /sdcard/Android/data/com.byga.smartoverlay/files/ runtime-device-results/device-files || true
  fi
}
trap collect_diagnostics EXIT
adb install -r runtime-apks/optimized/app-optimized.apk
adb install -r runtime-apks/androidTest/optimized/app-optimized-androidTest.apk
adb logcat -c
adb shell am instrument -w -r com.byga.smartoverlay.test/androidx.test.runner.AndroidJUnitRunner > runtime-device-results/instrumentation.txt
collect_diagnostics
trap - EXIT
python3 scripts/check-instrumentation-result.py runtime-device-results/instrumentation.txt
