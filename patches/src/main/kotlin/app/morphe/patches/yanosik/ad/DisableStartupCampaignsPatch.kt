/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch

internal object StartupCampaignRefreshFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/data/startappadvert/StartAppAdvertService;",
    name = "refresh",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(methodCall(name = "getPremiumRepositoryLegacy"))
)

internal object StartupCampaignSelectionFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/yanosik/mobi/android/common/ui/activities/launcher/LauncherActivity;",
    name = "x0",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Lpl/neptis/yanosik/mobi/android/common/ui/activities/launcher/LauncherActivity;", "J", "Ldm3;"
    ),
    filters = listOf(
        methodCall(smali = "Lmyf;-><init>(Lpzi;JLbm3;)V"),
        string("startAppAdvertRepository")
    )
)

@Suppress("unused")
val disableStartupCampaignsPatch = bytecodePatch(
    name = "Disable startup ad campaigns",
    description = "Skips the ad campaigns shown when you open Yanosik, even ones already saved " +
        "on your phone. Normal startup messages and account checks stay as they are."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch)
    execute {
        StartupCampaignRefreshFingerprint.method.addInstruction(0, "return-void")
        // The dedicated launch-ad resolver returns null when no ad is eligible.
        // Skip its coroutine and wait-for-campaign path without changing app startup.
        StartupCampaignSelectionFingerprint.method.addInstructions(0, """
            const/4 v0, 0x0
            return-object v0
        """)
    }
}
