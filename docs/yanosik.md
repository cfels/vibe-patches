# Yanosik patches: evidence and validation

Vibe Patch has five Yanosik patches and five universal patches. You can choose each one separately. The old YouTube, YouTube Music and Reddit collections were removed to keep this collection focused on what you use. This is a custom patch collection, not an official Yanosik product or endorsement.

For everyday use, the patch descriptions in the [README](../README.md) explain what you'll get. This page keeps the technical details, exact targets and testing limits together so future changes are easier to check.

The bundle is now named **Vibe Patch**, exported as `patches/build/libs/vibe-patch-1.46.0.mpp` by `compile.bat`. The current five-patch implementation passed serialized DEX checks for 17 intended methods with 147,205 unrelated outlines preserved. A signed test APK is installed in the Android API 37 emulator; the user signed in and confirmed the Application PRO switch is gone. Sidebar removal/local PRO badge/settings/map observations and remaining runtime limits are recorded in the [newest validation section](yanosik-validation.md). Earlier dated sections below retain historical results. See [custom source icon instructions](vibe-patch-branding.md).

## Reference and architecture

The repository uses Morphe's Kotlin `bytecodePatch`/`resourcePatch` DSL, Gradle settings plugin 1.3.4, Morphe Patcher 1.14.0, and patches library 1.8.0-dev.2. Named top-level patches are discovered by the framework's JAR loader; extension modules contain Android code injected when needed. The Yanosik patches require no extension, resource changes, manifest changes, new permissions, or extra runtime dependencies.

The supplied local `apk/yanosik_antisplit.apk` was inspected with jadx 1.5.3 and baksmali, then rechecked with the user's jadx 1.5.6 and apktool 3.0.3 during the navigation-banner follow-up. Its manifest reports:

| Field | Value |
| --- | --- |
| Package | `pl.neptis.yanosik.mobi.android` |
| Version name / code | `26.9.0` / `6001512` |
| Minimum / target / compile SDK | 29 / 37 / 37 |
| Application | `pl.neptis.yanosik.mobi.android.YanosikApp` |
| Launcher | `pl.neptis.yanosik.mobi.android.common.ui.activities.launcher.LauncherActivity` |
| SHA-256 | `78e5b1064ae0f774f9eed2b09c46998128ee6a2906f0a8cde57b766e8b1e300d` |

The package is also confirmed by the [official Google Play listing](https://play.google.com/store/apps/details?id=pl.neptis.yanosik.mobi.android). Version and SDK values above describe the supplied artifact; they are not a claim about the latest store release. The filename indicates an antisplit artifact, so publisher signature authenticity and pristine distribution provenance are not established. No certificate was invented for compatibility metadata. A publisher-signed reference with identical ABI should be validated before publishing a release.

Compatibility is deliberately limited to 26.9.0 and marked experimental. A shared execution guard rejects any other package or version even if a caller bypasses the framework's compatibility UI. Exact descriptors, anchors and register layouts must match. Other versions require fresh inspection and tests.

## Implemented patches

All five use the existing default-on patch selection convention. They are independently selectable; there are no injected settings or persistence keys. Configuration is at patching time. To restore a behavior, patch the original APK again with that patch omitted. An already patched APK is not an undo source.

| Patch | Exact modification | Coverage and limits |
| --- | --- | --- |
| Suppress Premium promotions | Guard `Lss0.b` for actions 68/70 and `Lo6f.a` for nine promotional card slots; return before `Lm1a.i` renders the sidebar Premium card; finish `PremiumActivity.onCreate` after base initialization for PRO/default/YBR targets. | Sidebar header, settings, wallet, account options and logout remain. Explicit DONATE/VOUCHER activity targets continue. The card's SUBSCRIPTION_MANAGEMENT/null paths remain, but its PRO activity closes. No global dialog filter or billing SDK modification. |
| Override local PRO checks | Prefix `Lr5f.b` with a local ACTIVE `Lb6f` state, seed it in `Lr5f.<init>`, override `Ll9f`/`La6f` flags, and stop `AppPreferenceActivity.setupNoAdvSection` after the original row/separator hiding. | Enables startup/refresh local PRO state and settings gates; clears marketing alerts. The hidden Switch to PRO control is checked/disabled; its old YBR observer cannot reveal it again. Expiry is 2100-01-01 UTC with willRenew=false. Remote receipts/transactions/entitlements and server-controlled benefits are unchanged. |
| Hide banner ads | Prefix `La5b;->a(Lah1;Lu1c;Ly93;I)V`, `Ln1a;->b(Lah1;Lu1c;Ly93;I)V` and `Lnhc;->j(Llp;Lu1c;Ly93;I)V` with `return-void`, plus the two legacy banner fragments' `onViewCreated(View, Bundle)` methods with `View.setVisibility(GONE)` and return. | Covers the current Compose map/navigation banners and dashboard ad card, including initial placeholders and subsequent cached/refreshed images. No Compose image or layout node is created by these renderers. Legacy observer registration is still skipped. Parent map/alert/search/route UI is preserved. Visual collapse and recomposition still require device QA. |
| Disable startup ad campaigns | Prefix `StartAppAdvertService.refresh()V` with `return-void`, and `LauncherActivity.x0(LauncherActivity, long, dm3)` with a null result. | Prevents campaign refresh and selection, including already cached banner/video campaigns. Null is the resolver's existing no-ad result; it is returned before creating its coroutine or waiting for an advertising pack. Lifecycle, logout, preferences and general startup-popup messages are retained. |
| Disable voice ads | Prefix `Lzr;->R(Lsva;Lwq;Z)V` with `return-void`. | Stops the dedicated sponsored-POI and stop-campaign audio dispatch before playback and emission bookkeeping. It does not patch the shared speech engine, navigation speech, warnings, radio or volume controls. This is independently selectable from banner suppression. |

The promotion patch now closes the PRO purchase/status activity even when another entry point opens it. This intentionally removes its subscription-management screen access too; explicit donation/voucher targets continue. Action 71 uses a different payload and remains untouched. Generic server-authored dialogs, notifications and other unclassified surfaces are outside coverage. There is no global dialog/string filter. The separate local PRO patch changes the app's local state producer, not the RevenueCat SDK or remote account data.

Banner suppression is UI-only. The separate voice-ad patch suppresses the verified advertising audio dispatcher. Neither claims network-wide blocking or changes the `AdvertService` entitlement result. Dedicated sponsored-POI banners using `Lah1;` are also hidden; other sponsored POI map markers and legacy alert visuals remain outside coverage. Navigation and warning services are not patch targets.

### Additional verified promotion and advertising coverage

The user's navigation screenshot exposed a coverage gap in the original fragment-only banner patch. `AdvertService.onSuccess` publishes a `Lgq;` map-banner event. `Lyh1;` consumes it, checks image dimensions of 960×225, and exposes an `Lah1;` (`Banner`) state with `banner_ad_placeholder` as its initial image. That drawable is resource `0x7f080152` in the apktool-decoded resource table. The model reaches `La5b.a` through `Lb61.g`, and `Ln1a.b` through the navigation sheet's `Lxa4;` callback. Both image renderers call `Luvg.a`; the separate dashboard `Llp;` (`Advert`) card uses `Lnhc.j` and `Lt8d.a`. Returning at each method entry suppresses the image, its aspect-ratio layout and placeholder before opening a Compose group. Passing modifiers from their callers does not itself create a banner layout node.

All three Compose targets are public/static/final and retain their original register counts (23, 22 and 25). Exact class, signature, access flags, image-field access and image-renderer calls are required; there is no permissive fallback. All five banner methods resolve before any is modified. `Lb61.g`'s separate `Lc9k;` alert branch, `Lxa4;`'s sheet content, `Lnhc.k`'s search/content UI, navigation services and entitlement repositories remain unchanged in the serialized instruction/reference-outline comparison. The navigation callback can still register its separate impression coroutine; this patch is not an analytics/network blocker.

The PRO card renderer uses `Lc24;->PROMO` and the dedicated `Lh9f;` (`ProAlert`) model, with title, description and upgrade-button text. Its input producer in `Lph0;` reads explicit marketing configuration keys such as `app.yanosik.pro.trip_summary`, `trip_history`, `gamification_statistics` and their `.title`/`.description`/`.button` fields. This identifies promotional cards separately from payment errors, expiration/status notices and account-security dialogs.

The nine suppressed `Li9f;` slots are `TRIP_SUMMARY`, `TRIP_HISTORY`, `MAP_SETTINGS`, `GAMIFICATION_STATISTICS`, `GAMIFICATION_POINTS_TABLE`, `EXIT`, `SETTINGS_FLOAT`, `SETTINGS_ALL` and `MENU_FOR_YBR`. The subscription-management slot continues through the original renderer. Guard code uses only dead scratch registers v0/v1, leaves all seven parameters intact, and exits before Compose opens its group. The original renderer and its recomposition callback remain intact on the preserved path. Device tests must confirm layout collapse and recomposition behavior.

`Lzr;` is the advertising audio controller: `R` calls `T`, which resolves campaign audio then calls `V`. `V` records `ADVERT_FORMAT_TYPE_ADVERTISING_POI_AUDIO` / `ADVERT_FORMAT_TYPE_ADVERTISING_POI_STOP_AUDIO` and calls `S`; `S` selects the shared player's `Lzoi;->ADVERT` category. The patch changes only `R`, leaving these helpers and the shared player untouched. It does not force `isPro`, edit the entitlement gate `f0()`, or emit false playback analytics. Actual speaker/Bluetooth/Android Auto behavior is not runtime-tested.

`Lpzi;` identifies itself as `StartAppAdvertRepository`. Its campaign selector reads `START_APP_ADVERT_STATE` and resolves cached banner/video media. `LauncherActivity.x0(LauncherActivity, long, dm3)` is the dedicated launch-ad resolver that waits for eligibility and calls `Lmyf;` to select a campaign. It already returns null when ads are ineligible. The patch returns null at this resolver before its ad-specific coroutine/wait path, so it also prevents selection of cached campaigns. The caller `Ldk4;->invokeSuspend(Object)` casts its result to the nullable `Lbzi;` model and stores it in `LauncherActivity.Y`; the launcher adds advertising extras only when Y is non-null. No cached files or seen counts are rewritten, and normal startup/authentication/permission flows remain unchanged. Visual transitions still require device QA.

## Investigation of requested feature groups

The screenshot includes paid benefits. On 10 October the user explicitly expanded the request to override local PRO checks and hide the PRO screen/sidebar promotions. The following table distinguishes local gates from server-authoritative behavior; enabling a UI gate does not establish a server subscription.

| Group | Evidence found | Decision / remaining blocker |
| --- | --- | --- |
| Advertising and Premium promotions | Exact renderers, advertising service, PRO dispatcher, sidebar renderer and purchase activity described above. | Five patches implemented. Dedicated banner/voice/startup surfaces and identified PRO promotion surfaces are suppressed. Other sponsored visuals and unclassified dialogs remain outside coverage. |
| Floating-icon data selection | Existing `OverlaySettingsActivity` consumes local PRO state; `OverlayService` and SYSTEM_ALERT_WINDOW already exist. PiP uses a separate observer. | Local settings gates now enabled. Overlay permission and device behavior still require runtime verification; no new overlay/PiP implementation. |
| Android counterpart to Live Activity / Dynamic Island | Manifest has notification permission and location foreground services; `SystemNotificationsService` manages system notification data. | Existing Android notification integration is preserved. No useful, independent notification customization was verified, and no iOS feature is claimed. Device/UI requirements are needed for a scoped Android customization. |
| Points, tasks and rankings | `NewGamificationActivity`, network `GamificationDayCompletedModel`, and protobuf `Llo4;` expose points/combo values. PRO benefits include extra monthly points and rankings. | No fake points, official tasks, rankings or reports. Local display is not proof of locally authoritative progression. No justified presentation-only change was identified. |
| Combo preservation without driving | Network completion model includes `comboDays` and `points`; `ComboDaysOverlayView` renders progression. | No verified local-only streak preservation without falsifying activity. PRO freezing and server progression remain intact. Backend behavior was not probed. |
| Default startup screen | APP_START_SCREEN, DASHBOARD/REPORTING/MAP preferences and AppPreferenceActivity's `Ll9f` observer gate radio-button interaction. | Local gate now enabled by the PRO-state patch. Uses the existing preference and startup implementation; runtime persistence/restart checks remain. |
| Route history and deletion | TravelHistoryActivity's state combines local PRO flags with remotely supplied trips; `Lcnk` exposes swipe/delete callbacks. | Local PRO presentation can unlock existing controls. Server retention/deletion remains authoritative and unverified. No trips were deleted or fabricated during testing. |

The local state override covers the verified PRO state and settings producers. Extra points, challenges, rankings, combo freezing and longer server history are not claimed as working benefits. Their server behavior remains unverified. Android cannot acquire the screenshot's iOS-only Live Activity/Dynamic Island implementation through these patches.

## Cleanup and file-level scope

Removed `patches/.../youtube`, `patches/.../music`, their extensions, `extensions/shared-youtube`, Music Jam tests/task, video-app resource families, custom-icon documentation/assets, and dedicated protobuf/JavaScript/Commons catalog entries. Factories under `patches/.../shared` used exclusively by the two removed collections were also removed: video streaming, Litho, audio, Google-app support, playback and related helpers.

Removed the Reddit implementations, dedicated extension/stubs and localized resources, its cloning special cases, translation mapping, and unused AndroidX Core/HiddenApiBypass dependency entries. Preserved universal implementations, `extensions/shared`, generic Kotlin preference serializers, shared experimental-version helpers and localized resources, licensing assets, Gradle/release coordinates and infrastructure. Removed the two video-app cases from universal cloning configuration while preserving Photos/Magazines cases. Updated resource locale checking, Crowdin mappings, issue template and README. Historical `CHANGELOG.md` and the pre-existing archives remain as attribution/history; their references are not active patch targets.

[File inventory](yanosik-migration-files.json) lists every removed tracked file. Six production Kotlin files, two Kotlin test files and two validation scripts were added. The generated index now contains five Yanosik and five universal patches. The README uses the existing generator. The stale old-release `patches-bundle.json` was removed; release automation will recreate it for an actual release. A signed local test APK is installed in the API 37 emulator; no published release is claimed.

## Navigation-banner follow-up validation (10 October 2026)

| Check | Result |
| --- | --- |
| Full Gradle build and Android bundle | Passed: `:patches:buildAndroid`, `:patches:test`, `:patches:validateYanosik`, `:patches:checkStringResources` and `:patches:generatePatchesList`; BUILD SUCCESSFUL in 25 seconds. |
| Reference APK integration and DEX write/read | All four patches applied. Ten intended methods changed; 147,212 unrelated method instruction/reference outlines preserved. The three new banner renderers return before any original instruction and retain their original register counts and bodies. |
| Promotion guards and negative checks | Passed all previous action/enum inputs, missing fingerprint and incompatible package/version checks. |
| Gradle JUnit | One test, zero skipped, zero failures/errors, using the supplied APK. |
| Resources and index | 7,103 strings checked without issues; complete patch index regenerated. Migration validation and README regeneration are recorded in the companion validation document. |
| Device UI verification | No ADB device connected. Installation, visual collapse/recomposition and navigation/audio behavior have not been verified on a phone. |

The current output is `patches/build/libs/vibe-patch-<version>.mpp` (version in `gradle.properties`); both build scripts create this bundle directly. Import the rebuilt bundle and patch the original 26.9.0 APK with **Hide banner ads** selected. Replacing the patch bundle alone does not change an already installed APK. The previous fragment-only bundle does not contain this fix. See [build and release setup](build-and-release.md).

## Historical validation results (9 October 2026)

The build-access limitations below were resolved by the user's configured GitHub Packages credentials and Android SDK before the 10 October follow-up.

| Check | Result |
| --- | --- |
| Baseline `:patches:build` | Failed before source compilation: settings plugin `app.morphe.patches:1.3.4` could not resolve from configured repositories. Initial sandbox download failure was overcome with an isolated cache and approved network access. GitHub Packages credentials remain unconfigured. |
| New production and validation Kotlin compilation | Passed using Kotlin 2.3.0/JDK 21 and the exact Morphe Patcher 1.14.0 bundled in official Morphe Desktop 1.16.0. This does not compile all unrelated modules. |
| Framework JAR discovery | Passed: exactly the four named Yanosik patches discovered. |
| Real reference APK patch application | All four passed under Morphe Patcher 1.14.0. |
| DEX write/read regression | Passed: seven intended methods modified, 147,215 unchanged method instruction/reference outlines compared. This is not a byte-for-byte comparison or an Android ART verifier run. |
| Encoded promotion control flow | Passed for actions 0–100, -1 and Int.MAX_VALUE: only 68/70 returned handled; other inputs continued. The serialized card guard suppressed all nine promotional slots and preserved subscription management/null arguments and the alert parameter. |
| Missing target / wrong package/version | Passed: missing fingerprint failed; package/version guard rejected incompatible input with an explicit message. |
| JUnit regression | Passed via JUnit console 1.10.1: one test, zero skipped, zero failures, using the supplied APK and patcher 1.14.0. |
| Final offline `:patches:compileKotlin`, full Gradle build, Android extensions, Gradle tests and official index generation | Blocked at settings plugin resolution (`app.morphe.patches:app.morphe.patches.gradle.plugin:1.3.4` unavailable). An independent Clone app compilation attempt also lacked the external patches-library XML helpers; only Yanosik standalone compilation is validated. No full-build pass claimed. |
| Final migration structural checks | Passed: nine indexed patches (four Yanosik, five universal), 131 resource XML files parsed, no removed app package/import references, and clean diff whitespace. Yanosik reference-APK validation rerun passed after the expanded implementation. |
| Installation, launch, notification permission behavior, layout/popup/navigation runtime QA | Not run: no configured Android SDK/adb/device session available. |

Inspection with jadx completed with 90 decompilation errors in the app/dependencies. Relevant patch methods were checked in baksmali or decoded directly from the original DEX with dexlib2, then matched/applied by the real patcher; decompiler output alone was not treated as verification.

Exact commands and outcomes for this follow-up are recorded in [validation commands](yanosik-validation.md).

## Reproduce validation

Configure Java 21 and Android SDK 36 for the retained extension modules. The target app's SDK 37 does not require raising the extensions' existing compile SDK. Configure GitHub Packages credentials privately via user Gradle `gpr.user`/`gpr.key`, or `GITHUB_ACTOR`/`GITHUB_TOKEN`. Keep tokens out of the repository and chat.

```powershell
./gradlew.bat :patches:buildAndroid :patches:test -PyanosikApk=C:/path/to/reference.apk
./gradlew.bat :patches:validateYanosik -PyanosikApk=C:/path/to/reference.apk
./gradlew.bat :patches:checkStringResources :patches:generatePatchesList
python scripts/validate-migration.py
git diff --check
```

For independent DEX validation without resolving the Gradle plugin, provide the official [Morphe Desktop 1.16.0 executable](https://github.com/MorpheApp/morphe-desktop/releases/tag/v1.16.0) (bundled patcher 1.14.0), and [Kotlin compiler 2.3.0](https://github.com/JetBrains/kotlin/releases/tag/v2.3.0):

```powershell
./scripts/validate-yanosik.ps1 -ReferenceApk ./apk/yanosik_antisplit.apk `
  -MorpheRuntimeJar ./tools/morphe-desktop-1.16.0-all.jar `
  -KotlinHome ./tools/kotlinc
```

The script compiles the real new source and runs reference-APK patching, DEX serialization, suppression control-flow checks and negative checks. It emits local DEX files, not an installable/signed APK. Tools, decrypted/decompiled application content, caches, APKs and intermediate DEX remain local ignored artifacts.

Before release, verify free and subscribed accounts, intentional PRO-screen suppression and donation/voucher preservation, banner layouts and recomposition, cached startup ads, advertising audio versus road warnings, launch/authentication/permissions, navigation restoration and essential road warnings. Local PRO state is intentionally overridden. Remote purchases, GPS, safety warnings and server progression are not modified.
