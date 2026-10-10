/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import com.android.tools.smali.dexlib2.Opcode

internal object AdvertisingVoiceDispatchFingerprint : Fingerprint(
    definingClass = "Lzr;",
    name = "R",
    returnType = "V",
    parameters = listOf("Lsva;", "Lwq;", "Z"),
    filters = listOf(
        string(" advertPoiSoundController.canPlayAudio"),
        methodCall(opcode = Opcode.INVOKE_DIRECT, smali = "Lzr;->T(Lwq;Z)V")
    )
)

@Suppress("unused")
val disableVoiceAdsPatch = bytecodePatch(
    name = "Disable voice ads",
    description = "Mutes the sponsored voice ads so they don't interrupt your drive. " +
        "You still get navigation directions and road warnings, with the same audio controls."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch)
    execute {
        AdvertisingVoiceDispatchFingerprint.method.addInstruction(0, "return-void")
    }
}
