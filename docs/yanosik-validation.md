# Yanosik validation

## Metadata dependency ordering — 12 October 2026

The push/PR metadata check failed because CI's library exporter already emitted `dependencies` before `compatiblePackages`, while the local exporter appended it after `options`. Gson replaces an existing member without moving it. The wrapper now removes that member before appending the compiled dependency values, giving both exporters the same order. The strict generated-diff check remains unchanged.

Reproduced the reported order-only diff in an isolated JVM fixture: JSON values matched but bytes differed before the fix. After the fix, both exporter layouts matched the checked-in index byte-for-byte across two runs each. All seven automation tests and whitespace checks passed. No version bump, metadata-value change, APK build/test or patch behavior change was needed.

## Manual version and metadata automation — 12 October 2026

Release preparation now runs only through manual dispatch on main, with patch/minor/major selection. `gradle.properties` remains the canonical version. The existing preparation script supports regeneration, `--bump` and `--check`; it calls the existing library exporter and README generator, derives the Morphe URL/description from `patches.about`, and preserves timestamps for unchanged versions. Ordinary push/PR CI fails on generated diffs without committing. The manual workflow commits only the version, index, Morphe JSON and README through the repository token with `[skip ci]`. Semantic-release, its release-only npm dependencies and the npm Dependabot entry were removed. Matching bundle build/upload remains separate.

The metadata Gradle task now archives only JVM classes and runs the existing exporter through a small Java source launcher. It does not depend on the full build, Android resources or APK tests. An isolated Gradle 9.8 JVM fixture with an Android-build failure sentinel passed; the executed tasks were JVM compilation, metadata archive/export and credential assertions. Its output matched all eleven repository entries. The fixture caught a classpath-order issue: repository classes now precede library classes so identical class names cannot export upstream metadata. Dotted `ORG_GRADLE_PROJECT_gpr.user` / `ORG_GRADLE_PROJECT_gpr.key` properties were verified with fake credentials, covering the plugin's dependency repository lookup as well as settings. Logs: `.tools/metadata-task-fixture.log`.

Seven isolated automation tests passed, covering version bumps/invalid versions, regeneration of JSON/README, stable repeat generation, failure handling, stale index rejection and CI failures for each tracked generated file without committing. Both changed workflows passed actionlint 1.7.12; structural migration and whitespace checks passed. Generation from existing compiled JVM patches was run twice and produced identical files. Version remains 1.2.2 until the next manual bump.

The actual repository Gradle dry run first hit sandbox networking restrictions; outside that restriction it stopped at unresolved `app.morphe.patches:1.3.4` in the offline cache, consistent with this host's missing GitHub Packages access (`.tools/metadata-gradle-dry-run.log`). The isolated check is not a successful authenticated repository build or remote workflow run. No APK was rebuilt, tested or investigated, no patch behavior changed, and no workflow, commit, push, tag or publication was performed locally.

## Public wording, Morphe source and 1.2.2 — 12 October 2026

Read the user's public homepage, profile, contact, bookmarks, gallery and support pages at [moxiu.vacpro.fyi](https://moxiu.vacpro.fyi), plus recent repository commits, before editing. Public copy uses short, plain sentences and occasional small asides; it avoids copying the commit messages' profanity, spelling mistakes or uncertainty. No separate style-guide file was added.

All eleven public patch names/descriptions were rewritten in Kotlin. Clone option descriptions were shortened; keys, defaults, values, dependencies and compatibility stayed the same. Production sources compiled with the existing Java 21/Kotlin toolchain and public Morphe library compile classpath (`.tools/voice-all-source-compile.log`). The existing generator exported all eleven entries from `.tools/voice-all-patches.jar`, and README was regenerated. Source normalization confirmed every production Kotlin difference is limited to metadata wording. Generated metadata comparison confirmed unchanged defaults, option keys/values, dependencies and package/version targets. No new APK or phone QA was needed for wording-only changes.

README now uses the collection's own icon/branding, links to the supported Morphe add-source route and explains remote/local updates. License/copyright attribution remains. Bundle identity points to `https://github.com/cfels/vibe-patches`, with Moxiu as author and the user's site/contact page. Gradle, the generated patch index, README and bundle JSON agree on **1.2.2**; the prepared asset URL is `https://github.com/cfels/vibe-patches/releases/download/v1.2.2/vibe-patch-1.2.2.mpp`.

Inspection of Morphe Manager's public source confirms repository sources read `main/patches-bundle.json` directly and then download its `download_url`. The currently published 1.2.1 JSON matches an existing 1.2.1 release asset, even though the release is marked prerelease. The prepared 1.2.2 metadata is not a claim that a new release is live. Do not publish versioned JSON without its matching bundle.

CI supplies `ORG_GRADLE_PROJECT_gpr_user` / `ORG_GRADLE_PROJECT_gpr_key`, but settings previously read only dotted `gpr.user` / `gpr.key` and the GitHub fallback. The underscore aliases are now supported after the dotted properties and before GitHub fallback. Three isolated real Gradle 9.8 checks using fake values passed: CI aliases, dotted-property precedence and environment fallback (`.tools/ci-credential-check.log`, `.tools/dotted-credential-check.log`, `.tools/fallback-credential-check.log`). No real credentials were printed or saved to source.

The old Nix build helper referenced in documentation is absent from this checkout. README/build notes now use the existing `nix-shell` and `compile.sh`. The first attempted helper command failed with exit 127; the corrected full-build command was `nix-shell --argstr runScript 'bash compile.sh --no-daemon :patches:generatePatchesList :patches:checkStringResources'`. It reached Gradle but still failed at settings plugin `app.morphe.patches:1.3.4` resolution because this host lacks package credentials (`.tools/voice-nix-build.log`). No full Gradle-built 1.2.2 bundle was delivered or published. This limitation is separate from successful source compilation/index export.

All six release staging checks passed, including the new repository/JSON/index version and Morphe add-source-link consistency check. Both workflows passed actionlint 1.7.12 (shellcheck/pyflakes integrations disabled). Migration checks and `git diff --check` passed. The user's existing `ubuntu-24.04` workflow edit was preserved. Patching behavior, extensions, SDK/patcher versions and APK compatibility were not changed.

## Opened search/maps dark mode and Nix scripts — 12 October 2026

The user reported that Vibe dark mode worked on the emulator's home screen but opened search/map screens were light on their phone, and requested investigation plus Nix build/GPU emulator scripts. This authorizes the targeted checks below; it does not reopen the earlier broad phone QA campaign.

The home/login palette hook deliberately excludes MapActivity. The separate native map settings factory `Lb6i;->a()Lw5i;` reads `MAP_AUTO_DAY_NIGHT_MODE` and `MAP_DAY_MODE`; its mode flows drive both the map style and MapActivity's night theme, including search. Vibe's saved toggle previously never reached this producer. `MapTheme.kt` now overrides only its mode to the existing `Ldfb;->NIGHT` while Vibe dark mode is enabled, preserving traffic/3D flags and native preferences. Disabled Vibe mode retains the original Day/Night/Auto decision. The map's watched keys include the local toggle, and the original listener registers/unregisters with Vibe preferences alongside its native registration/cleanup. Missing fingerprints and changed register/layout anchors fail explicitly.

The home palette conversion remains restricted to dashboard/login. An intermediate extension of that conversion to MapActivity made native white control backgrounds conflict with converted foregrounds during a Day-to-dark transition; it was removed. Final map/search colors come from the original native night palette, including readable radio/settings controls.

Validation used Java 21, the existing Kotlin compiler and Morphe Patcher 1.14.0 against `apk/yanosik_antisplit.apk` (unchanged 26.9.0 reference identity/SHA-256). Production Kotlin sources compiled. The extension compiled with SDK 36 and D8; existing color checks passed. Real APK application, serialized DEX validation and negative tests passed: **77 intended original methods, 147,145 unrelated instruction/reference outlines**, and **40 extension methods/eight classes**. The new validator executes all 32 dark/auto/day/traffic/building combinations against serialized instructions and checks paired listener operands/cleanup. Logs: `.tools/map-theme-compile.log`, `.tools/map-theme-all-source-compile.log`, `.tools/map-theme-validation-verified.log`. This is not a byte-for-byte comparison of all unrelated operands.

Initial compilation caught an escaped dollar sign used in a Kotlin raw smali string; the literal dollar expression fixed it. The first serialized outline check caught automatic switch-payload padding in the merged cleanup method. Keeping the injected register/unregister sequences at an even code-unit length preserves the original payload alignment; the final actual DEX checks passed. No failure was ignored.

The signed test APK is `patches/build/yanosik-emulator/yanosik-26.9.0-map-theme-test.apk`, SHA-256 `fd51fdfc66295434486fcde3571c59e15c3f7f44250d5df6fc28c393a30f39f9`. APK v3 verification passed using the existing ignored local key. `adb -s emulator-5554 install --no-incremental -r` succeeded without uninstalling or losing sign-in. With native Day selected, Vibe off showed light map/search; Vibe on showed dark map/search. Final screenshots `.tools/map-theme-verified-map.png` and `.tools/map-theme-verified-search.png` confirm readable controls, text and preserved native map content. The emulator retains native Day beneath the enabled Vibe toggle for comparison. Physical-phone verification is pending; road/audio/backend coverage has not expanded.

`bash scripts/build-nix.sh` enters the repository's FHS environment, runs the existing build/tests plus metadata/resource tasks, then regenerates README and validates the collection. Additional arguments survive quoting. README repository links come from `GITHUB_REPOSITORY` or the existing bundle download URL. `shell.nix` accepts `runScript` because its FHS shell hook intercepts ordinary `nix-shell --run` commands. The actual new script reached Gradle but failed at Morphe settings-plugin resolution, consistent with the host's missing GitHub Packages authentication. Log: `.tools/map-theme-gradle-build.log`. No Gradle-built bundle or release is claimed. The existing generator separately regenerated all eleven entries from compiled production patches and README preserved this fork's release links.

`bash scripts/start-android-gpu.sh` launches `Vibe_API_37` with KVM and `-gpu host`, cold-boots without wiping data, and opens the Wayland scrcpy window. The running emulator's logs identify the Radeon RX 6800 XT for Vulkan/OpenGL. Reuse was checked on the real emulator/window. Isolated mock checks passed for quoted arguments, build failure stopping exports, window reuse, wrong AVD/software renderer/offline/invalid-port guards and host-GPU/KVM launch flags (`.tools/test-nix-scripts.py`). Shell syntax, Nix formatting, migration validation and whitespace checks passed. Closing scrcpy leaves the emulator running. UI tests do not establish audio coverage.

## NixOS, Material 3 settings and 1.2.1 — 12 October 2026

The user confirmed all patches work in their testing, approved the dark theme and Material 3 switch, and explicitly requested ending further testing. The final supplied screenshot also confirms the Appearance subtitle is gone. No further phone QA is required for these completed requests unless the user asks or a later behavior change needs a targeted check. This is the user's confirmation, not a new claim of comprehensive road/audio/backend coverage.

The workspace now runs on NixOS. `shell.nix` supplies Java 21 and the Android SDK binary libraries. SDK platform 36 and the API 37 Google APIs image were installed. The `Vibe_API_37` AVD booted with KVM on `emulator-5554`; a native Wayland scrcpy window provides the phone UI because the emulator's Qt window couldn't connect to the desktop X display. The user handled sign-in.

Dark login styling preserves the Google button's original white surface, gray label and colored logo. Login hints and outlines have explicit readable colors. Vibe settings uses a native Android switch with Material 3 track/thumb styling; the APK only supplies the older Material 2 SwitchMaterial. The new control keeps native switch input, animation and accessibility. The subtitle was removed, and dark system-bar appearance uses the current Android API after layout.

Before the user ended QA, standalone compilation and real reference-APK application passed with Morphe Patcher 1.14.0: 73 intended methods, 147,149 unrelated instruction/reference outlines, manifest/resource guards and negative package/version/missing-target checks. The reference APK's SHA-256 remains `78e5b1064ae0f774f9eed2b09c46998128ee6a2906f0a8cde57b766e8b1e300d`. The final subtitle-free extension passed serialized checks with 36 methods/eight classes, including the Google exception, Material 3 drawing, activity/preference gates and saved toggle. Standalone D8 emits direct system-bar API calls; the validator also retains checks for the equivalent AGP-generated outline pair.

All production Kotlin sources compiled after the wording changes. The official public 1.47.0 bundle supplied the existing library classes as an ignored local compile classpath; no removed collection was restored to production. The existing metadata generator exported eleven compiled patches, retaining defaults, options, dependency metadata and exact compatibility. README was regenerated, structural/whitespace checks passed, and all six Yanosik targets now have `isExperimental=false`. Version is 1.2.1 in Gradle and both metadata files. AGENTS.md records the user's confirmation and request to stop QA.

The final APK is `patches/build/yanosik-emulator/yanosik-26.9.0-login-material-test.apk`. It has a verified local APK v3 signature and was installed with `adb -s emulator-5554 install --no-incremental -r`, preserving data and sign-in. A signing attempt using the same password file for two explicit password reads failed at EOF; using the PKCS12 store password once resolved it. Credentials and the test key remain in ignored local files.

The full Gradle build still stops at settings plugin resolution because this host has no configured GitHub Packages credentials; the registry returned HTTP 401. Java 21 and the NixOS runtime don't remove that authentication requirement. No new Gradle-built `.mpp` or published release is claimed. The 1.2.1 download URL is prepared release metadata and isn't claimed to be live.

## All six patches, Vibe Patched tile and voice follow-up — 10 October 2026

The user signed in to the existing API 37 emulator and explicitly confirmed local PRO, the Vibe Patched tile and banner suppression work. The home PRO shortcut now displays the supplied `patches-bundle.png` with **Vibe Patched** underneath. Both action codes 68/70 bind the same decorative tile. Its click listener is cleared and clicking is disabled; the grid models and neighboring actions remain intact. Screenshot/UI hierarchy checks showed the exact image and label, and tapping both the image and label left the home screen unchanged. Evidence: `.tools/telemetry-vibe-home-before.png`, `.tools/telemetry-vibe-home-after.png` and their XML files.

The initial six-patch Vibe tile build passed in 51 seconds with one JUnit integration test, no skips/failures/errors, all six reference-APK applications, 60 intended methods (42 telemetry), 147,162 unrelated method outlines, 7,103 resource strings, negative compatibility/missing-target checks, 16 disabled ProxiCloud components and seven collection flags. The compiled icon matched the bundled bytes exactly. The standalone validator also passed after including the new validation helpers and the resources directory on its classpath. Logs: `.tools/all-six-vibe-tile-build.log`, `.tools/all-six-vibe-standalone.log`.

The expanded telemetry targets stop persisted ProxiCloud Wi-Fi/event-reaction workers and Notinote beacon/Wi-Fi sample storage, network sending and persisted workers. Natural Notinote worker runs finished with local WorkManager SUCCESS; forced scheduler wrappers deferred their actual bodies, so those forced runs are not counted as worker-body coverage. Firebase logged permanently deactivated measurement, dropped events and disabled Sessions sending. No Yanosik fatal exception or ANR appeared in the observed windows. Original records are not deleted.

All 19 initial ART guard checks passed on the installed Vibe tile APK, whose checksum matched the local file. The runner executes selected banner, startup, voice, local PRO, sidebar and telemetry guards in Android, without fake driving, reports or purchases. Its initial startup race was fixed with `waitForIdleSync`; assertions and production behavior were preserved. Logs: `.tools/all-six-vibe-art-runtime.log` and `.tools/telemetry-vibe-instrumentation-logcat.txt`. The broader voice follow-up below supersedes this initial voice coverage.

Signed-in UI checks also found the sidebar upgrade card absent, all three startup choices enabled, and the Application PRO switch hidden. The built-in announcement preview started the app's SoundPool with `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` / `CONTENT_TYPE_SPEECH`; the PLAY control returned after the preview. No audio preference was changed. Evidence: `.tools/telemetry-qa-startup-options.xml`, `.tools/telemetry-qa-sidebar.xml`, `.tools/qa-audio-preview.txt`, `.tools/qa-audio-finished.txt`.

The voice audit found a second dedicated advertising path, `AudioAlertsService.playAudioNotification`, which bypassed `Lzr.R`. It schedules the ADVERT playback category, updates `ADVERT_AUDIO_PLAY_TIME`, and reports an audio-ad campaign. The follow-up adds a narrow return at this method's entry while leaving the shared speech engine, warning controls and other service methods intact.

That audit also identified four dedicated marketing senders outside the generic analytics queues: `AudioAlertsService.sendAudioNotificationRequest` sends coordinates/speed/heading in an `ob8` request; `sendAudioNotificationExecutedRequest` sends campaign/source/timestamp completion data and analytics; `AdvertService.sendLocationAdvertRequest` sends position/speed/heading/timestamp; `AdvertService.analyticsSend` creates banner-impression events. All four are now explicit telemetry targets. Their service lifecycle and unrelated application requests remain intact. Final coverage is 46 telemetry targets and 65 modified methods across all six patches.

The final complete Gradle command is the same six-task combination in the next dated section, using Java 21, Android SDK 36, the configured user Gradle home and the absolute reference APK path. It passed in **55 seconds**, with one JUnit test and zero skips/failures/errors. Both actual advertising playback producers and their DEX call chains passed validation. The four marketing guards passed independent payload/register/first-return checks. Total serialized coverage: **65 intended methods, 147,157 unchanged method outlines**. This is an instruction/reference-outline comparison, not byte-for-byte operand comparison. Manifest, exact compiled icon, all previous guards/negative tests and 7,103 strings passed. Log: `.tools/all-six-final-build.log`.

README/index regeneration, migration validation (11 patches, 131 XML files), bundle manifest/version/Android DEX/icon checks and diff whitespace checks passed. The final bundle is `patches/build/libs/vibe-patch-1.0.0.mpp`, **2,123,914 bytes**, SHA-256 `fb1117807670217ab520b17b0057d6de7bd334153cdf0aa3fa56793598879d8c`. The exact supplied icon has SHA-256 `cee555cbc741e28acef9c14caf5b55c19cf41591d69c63b3e4b13c557f8edb6d` in the source and compiled resources. Import the bundle and repatch the original 26.9.0 APK; updating a bundle alone does not update an installed app.

The final emulator APK is `patches/build/yanosik-emulator/yanosik-26.9.0-all-six-test.apk`, **81,700,494 bytes**, SHA-256 `b03bd296b592f534868b0150d040fde0bbf40640e9d8852ddb7181a8fba22403`. It uses the existing ignored local test key, verified APK v3 signature, and was installed with `adb -s emulator-5554 install -r`, preserving data/sign-in. Its manifest/resources and four DEX files match the validator outputs; all original entries other than manifest/resources/DEX/signatures are preserved. This is a test-signed APK, not a publisher-signed update.

All **26 final ART checks passed** on this exact installed APK. The added voice checks cover a non-null offline campaign object and confirm `ADVERT_AUDIO_PLAY_TIME` remains unchanged. All four dedicated marketing send guards returned before request/event processing. Installed/local APK checksums matched; latest logs again showed deactivated Firebase measurement and no Yanosik fatal exception/ANR. Results: `.tools/all-six-final-art-runtime.log`, `.tools/telemetry-final65-instrumentation-logcat.txt`. A normal launch then displayed the map, intact controls, PRO badge, Vibe Patched home tile and sidebar without its upgrade card. The final normal-speech preview was not repeated because the user requested ending QA and pushing the version update.

The first signing attempt used the shell's stale Java 25 path and failed before signing/installing. Setting the verified Java 21 directory fixed it; `apksigner` v3 verification and `adb install -r` succeeded, preserving the user's login. An earlier home-grid removal left a default icon because its folder model still existed; the final user-requested renderer replacement avoids that path. A duplicate validator import was caught by Kotlin compilation and removed before the successful build.

Complete privacy and road QA remain limited: the emulator is non-root, ordinary account/map/push/remote-configuration/installation requests still exist, and encrypted/global packet capture cannot prove zero data reaches servers. Native/Flutter/web paths and server-side logging are not comprehensively audited. Actual road navigation, delivered live campaigns, Bluetooth and Android Auto still require device testing. No remote records were fabricated or deleted, and no release was published.

## Disable telemetry — 10 October 2026

Added the sixth independently selectable Yanosik patch, **Disable telemetry**. Reference APK identity remains `pl.neptis.yanosik.mobi.android`, 26.9.0, code 6001512, SHA-256 `78e5b1064ae0f774f9eed2b09c46998128ee6a2906f0a8cde57b766e8b1e300d`. The requested `yanosik_antispilt.apk` filename does not exist; the matching local file is `apk/yanosik_antisplit.apk`.

Inspected existing decompiled producers/services and confirmed exact DEX signatures, complete access flags, call/string anchors and register counts with dexlib2 (`.tools/InspectTelemetry.kt`, `.tools/telemetry-dex.txt`, `.tools/telemetry-extra-dex.txt`, `.tools/facebook-dex.txt`). The patch stops the identified protobuf/Flutter analytics, legacy Big Data, usage sampling, Wi-Fi/app scans, kiosk and ad-statistics paths; disables Firebase measurement/crash-upload/session gates and Facebook app events; uses ProxiCloud's existing inert implementation and disables its sixteen manifest services/receivers. Base lifecycle calls remain, including safe shutdown when subclass startup never ran. See [coverage and network limits](yanosik.md#telemetry-coverage-and-network-limits--10-october-2026).

Full command:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot'
$env:ANDROID_HOME = "$env:LOCALAPPDATA/Android/Sdk"
./gradlew.bat -g "$env:USERPROFILE/.gradle" --no-daemon :patches:buildAndroid :patches:test :patches:validateYanosik :patches:checkStringResources :patches:generatePatchesList '-PyanosikApk=C:/Users/Moxiu/Documents/morphe-patches/apk/yanosik_antisplit.apk'
```

The initial sandboxed command failed opening the user Gradle wrapper `.zip.lck` with access denied; approved access to the configured Gradle home/SDK resolved it. Initial compilation caught numeric access flags passed to the API that requires `List<AccessFlags>`; conversion to the complete flag list fixed it. First APK application caught a lifecycle assertion comparing the opcode's textual name with its enum constant name; comparing `Opcode.INVOKE_SUPER` directly fixed it. No missing fingerprint was skipped or weakened. The first optional manifest test could not import implementation-only ARSCLib/XMLPull classes from the Gradle compile classpath; replaced it with an independent binary-XML reader using Java's standard library.

The complete build passed in 42 seconds; subsequent compiled-manifest integration passed in 43 seconds. The final complete bundle/index/resource/test rebuild passed in 47 seconds: one JUnit test, zero skipped/failures/errors. Both JUnit and the standalone Gradle validator applied all six patches. Serialized DEX checks passed for **52 intended methods (35 telemetry), 147,170 unchanged method outlines**. Tests verify return types/values, encoded registers, the inert ProxiCloud constructor call, original method bodies and base lifecycle calls. An independent reader of the actual recompiled binary manifest verified **16 disabled ProxiCloud components, seven collection flags, and all unrelated manifest content unchanged**. Existing promotion/PRO branch, missing-target and incompatible-package/version tests passed. Resource validation checked 7,103 strings. Logs: `.tools/telemetry-build.log`, `.tools/telemetry-manifest-build.log`, `.tools/telemetry-final-build.log`.

Generated metadata and README now contain six Yanosik plus five universal patches. Structural validation and whitespace checks passed. The final bundle follows current `gradle.properties` version 1.0.0: `patches/build/libs/vibe-patch-1.0.0.mpp`. The final bundle is 1,264,925 bytes with SHA-256 `314394f82181c07765f32225c92129a3c9415d41c1faf499e02e9450f69cbd21`; its manifest reports Vibe Patch 1.0.0 and its Android DEX is present. This is an importable patch bundle, not an installable APK; import it and repatch the original compatible APK with Disable telemetry selected. Earlier emulator APKs/bundles do not contain this patch. No new APK was installed and no release was published.

No device, packet capture, crash/restart, route/audio or backend verification was performed for this patch. Online feature requests, native libraries/Flutter/web content and server request logging are not comprehensively audited. Firebase messaging/installations/remote configuration and purchase/account/navigation requests remain; some can carry identifiers or location. Old local telemetry records are not erased and already-uploaded server data is not removed. Crashlytics can still store fatal crash files locally, but its verified automatic/explicit upload paths are disabled. This implementation must not be described as proof of zero outbound data or an absolute privacy guarantee.

## Release commit argument-size fix — 10 October 2026

The supplied Actions log failed during semantic-release preparation with `Error: Command failed with E2BIG: git commit -m chore: Release v1.0.0 [skip ci]`. The configured commit message included the full generated notes from the fork's upstream history, exceeding the process argument-size limit. Removed `${nextRelease.notes}` from the git plugin's message; it now commits only the version and `[skip ci]`. The changelog asset, notes generator and GitHub release plugin remain configured, preserving full release notes in their existing destinations.

Added a regression check for the bounded commit message and retained notes destinations to `scripts/test_release_build.py`. All five checks passed with `python -X utf8 scripts/test_release_build.py`. Sandboxed attempts with both system and bundled Python failed writing temporary fixture files under `.tools` with `PermissionError`; a narrowly approved run outside the sandbox passed with `TEMP`/`TMP` still directed to `.tools`. Both workflows passed `.tools/actionlint/actionlint.exe -shellcheck= -pyflakes= .github/workflows/release.yml .github/workflows/build_pull_request.yml`; `git diff --check` passed.

The exact release build task combination in `prepare_release.py` remains `bash compile.sh --no-daemon :patches:generatePatchesList :patches:checkStringResources`, followed by README generation and migration validation. No build tasks or patch behavior changed, so the APK build was not repeated. No release was published or workflow dispatched. Push the fix and start a fresh run on the updated main commit to verify remote release preparation.

## GitHub runner SDK setup fix — 10 October 2026

The first remote run failed before compilation: `sdkmanager 'platforms;android-36' 'build-tools;36.0.0'` exited 127 because `sdkmanager` was not on the runner's PATH. Added `android-actions/setup-android@v4` after Java setup and before SDK package installation in both the release and pull-request build workflows. The action installs the command-line tools, accepts SDK licenses and exposes `sdkmanager` on PATH.

Local checks passed: `.tools/actionlint/actionlint.exe -shellcheck= -pyflakes= .github/workflows/release.yml .github/workflows/build_pull_request.yml` and `git diff --check`. No patch source or Gradle build tasks changed. A fresh GitHub run after pushing this commit is required to verify the runner setup; rerunning the old failed commit retains its old workflow.

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
