#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

# Use an explicitly configured JDK, or the system Java. macOS can locate 21.
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 21)"
    export JAVA_HOME
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        export ANDROID_HOME="$ANDROID_SDK_ROOT"
    elif [[ -d "$HOME/Android/Sdk" ]]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [[ -d "$HOME/Library/Android/sdk" ]]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [[ -n "${LOCALAPPDATA:-}" && -d "$LOCALAPPDATA/Android/Sdk" ]]; then
        export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
    fi
fi

args=(-g "${GRADLE_USER_HOME:-$HOME/.gradle}" --refresh-dependencies
      :patches:buildAndroid :patches:test)
reference_apk="${YANOSIK_APK:-$PWD/apk/yanosik_antisplit.apk}"
if [[ -f "$reference_apk" ]]; then
    args+=("-PyanosikApk=$reference_apk")
elif [[ -n "${YANOSIK_APK:-}" ]]; then
    printf 'Reference APK does not exist: %s\n' "$reference_apk" >&2
    exit 1
else
    printf 'No reference APK supplied; the Yanosik integration test will skip.\n'
fi

# Invoke through bash so fresh checkouts do not depend on executable file bits.
bash ./gradlew "${args[@]}" "$@"
version="$(awk -F= '/^version[[:space:]]*=/ {gsub(/[[:space:]\r]/,"",$2); print $2; exit}' gradle.properties)"
bundle="$PWD/patches/build/libs/vibe-patch-$version.mpp"
[[ -f "$bundle" ]] || { printf 'Expected bundle missing: %s\n' "$bundle" >&2; exit 1; }
printf 'Patch bundle: %s\n' "$bundle"
