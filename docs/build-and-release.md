# Build and release Vibe Patch

## Local builds

On this Windows machine, run `compile.bat` from the repository root. On Linux/macOS (or Git Bash), run:

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME="$HOME/Android/Sdk" # macOS commonly uses $HOME/Library/Android/sdk
bash compile.sh
```

Install Android SDK platform 36 and build-tools 36.0.0. Set `JAVA_HOME` to an installed Java 21 JDK; the Bash script otherwise uses system Java, or discovers Java 21 on macOS. `ANDROID_HOME` can be supplied explicitly; the script also checks `ANDROID_SDK_ROOT` and common SDK locations.

The scripts refresh dependencies, build the Android patch bundle and run tests. Both produce **`patches/build/libs/vibe-patch-<version>.mpp`** directly. The version comes from `gradle.properties`. Gradle's `buildVibePatch` task remains an alias for `buildAndroid` for existing commands. There is no second `patches-<version>.mpp` export.

The Bash script uses `${GRADLE_USER_HOME:-$HOME/.gradle}` and passes additional arguments through to Gradle, for example `bash compile.sh --no-daemon`. Morphe's Maven dependencies require GitHub Packages credentials: user Gradle properties `gpr.user` / `gpr.key`, or `GITHUB_ACTOR` / `GITHUB_TOKEN`. CI supplies the same dotted properties through `ORG_GRADLE_PROJECT_gpr.user` / `ORG_GRADLE_PROJECT_gpr.key` so both the settings plugin and its dependency repositories receive the credentials. The underscore aliases remain supported by this checkout’s settings as fallbacks. Keep the token outside this repository.

The reference APK defaults to `apk/yanosik_antisplit.apk`. On Bash, set `YANOSIK_APK=/absolute/path/to/yanosik.apk` to choose another location. An explicitly supplied missing APK fails immediately. Without a reference APK, the Bash build succeeds but the Yanosik integration test skips; this is not reference-APK validation. Local APKs are intentionally excluded from Git.

### NixOS

Enter the repository's development shell and run the existing build script:

```bash
nix-shell
bash compile.sh --no-daemon
python3 .github/scripts/prepare_release.py
```

The metadata command compiles JVM patch classes and runs the existing index and README generators; it does not build an Android bundle or run APK tests. `compile.sh` uses the existing Gradle home and credentials; missing GitHub Packages authentication still blocks the full build.

`shell.nix` uses your configured Nixpkgs, supplies Java 21 and the shared libraries needed by Google's Android SDK binaries, and uses your existing SDK at `${ANDROID_HOME:-$HOME/Android/Sdk}`. Install platform 36 and build-tools 36.0.0 in that SDK. Its `runScript` argument also supports non-interactive commands; a plain `nix-shell --run` is intercepted by the FHS shell hook.

To open the existing API 37 test phone using your GPU and KVM:

```bash
bash scripts/start-android-gpu.sh
```

The script defaults to `Vibe_API_37` on `emulator-5554`. It boots with `-gpu host -accel on`, avoids loading snapshots from a different graphics renderer, and opens a scrcpy window using Wayland when available. Existing app data/sign-in are preserved. Repeating the command reuses the emulator and its existing window; closing the window leaves Android running. No APK is installed by this script. Logs go to `.tools/emulator-gpu-5554.log`. The window has audio forwarding disabled, so it is intended for UI checks.

Supply an AVD name as the first argument or set `ANDROID_EMULATOR_PORT` to another even port from 5554 through 5682. The script refuses to reuse a different AVD or a software-rendered emulator on the selected port. It does not create or wipe AVDs. If the default AVD is missing, install `system-images;android-37.0;google_apis;x86_64` in the SDK and create it inside `nix-shell`:

```bash
avdmanager create avd -n Vibe_API_37 -k 'system-images;android-37.0;google_apis;x86_64' -d pixel_7
```

Check hardware acceleration with `emulator -accel-check`. No NixOS system rebuild is needed when `/dev/kvm` and the GPU render devices are already accessible.

## GitHub Actions setup

Push these changes to GitHub and enable Actions. **Build and release Vibe Patch** runs only when manually triggered on main. It bumps the version, regenerates metadata, commits and pushes it, then builds/tests the Android bundle and publishes the matching GitHub release asset. **Check generated metadata** runs on ordinary pushes and pull requests, regenerates the files and fails on a diff. It never bumps, commits, pushes or builds an Android bundle. Both jobs need Java 21 and Morphe Maven access. The release job installs SDK platform 36 and build-tools 36.0.0. CI has no reference APK, so its Yanosik integration test skips.

In **Settings → Secrets and variables → Actions**, add these repository secrets if the job's built-in token cannot read Morphe's external Maven registry:

| Secret | Value |
| --- | --- |
| `PACKAGES_USERNAME` | GitHub username owning the package-read token. |
| `PACKAGES_TOKEN` | Personal access token (classic) with `read:packages`, authorized for the package registry where required. |

Your local Gradle credentials are not transferred to GitHub Actions. Without these secrets, the workflow tries `github.actor` and the built-in `GITHUB_TOKEN`. See [GitHub's package authentication documentation](https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages#about-access-tokens).

The workflow grants its repository token `contents: write` to commit/push generated files and publish the GitHub release, plus `packages: read` for dependency resolution. A protected main branch must permit the release commit for this automation to complete. The separate package token is used only for Gradle downloads.

## Adding the collection to Morphe

Use [Add Vibe Patch to Morphe](https://morphe.software/add-source?github=cfels/vibe-patches&name=Vibe%20Patch), or add `https://github.com/cfels/vibe-patches` as a remote source. Morphe resolves the repository to `https://raw.githubusercontent.com/cfels/vibe-patches/main/patches-bundle.json` and downloads the `.mpp` at `download_url`.

The JSON version, Gradle version, compiled bundle manifest and asset filename must agree. Publish the matching asset when publishing the versioned metadata; a JSON file pointing at an unpublished release returns a failed download. A local import doesn't auto-update. Updating the source also doesn't replace an installed patched APK.

## Releases

`gradle.properties` is the only version source. The JSON files and README copy that version during generation; they are not independent version settings. Commit wording has no effect on versioning.

For the next release:

1. Push the changes to main. Configure the package-read secrets above if needed and allow the Actions bot to push to main under your branch rules.
2. Open **Actions → Build and release Vibe Patch → Run workflow**, select **main**, and choose **patch** (default), **minor** or **major**. For example, a patch bump takes `1.2.2` to `1.2.3`; minor/major bumps reset the lower components.
3. The job checks out the latest main and runs `python3 .github/scripts/prepare_release.py --bump <part>` to regenerate metadata from compiled JVM patches. After the automation checks, it commits and pushes only `gradle.properties`, `patches-list.json`, `patches-bundle.json` and `README.md`. The release workflow has no push trigger, and its commit uses the repository's `GITHUB_TOKEN` plus `[skip ci]`, so it cannot release itself again. A failed metadata check or push stops before building the Android bundle.
4. The job installs SDK components and runs `python3 .github/scripts/prepare_release.py --build` at that prepared commit, without another version bump. It uses the existing `compile.sh` build/test path plus `generatePatchesList` and `checkStringResources`, verifies the bundle name/version and Android DEX, and checks that regenerated metadata matches the committed files. It saves the exact `vibe-patch-<version>.mpp` as an Actions artifact, then attaches it to a GitHub release tagged `v<version>` at the prepared commit, with generated release notes. Source/Javadoc bundles are excluded. No separate build or upload is required after a successful run.

Every manual run that pushes metadata bumps the version, even if its subsequent build fails. Rerunning after a successful push starts from that new version. The workflow serializes runs and checks out the latest main so queued runs use the latest committed version. A concurrent push can make the metadata push fail; it never force-pushes.

If the build fails after the metadata push, fix the failure and build that prepared version with `--build` without another bump to finish its release. If publication fails after artifact upload, use the Actions artifact to finish that version's release at the prepared commit. Until its matching release asset exists, the metadata URL cannot serve downloads. Rerunning the workflow bumps again.

Locally, use the same path:

```bash
python3 .github/scripts/prepare_release.py          # regenerate without bumping
python3 .github/scripts/prepare_release.py --check  # regenerate and fail on tracked differences
python3 .github/scripts/prepare_release.py --bump patch
python3 .github/scripts/prepare_release.py --bump patch --build
```

On NixOS, run these inside `nix-shell`. Without `--build`, regeneration stays JVM-only. `--build` removes any existing bundle for the selected version before compilation so a stale file cannot pass as a fresh build; it does not commit, push or publish locally. `--check` cannot be combined with `--build` and leaves regenerated files available for review; commit those yourself when making normal patch metadata edits. Release preparation updates the Morphe URL and description from the bundle's existing `patches.about` source/description information. The timestamp changes only when the generated version changes, so repeated checks are stable. Historical validation documents and the existing changelog are not generated release metadata and are not rewritten.

Run `python3 scripts/test_release_build.py` and actionlint after changing the automation. Fork PRs cannot access repository package-read secrets; they require registry access through their built-in token or a maintainer run on a trusted branch. No credentials are exposed through a privileged PR workflow.
