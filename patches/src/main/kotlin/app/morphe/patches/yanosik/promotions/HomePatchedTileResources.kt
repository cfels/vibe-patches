/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.promotions

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.yanosik.shared.referenceVersionPatch

private object HomePatchedTileAssets

internal val homePatchedTileResourcesPatch = resourcePatch {
    dependsOn(referenceVersionPatch)
    execute {
        val destination = get("res/drawable-nodpi/vibe_patched.png")
        destination.parentFile.mkdirs()
        check(!destination.exists()) { "Vibe Patched drawable already exists; use the original APK" }
        val icon = checkNotNull(HomePatchedTileAssets::class.java.getResourceAsStream("/yanosik/vibe_patched.png")) {
            "Missing bundled Vibe Patched icon"
        }
        icon.use { source -> destination.outputStream().use { source.copyTo(it) } }
    }
}
