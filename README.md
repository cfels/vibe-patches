<p align="center">
  <img src="patches-bundle.png" width="128" alt="Vibe Patch">
</p>

# Vibe Patch ☆

My shitty ass patches that are mostly vibed for apks / hell even the reamdme😭

## Get the patches

[Add Vibe Patch to Morphe »](https://morphe.software/add-source?github=cfels/vibe-patches&name=Vibe%20Patch)

Or open **Morphe → Sources → + → Remote** and paste:

```text
https://github.com/cfels/vibe-patches
```

Pick your patches, patch the original APK, then install the result. The source can update through Morphe; your installed Yanosik still needs to be patched again.

A local `.mpp` works too: **Sources → + → Local**. It's a patch bundle, so it goes into Morphe, not Android's app installer. Local sources need to be reimported after updates.

A couple of things:

- Local PRO opens app-side settings. Server-only perks stay server-only.
- Less tracking leaves maps and login online. It also blocks RevenueCat purchases and subscription syncing.

## Build it yourself

NixOS:

```bash
nix-shell
bash compile.sh --no-daemon
```

Windows: `compile.bat`. Other Linux/macOS setups: `bash compile.sh`.

The bundle ends up at `patches/build/libs/vibe-patch-<version>.mpp`. Java 21, Android SDK 36 and GitHub Packages credentials are needed. [Build notes »](docs/build-and-release.md)

For the GPU test phone on NixOS, run `bash scripts/start-android-gpu.sh`. Keeps the existing app data and login.

## Releases

Run **Actions → Prepare Vibe Patch release** on `main`. Pick patch, minor or major; it bumps the version and commits the metadata for you. Pushes and PRs only check for stale files. The matching `.mpp` still needs to be built and uploaded to the release. [More on releases »](docs/build-and-release.md#releases)

## Patch list

Same names and descriptions you'll see in Morphe. The longer technical stuff is in the [Yanosik notes](docs/yanosik.md).

<!-- PATCHES_START -->
> **[v1.2.3](https://github.com/cfels/vibe-patches/releases/tag/v1.2.3)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;11 patches total
<details open>
<summary>📦 Yanosik&nbsp;&nbsp;•&nbsp;&nbsp;6 patches</summary>
<br>

**🎯 Supported versions:**

| 26.9.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Less tracking](#less-tracking) | Cuts down on analytics, ad reports, location targeting, Wi-Fi/app scans and crash uploads. Also blocks RevenueCat, so purchases and subscription syncing stop working. Maps and login still go online and may send your location or identifiers. |  |
| [Local PRO](#local-pro) | Unlocks the app's local PRO settings and hides the Switch to PRO toggle. Doesn't create a purchase or unlock server-only perks. |  |
| [No banner ads](#no-banner-ads) | Removes banners from the map, navigation and home screen, including the empty ad space. Road alerts and route controls stay. |  |
| [No startup ads](#no-startup-ads) | Skips ads when Yanosik opens, including cached ones. Normal startup messages and account checks still work. |  |
| [No voice ads](#no-voice-ads) | Mutes sponsored audio and spoken ads. Directions and road warnings still play. |  |
| [Vibe settings & less PRO spam](#vibe-settings-less-pro-spam) | Adds a saved dark mode toggle for home, sidebar, login, search and maps (on by default). The home PRO shortcut opens Vibe settings instead. Hides upgrade cards, closes the PRO page and removes donation prompts from settings. Direct donation and voucher pages still work. |  |

</details>

<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;5 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Another app copy](#another-app-copy) | Installs a second copy with a different package name. Adds .morphe by default. Each copy needs its own package name; some apps may crash or lose features. | • Package name<br>• Update permissions<br>• Update providers |
| [Debug app traffic](#debug-app-traffic) | Bypasses certificate pinning so a debugging proxy can inspect the app's traffic. |  |
| [No Play Store updates](#no-play-store-updates) | Raises the version code so Google Play won't overwrite the patched app. Some apps may break. Doesn't support root-mounted installs. |  |
| [Original app signature](#original-app-signature) | Makes the app see its original APK signature when it checks its own package. |  |
| [Pick install source](#pick-install-source) | Sets the store the app sees as its installer, like Google Play. | • App store to show |

</details>

<!-- PATCHES_END -->

## Changing stuff

Start with the [Yanosik notes](docs/yanosik.md) if you're working on a Yanosik patch. They cover the targets, limits and checks.

For a separate collection, there's the [Morphe Patches template](https://github.com/MorpheApp/morphe-patches-template).

## Credits

Made by [Moxiu](https://moxiu.vacpro.fyi). Built on [Morphe Patches](https://github.com/MorpheApp/morphe-patches), based on the prior work of [ReVanced](https://github.com/ReVanced). This is my own collection, not an official Morphe release.

Morphe's changes and their dates are in the Git history. Original notices and license terms are kept below and in [NOTICE](NOTICE).

## 📜 License

Morphe Patches are licensed under the [GNU General Public License v3.0](LICENSE), with additional conditions under GPLv3 Section 7:

- **Attribution (7b):** Any use of this code, including derivative works, must preserve all original notices and disclaimers.
- **Name & Branding Restrictions (7c & 7e):** Derivative works must use their own distinct branding. The **"Morphe"** name, logos, and trademarks may not be used for the branding or title of derivative works (e.g., names like *"Morphe Plus"*, *"Morphe Expanded"*, or *"Morphe UserXYZ"* are strictly prohibited).

See the [LICENSE](LICENSE) file for full GPLv3 terms and the [NOTICE](NOTICE) file for full conditions of GPLv3 Section 7.
