/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

internal const val PACKAGE_NAME = "pl.neptis.yanosik.mobi.android"
internal const val VERSION_NAME = "26.9.0"

internal val COMPATIBILITY_YANOSIK = Compatibility(
    name = "Yanosik",
    packageName = PACKAGE_NAME,
    // The supplied antisplit APK is a reference, not proof of the publisher's signature.
    targets = listOf(AppTarget(version = VERSION_NAME, minSdk = 29, isExperimental = true))
)

internal fun requireReferenceVersion(packageName: String, versionName: String) {
    if (packageName != PACKAGE_NAME || versionName != VERSION_NAME) {
        throw PatchException("Yanosik patches require $PACKAGE_NAME $VERSION_NAME; " +
            "received $packageName $versionName.")
    }
}

internal val referenceVersionPatch = bytecodePatch {
    execute {
        requireReferenceVersion(packageMetadata.packageName, packageMetadata.versionName)
    }
}
