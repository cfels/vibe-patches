/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3330
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.all.misc.signature

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.fix.spoofsignature.spoofSignaturePatch

@Suppress("unused")
val spoofSignaturePatch = resourcePatch (
    name = "Spoof signature",
    description = "Makes the app see the original APK's signature when it checks its own package.",
    default = false
) {
    dependsOn(spoofSignaturePatch)
}
