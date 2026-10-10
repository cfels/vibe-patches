# Yanosik validation

## Friendlier patch descriptions — 10 October 2026

Rewrote all ten public patch descriptions, clone option descriptions, the installer option title, bundle description and README introduction in a more conversational voice. Patch names, compatibility, defaults and execution logic remain the same. Technical coverage/limitations remain documented, including the difference between local PRO settings and server-controlled perks.

The Android bundle, reference-APK JUnit test and generated patch index passed in 21 seconds (`.tools/friendly-descriptions-build.log`): one test executed, zero skipped/failures/errors. README was regenerated from the compiled metadata; all ten descriptions matched the generated table, and structural checks passed with ten patches and 131 XML files. The new wording is also in `patches/build/libs/vibe-patch-1.46.0.mpp` for Morphe Manager. No emulator reinstall was needed for this wording change.

## Single bundle, Bash build and GitHub releases — 10 October 2026

Gradle now names the archive `vibe-patch-<version>.mpp` directly. The old copy task was replaced by a `buildVibePatch` alias for `buildAndroid`. After verifying the new bundle's Vibe Patch name, exact version and Android DEX, the stale generated `patches-1.46.0.mpp` was removed from `patches/build/libs`. Source/Javadoc classifier archives are separate development artifacts and are excluded from release upload.

The Windows Gradle build passed in 19 seconds, `bash compile.sh --no-daemon` passed in 29 seconds, and `compile.bat` passed in 19 seconds with the dynamic version/path printed correctly. These runs used Java 21, SDK 36, the configured user Gradle home and the reference APK. Unchanged integration tests were up-to-date or restored from cache; no new runtime APK test is claimed for this build automation change. Logs are `.tools/single-bundle-build.log`, `.tools/compile-sh-build.log` and `.tools/compile-bat-single-bundle.log`.

The exact release task combination also passed:

```bash
bash compile.sh --no-daemon :patches:generatePatchesList :patches:checkStringResources
```

It generated ten patch entries and checked 7,103 resource strings successfully in 29 seconds (`.tools/release-tasks-build.log`). README regeneration and the migration validator passed with ten patches and 131 XML files.

Four isolated release staging tests passed. They check that the new Gradle version is written before building, the staged bytes match the bundle, mismatched manifest versions/missing DEX/stale download URLs fail, and a build failure removes any previously staged asset. On this restricted Windows host, `TEMP`/`TMP` were set to the ignored `.tools` directory. Bash syntax and the release workflow passed Git Bash `bash -n` and official `actionlint` (shellcheck/pyflakes integrations disabled). `.releaserc` now parses as valid JSON; its release asset and metadata URL use the Vibe filename. The pinned public changelog dependency uses HTTPS instead of SSH in the npm lockfile. Diff whitespace checks passed.

The workflow builds on main pushes/manual dispatch and uses the retained conventional-commit release rules. The exact release version is rebuilt before publication, and only one staged Android bundle is attached. Local checks did not run npm installation or publish a GitHub release; remote package access, branch rules and release permissions must be verified by the first workflow run in the user's repository. CI has no ignored reference APK, so its APK integration test skips. See [build and release setup](build-and-release.md).

## Local PRO, sidebar/purchase screens and Vibe Patch — 10 October 2026

The user explicitly requested overriding local PRO checks, hiding PRO purchase/sidebar sections, and hiding or locking the Application settings switch. The current bundle has five Yanosik patches and five universal patches. The new local patch changes the ACTIVE status converter, its startup flow seed, two UI state constructors and the dedicated switch setup. The promotion patch additionally suppresses the sidebar PremiumCard renderer and closes PRO/default/YBR activity targets after base initialization, preserving explicit DONATE/VOUCHER targets.

The local ACTIVE model has expiry 2100-01-01 UTC and willRenew=false. It does not create a RevenueCat purchase, receipt or remote entitlement. Server points/rankings/combo freezing/history retention are not claimed as working benefits. PRO status/activity suppression also removes the app's PRO subscription-management screen access.

Validation used the same reference APK and Java/SDK/Gradle home as the navigation follow-up:

```powershell
./gradlew.bat -g 'C:/Users/Moxiu/.gradle' --no-daemon :patches:buildAndroid :patches:test :patches:validateYanosik :patches:checkStringResources :patches:generatePatchesList '-PyanosikApk=C:/Users/Moxiu/Documents/morphe-patches/apk/yanosik_antisplit.apk'
```

Compilation, Android bundle, JUnit, reference-APK application, DEX regression and 7,103 resource strings passed. The first new-status attempt was caught by the serialized DEX assertion: the assembler required the wide literal's `L` suffix. After correcting it, sixteen intended methods passed with 147,206 unrelated outlines preserved. The index task then encountered a Windows mapped-file lock. Moving the exact old `patches-list.json` into `.tools/patches-list-before-local-pro.json` allowed the index-only retry to succeed in 10 seconds. No caches or unrelated processes were removed.

The switch follow-up stopped `AppPreferenceActivity.setupNoAdvSection` after its existing row/separator hiding, set the hidden control checked/disabled and returned before registering the YBR observer that had revealed it. The original binding/null handling remains. The full follow-up build passed in 32 seconds, including JUnit, actual APK patch application, serialized PRO screen branch tests, switch instruction/register checks and all prior negative tests: **17 intended methods, 147,205 unchanged method outlines**. One JUnit test, zero skipped/failures/errors. Logs: `.tools/local-pro-build.log`, `.tools/local-pro-index.log`, `.tools/local-pro-switch-build.log`.

The display name in `patches.about.name` is now **Vibe Patch**. `:patches:buildVibePatch` depends on the Android bundle and exports the named copy; `compile.bat` invokes it and the reference-APK tests. The final branding/export/index build passed in 15 seconds. `patches/build/libs/vibe-patch-1.46.0.mpp` matches the standard bundle's SHA-256: `f8f4e81b210c480ecc08d077a9378bb5c191680f84370504d11e8a29fb25b209`. README regeneration and structural validation passed with ten patches and 131 XML files. See [bundle icon instructions](vibe-patch-branding.md).

### Emulator installation and observations

Started the existing Android Studio AVD `Medium_Phone_API_37.0` on `emulator-5554`, Android API 37 with x86_64/arm64-v8a support. Windows window capture timed out even after reselection; screenshots were read through ADB instead.

Downloaded and installed official [Morphe Manager 1.35.0](https://github.com/MorpheApp/morphe-manager/releases/tag/v1.35.0). Its APK checksum matched the official asset digest: `64732f6a1c718123528fcb3e0b07b42edfcd28c03c131ae5d0b3d76539101ec7`. The downloaded file is ignored local `apk/morphe-manager-1.35.0.apk`.

Assembled a local test APK by rewriting the original ZIP with all four validated DEX outputs, removing obsolete ZIP signature entries. Manifest/resources bytes were checked unchanged. Aligned with SDK 36 zipalign `-P 16 -f 4`, signed using an ignored local test keystore and verified with apksigner (v3 signature). This is a test certificate, not the publisher's key. Installed successfully without uninstalling the app; the switch update preserved the signed-in account.

| Runtime check | Observed result |
| --- | --- |
| Initial launch | Reached login without a Yanosik crash. User handled sign-in/onboarding personally. |
| Signed-in dashboard and sidebar | Local PRO badge visible. Large standard-version/Enable PRO sidebar card absent; settings/wallet/about/FAQ/Y-KEY/logout/report remain visible. |
| Application settings | Startup-screen radio choices opened. User confirmed the final updated APK no longer shows Switch to PRO. The hidden control's checked/disabled operands are verified in serialized DEX. |
| Startup preference | Map selection accepted; after force-stop/relaunch and the update, map UI opened. A controlled restore of the user's preferred final value was not performed. |
| Floating-icon settings | Existing speed/speed-limit/road-number/radio controls displayed without a purchase card. Actual overlay permission/background behavior was not exercised. |
| Map/dashboard ads | No banner visible in the observed screens. Cached/refreshed banner delivery, active route navigation, voice output, Bluetooth and Android Auto were not comprehensively exercised. |
| PRO purchase activity | Serialized branch execution verifies PRO/YBR/default close and DONATE/VOUCHER continue. A dedicated Android UI test opening each target was not performed. |
| Other promotions | Dashboard Enable PRO service tile was observed; its separate data-driven renderer is not hidden. Existing action guards remain. No claim that every marketing entry is removed. |

The installed APK is `patches/build/yanosik-emulator/yanosik-26.9.0-local-pro-test.apk`. The final bundle and updated test APK are also in the emulator's `/sdcard/Download`. Old named navigation/local-PRO bundle copies may be stale; use the **vibe-patch-1.46.0.mpp** export. No release was published. More complete road/audio/backend QA remains outside these observations.

## Navigation-banner fix — 10 October 2026

The fragment-only patch missed the Compose banners in the user's navigation screenshot. The follow-up adds three exact targets (`La5b.a`, `Ln1a.b`, `Lnhc.j`) to **Hide banner ads**, retaining the legacy fragment targets. Their first serialized instruction is now `return-void`, before any Compose group, image or banner layout is created. The reference APK remains version 26.9.0 with SHA-256 `78e5b1064ae0f774f9eed2b09c46998128ee6a2906f0a8cde57b766e8b1e300d`.

Inspection used the tools supplied under `reverse/`:

```powershell
$javaExec = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/java.exe'
& $javaExec -jar reverse/apktool_3.0.3.jar d -s -p .tools/apktool-framework -o .tools/yanosik-apktool-3-fixed apk/yanosik_antisplit.apk
$env:JADX_CACHE_DIR = Join-Path (Get-Location) '.tools/jadx-cache'
$env:JADX_CONFIG_DIR = Join-Path (Get-Location) '.tools/jadx-config'
& $javaExec -Xmx3g -cp reverse/jadx-1.5.6/lib/jadx-1.5.6-all.jar jadx.cli.JadxCLI --single-class pl.neptis.features.adsbanners.adsservice.AdvertService --single-class-output .tools/AdvertService-1.5.6.java --no-res apk/yanosik_antisplit.apk
& $javaExec -Xmx3g -cp reverse/jadx-1.5.6/lib/jadx-1.5.6-all.jar jadx.cli.JadxCLI --single-class a5b --single-class-output .tools/a5b-1.5.6.java --no-res apk/yanosik_antisplit.apk
& $javaExec -Xmx3g -cp reverse/jadx-1.5.6/lib/jadx-1.5.6-all.jar jadx.cli.JadxCLI --single-class n1a --single-class-output .tools/n1a-1.5.6.java --no-res apk/yanosik_antisplit.apk
```

Apktool resource/manifest extraction passed. JADX wrote each class but reported 2, 5 and 5 decompilation errors respectively (exit 1). Relevant renderer bodies and event producers were recovered; exact signatures, access flags, fields, calls and register layouts were independently checked in the original DEX with dexlib2. The CLI main class must be explicit with this supplied combined JADX JAR; its default main opens the GUI. Local framework/cache paths avoid writing decompiled data outside the workspace. `/reverse/` is now ignored so downloaded tools are not included in the source migration.

The first integration attempt caught an access-flag mismatch: the new targets are public/static/**final**, and fingerprints require the complete flag set. After correcting those flags, the full command passed:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot'
$env:ANDROID_HOME = 'C:/Users/Moxiu/AppData/Local/Android/Sdk'
./gradlew.bat -g 'C:/Users/Moxiu/.gradle' --no-daemon :patches:buildAndroid :patches:test :patches:validateYanosik :patches:checkStringResources :patches:generatePatchesList '-PyanosikApk=C:/Users/Moxiu/Documents/morphe-patches/apk/yanosik_antisplit.apk'
```

**BUILD SUCCESSFUL in 25 seconds**, 80 actionable tasks (9 executed, 71 up-to-date). Production Kotlin, Android bundle generation, JUnit, resource strings and the complete framework-generated index passed. JUnit: one test, zero skipped, zero failures/errors. The real patcher applied all four Yanosik patches, serialized/reloaded their generated DEX, verified ten intended methods and preserved 147,212 unrelated method instruction/reference outlines. The new renderers retain their original register counts (23/22/25), and their remaining instruction/reference outlines match the original bodies. Existing promotion guard/negative checks also passed. This is not an ART verifier or byte-for-byte operand comparison.

The SDK XML/AGP compile-SDK compatibility and Gradle deprecation warnings did not prevent the build. No unrelated toolchain upgrade was made. The build log is local at `.tools/navigation-banner-build.log`; Gradle JUnit reports are under `patches/build/reports/tests/test`.

The new bundle is `patches/build/libs/patches-1.46.0.mpp` (1,236,755 bytes). Its identical local copy, `yanosik-26.9.0-navigation-banner-fix.mpp`, has SHA-256 `c9b302d00b61ccb6121a46de8f430a0ac40508980e49fc85b3e431ba6b047313`. Import this rebuilt bundle, select **Hide banner ads**, and patch the original reference APK again. No installable APK was produced. No ADB device was connected, so visual removal, empty-space collapse, recomposition and navigation/audio behavior still require phone testing.

The README generator, structural validator and diff check were run after complete index regeneration:

```powershell
& 'C:/Users/Moxiu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 .github/scripts/generate_patches_readme.py MorpheApp/morphe-patches main
& 'C:/Users/Moxiu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 scripts/validate-migration.py
git diff --check
```

## Historical follow-up — 9 October 2026

These earlier results concern the four Yanosik patches before the Compose-banner fix. The reference APK and downloaded tool binaries remain local ignored files. No installable APK or release bundle was produced on that date. The Gradle access limitations recorded below were resolved before the 10 October validation above.

## Compilation and reference-APK integration

Executed from the repository root in PowerShell:

```powershell
./scripts/validate-yanosik.ps1 -ReferenceApk ./apk/yanosik_antisplit.apk -MorpheRuntimeJar ./.tools/morphe-1.14.jar -KotlinHome ./.tools/kotlin/kotlinc -JavaExecutable 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/java.exe'
```

Passed, exit 0. The script compiled the actual production sources and integration validator using Kotlin 2.3.0 and Morphe Patcher 1.14.0, applied all four patches, serialized/reloaded their DEX output, verified seven intended modified methods and compared 147,215 unchanged method instruction/reference outlines. This comparison does not cover every encoded operand and is not an ART verifier or device test.

The serialized action guard passed inputs 0–100, -1 and Int.MAX_VALUE, returning handled only for 68/70. The serialized card guard passed all ten enum slots found in the original DEX: nine promotional slots returned before Compose, while subscription management and a null alert continued into the unchanged original renderer. The alert parameter was preserved. Voice dispatch and campaign resolution returned before their original bodies. The validator also passed missing-target and incompatible package/version rejection checks.

An intermediate launcher fingerprint attempt failed because its instruction filters were ordered differently from the original DEX. The filter order was corrected to constructor call followed by repository string; the final command above passed. No silent fallback was added.

## JUnit

Executed the following compilation and invocation (the source list includes actual production and test files):

```powershell
$javaExec = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin/java.exe'
$productionSources = @(Get-ChildItem patches/src/main/kotlin/app/morphe/patches/yanosik -Recurse -Filter '*.kt' | ForEach-Object FullName)
$testSources = $productionSources + @(Get-ChildItem patches/src/test/kotlin/app/morphe/patches/yanosik -Filter '*.kt' | ForEach-Object FullName)
$testClasspath = '.tools/morphe-1.14.jar;.tools/junit.jar;.tools/kotlin/kotlinc/lib/kotlin-test.jar;.tools/kotlin/kotlinc/lib/kotlin-test-junit5.jar'
& $javaExec -cp '.tools/kotlin/kotlinc/lib/*' org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -kotlin-home .tools/kotlin/kotlinc -Xcontext-parameters -classpath $testClasspath -jvm-target 17 -d .tools/yanosik-junit.jar @testSources
$env:TEMP = (Resolve-Path .tools/test-tmp).Path
$env:TMP = $env:TEMP
& $javaExec -Xmx3g '-DyanosikApk=apk/yanosik_antisplit.apk' -cp '.tools/yanosik-junit.jar;.tools/morphe-1.14.jar;.tools/junit.jar;.tools/kotlin/kotlinc/lib/kotlin-test.jar;.tools/kotlin/kotlinc/lib/kotlin-test-junit5.jar' org.junit.platform.console.ConsoleLauncher execute --select-class app.morphe.patches.yanosik.YanosikPatchTest --details summary --disable-ansi-colors
```

Passed: one test started, one successful, zero skipped, zero failed. It ran the full reference-APK integration validator. Temporary files were directed to the ignored workspace directory.

## Gradle

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot'
./gradlew.bat --offline --no-daemon -g .gradle-user :patches:compileKotlin
```

Failed, exit 1, before source compilation at `settings.gradle.kts:22`: plugin `app.morphe.patches:app.morphe.patches.gradle.plugin:1.3.4` was unavailable from the configured repositories/cache. The earlier online baseline also failed resolving this plugin. GitHub Packages access remains a prerequisite for the full build; the offline attempt does not test newly supplied credentials. No full Gradle build, Android extension compilation, Gradle test run or official complete-index generation is claimed.

## Metadata, resources and diff

The four Yanosik production patches were compiled separately, then discovered by the real framework `loadPatchesFromJar` API using the local `.tools/ExportMetadata.kt` helper. Discovery passed and exported all four names, descriptions, dependencies, defaults and compatibility declarations. `patches-list.json` was updated from that export, retaining five universal entries. It remains an unreleased migration snapshot.

```powershell
& 'C:/Users/Moxiu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 .github/scripts/generate_patches_readme.py MorpheApp/morphe-patches main
& 'C:/Users/Moxiu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 scripts/validate-migration.py
git diff --check
```

Passed: README regenerated for nine patches; structural validation confirmed four Yanosik plus five universal entries, parsed 131 resource XML files, checked duplicate resource names, source/index names and absence of removed collections and active package/import references. Diff whitespace checks passed.

## Device verification

Not run: APK assembly/signing, installation, launch, Compose layout/recomposition, warning versus advertising audio, speaker/Bluetooth/Android Auto behavior, notification/overlay behavior and end-to-end navigation tests. No configured Android SDK/adb/device session was available. The exact supported descriptors, guards and remaining seven-feature limitations are documented in [Yanosik coverage](yanosik.md). A verified publisher-signed reference and device QA are required before release.
