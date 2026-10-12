#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

if [[ "${VIBE_NIX_SHELL:-}" != 1 ]]; then
    command -v nix-shell >/dev/null || { printf 'nix-shell is required.\n' >&2; exit 1; }
    printf -v emulator_command '%q ' bash "$PWD/scripts/start-android-gpu.sh" "$@"
    exec nix-shell shell.nix --argstr runScript "$emulator_command"
fi

if (( $# > 1 )); then
    printf 'Usage: bash scripts/start-android-gpu.sh [AVD name]\n' >&2
    exit 1
fi
avd="${1:-Vibe_API_37}"
port="${ANDROID_EMULATOR_PORT:-5554}"
if [[ ! "$port" =~ ^[0-9]{4}$ ]]; then
    printf 'ANDROID_EMULATOR_PORT must be an even port from 5554 to 5682.\n' >&2
    exit 1
fi
port=$((10#$port))
if (( port < 5554 || port > 5682 || port % 2 != 0 )); then
    printf 'ANDROID_EMULATOR_PORT must be an even port from 5554 to 5682.\n' >&2
    exit 1
fi
serial="emulator-$port"
for tool in emulator adb scrcpy; do
    command -v "$tool" >/dev/null || { printf 'Missing Android tool: %s\n' "$tool" >&2; exit 1; }
done
adb start-server
state="$(adb -s "$serial" get-state 2>/dev/null || :)"
if [[ -n "$state" && "$state" != device ]]; then
    printf '%s is %s; wait for it to become ready before starting another emulator.\n' "$serial" "$state" >&2
    exit 1
fi
if [[ "$state" == device ]]; then
    running_avd="$(adb -s "$serial" shell getprop ro.boot.qemu.avd_name | tr -d '\r')"
    if [[ "$running_avd" != "$avd" ]]; then
        printf '%s already runs %s; choose another ANDROID_EMULATOR_PORT.\n' "$serial" "$running_avd" >&2
        exit 1
    fi
    graphics="$(adb -s "$serial" shell dumpsys SurfaceFlinger)"
    if [[ "$graphics" == *SwiftShader* || "$graphics" == *llvmpipe* || "$graphics" == *Lavapipe* ]]; then
        printf '%s uses software graphics. Shut it down before starting with host GPU rendering.\n' "$serial" >&2
        exit 1
    fi
else
    found=false
    while IFS= read -r available_avd; do
        if [[ "${available_avd%$'\r'}" == "$avd" ]]; then found=true; fi
    done < <(emulator -list-avds)
    if [[ "$found" != true ]]; then
        printf 'AVD not found: %s. Install the API 37 image and create the AVD first.\n' "$avd" >&2
        exit 1
    fi
    emulator -accel-check
    mkdir -p .tools
    log="$PWD/.tools/emulator-gpu-$port.log"
    nohup emulator -avd "$avd" -port "$port" -accel on -gpu host -no-window -no-snapshot-load \
        > "$log" 2>&1 < /dev/null &
    emulator_pid=$!
    deadline=$((SECONDS + 180))
    while [[ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || :)" != 1 ]]; do
        if ! kill -0 "$emulator_pid" 2>/dev/null || (( SECONDS >= deadline )); then
            printf 'Android did not finish booting. See %s\n' "$log" >&2
            tail -40 "$log" >&2
            exit 1
        fi
        sleep 1
    done
fi

if pgrep -f -- "^([^ ]*/)?scrcpy -s $serial --window-title Vibe Android - GPU( |$)" >/dev/null; then
    printf 'The GPU Android window for %s is already open.\n' "$serial"
    exit 0
fi

if [[ -n "${WAYLAND_DISPLAY:-}" ]]; then
    export SDL_VIDEODRIVER=wayland
fi
printf 'Opening %s with host GPU rendering. Closing this window leaves Android running.\n' "$avd"
exec scrcpy -s "$serial" --window-title 'Vibe Android - GPU' --max-size=1200 --no-audio
