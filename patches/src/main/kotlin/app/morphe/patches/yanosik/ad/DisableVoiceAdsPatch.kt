/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags

internal object AdvertisingVoiceDispatchFingerprint : Fingerprint(
    definingClass = "Lzr;",
    name = "R",
    returnType = "V",
    parameters = listOf("Lsva;", "Lwq;", "Z"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        string(" advertPoiSoundController.canPlayAudio"),
        methodCall(opcode = Opcode.INVOKE_DIRECT, smali = "Lzr;->T(Lwq;Z)V")
    )
)

// Campaign audio uses the same ADVERT player category as sponsored POIs, but
// enters it through a separate service rather than the POI dispatcher above.
internal object AdvertisingCampaignVoiceFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/features/audioalerts/AudioAlertsService;",
    name = "playAudioNotification", returnType = "V", parameters = listOf("Ll11;"),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, smali = "Llo0;->I()Lcqi;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Ll11;->g:Ljava/lang/String;"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Lzoi;->ADVERT:Lzoi;"),
        methodCall(opcode = Opcode.INVOKE_DIRECT_RANGE,
            smali = "Lrkd;-><init>(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;" +
                "Ljava/lang/Object;Lbm3;I)V"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Lk2f;->ADVERT_AUDIO_PLAY_TIME:Lk2f;")
    )
)

@Suppress("unused")
val disableVoiceAdsPatch = bytecodePatch(
    name = "Disable voice ads",
    description = "Mutes sponsored voice ads and spoken ad campaigns so they don't interrupt your drive. " +
        "You still get navigation directions and road warnings, with the same audio controls."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch)
    execute {
        val poiVoice = AdvertisingVoiceDispatchFingerprint.method
        val campaignVoice = AdvertisingCampaignVoiceFingerprint.method
        poiVoice.addInstruction(0, "return-void")
        campaignVoice.addInstruction(0, "return-void")
    }
}
