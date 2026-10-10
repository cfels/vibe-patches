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

The Bash script uses `${GRADLE_USER_HOME:-$HOME/.gradle}` and passes additional arguments through to Gradle, for example `bash compile.sh --no-daemon`. Morphe's Maven dependencies require GitHub Packages credentials: user Gradle properties `gpr.user` / `gpr.key`, or `GITHUB_ACTOR` / `GITHUB_TOKEN`. Keep the token outside this repository.

The reference APK defaults to `apk/yanosik_antisplit.apk`. On Bash, set `YANOSIK_APK=/absolute/path/to/yanosik.apk` to choose another location. An explicitly supplied missing APK fails immediately. Without a reference APK, the Bash build succeeds but the Yanosik integration test skips; this is not reference-APK validation. Local APKs are intentionally excluded from Git.

## GitHub Actions setup

Push these changes to your own GitHub repository and enable Actions. `.github/workflows/release.yml` runs on pushes to **main** and can also be started from **Actions → Build and release Vibe Patch → Run workflow**, selecting main.

In **Settings → Secrets and variables → Actions**, add these repository secrets if the job's built-in token cannot read Morphe's external Maven registry:

| Secret | Value |
| --- | --- |
| `PACKAGES_USERNAME` | GitHub username owning the package-read token. |
| `PACKAGES_TOKEN` | Personal access token (classic) with `read:packages`, authorized for the package registry where required. |

Your local Gradle credentials are not transferred to GitHub Actions. Without these secrets, the workflow tries `github.actor` and the built-in `GITHUB_TOKEN`. See [GitHub's package authentication documentation](https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages#about-access-tokens).

The workflow grants its repository token `contents: write` to create the release/tag and commit generated version/metadata files, plus `packages: read` for dependency resolution. A protected main branch must permit the release commit for this automation to complete. The separate package token is used only for Gradle downloads.

## Releases

The existing conventional-commit versioning is retained. A main-branch commit such as `fix: repair navigation banners` creates a patch release; `feat: add a new patch` creates a minor release. Breaking changes create a major release. `bump:` and `perf:` also create patch releases. A manual run uses the same commit rules: it does not force another release when there are no release-worthy commits.

Each run builds and tests, then uploads one current-version bundle as a workflow artifact. For a new release, preparation updates `gradle.properties` to the calculated version, rebuilds, checks resource strings, regenerates the patch index/README and checks the collection structure. It verifies the archive name/version/Android DEX and the generated download URL before staging a single asset: **`vibe-patch-<release-version>.mpp`**. Semantic-release commits generated metadata and publishes a GitHub release with that asset. It does not publish Maven packages or call Morphe's website/notification services.

The ignored reference APK is absent on GitHub's runner, so CI's APK integration test skips. Run a local build with that APK before releasing patch behavior changes. The GitHub job itself must run in your repository to verify remote credentials, branch rules and publication permissions; local checks cannot exercise those.
