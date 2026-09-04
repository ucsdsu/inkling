#!/usr/bin/env bash
# Start an Android emulator shaped like the BOOX Go Color 7 (7", 1264x1680).
# Usage: tools/emulator.sh [create|start|install|all]
#   create  - make the AVD once
#   start   - boot it (window opens on the Mac; click it like a tablet)
#   install - build the debug APK and install it on the running emulator
#   all     - create if missing, start, wait for boot, install
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

AVD="boox7"
IMAGE="system-images;android-34;google_apis_playstore;arm64-v8a"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

create() {
  if avdmanager list avd 2>/dev/null | grep -q "Name: $AVD"; then
    echo "AVD $AVD exists"
    return
  fi
  echo no | avdmanager create avd -n "$AVD" -k "$IMAGE" -d "pixel_tablet" --force
  local cfg="$HOME/.android/avd/$AVD.avd/config.ini"
  # Go Color 7 panel. Density 320 is the nearest standard bucket to 300 ppi.
  sed -i '' -e '/^hw.lcd.width/d' -e '/^hw.lcd.height/d' -e '/^hw.lcd.density/d' -e '/^hw.keyboard/d' -e '/^hw.audioInput/d' "$cfg"
  cat >> "$cfg" <<EOF
hw.lcd.width=1264
hw.lcd.height=1680
hw.lcd.density=320
hw.keyboard=yes
hw.audioInput=yes
hw.ramSize=4096
disk.dataPartition.size=6G
EOF
  echo "created $AVD"
}

start() {
  if adb devices | grep -q emulator; then
    echo "emulator already running"
    return
  fi
  nohup emulator -avd "$AVD" -no-snapshot-load -gpu auto -no-boot-anim -scale 0.45 \
    >"$HOME/.android/$AVD.log" 2>&1 &
  echo "booting $AVD (log: ~/.android/$AVD.log)"
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  echo "booted"
}

install() {
  (cd "$ROOT/android" && ./gradlew -q :app:assembleDebug)
  adb install -r "$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
  adb shell am start -n dev.inkling/.MainActivity
}

case "${1:-all}" in
  create) create ;;
  start) start ;;
  install) install ;;
  all) create; start; install ;;
  *) echo "usage: $0 [create|start|install|all]"; exit 1 ;;
esac
