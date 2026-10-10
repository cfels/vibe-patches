/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val localStatusFingerprint = Fingerprint(
    definingClass = "Lr5f;", name = "b", returnType = "Lh6f;",
    parameters = listOf("Lr5f;", "Lcom/revenuecat/purchases/EntitlementInfos;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(string("pro_"), string("pro_subscription"))
)

private val initialStatusFingerprint = Fingerprint(
    definingClass = "Lr5f;", name = "<init>", returnType = "V",
    parameters = listOf("Lkp3;", "Landroid/content/Context;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Lwwb;->L:Le6f;"),
        methodCall(smali = "Ll3j;->a(Ljava/lang/Object;)Lk3j;")
    )
)

private val localUiStateFingerprint = Fingerprint(
    definingClass = "Ll9f;", name = "<init>", returnType = "V",
    parameters = listOf("Z", "Z", "Lh9f;"),
    filters = listOf(fieldAccess(opcode = Opcode.IPUT_BOOLEAN, smali = "Ll9f;->e:Z"))
)

private val premiumUiStateFingerprint = Fingerprint(
    definingClass = "La6f;", name = "<init>", returnType = "V",
    parameters = listOf("Lh6f;", "Z", "Z", "Lh9f;"),
    filters = listOf(fieldAccess(opcode = Opcode.IPUT_BOOLEAN, smali = "La6f;->b:Z"))
)

private val proSwitchFingerprint = Fingerprint(
    definingClass = "Lpl/neptis/features/settings/AppPreferenceActivity;",
    name = "setupNoAdvSection", returnType = "V", parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    filters = listOf(
        fieldAccess(smali = "Ldh;->F:Landroid/widget/RelativeLayout;"),
        fieldAccess(smali = "Ldh;->G:Landroid/view/View;"),
        methodCall(smali = "Lpl/neptis/features/settings/AppPreferenceActivity;->getYbrViewModel()Lm0n;")
    )
)

internal const val LOCK_LOCAL_PRO_SWITCH = """
    iget-object v1, v0, Ldh;->D:Landroidx/appcompat/widget/SwitchCompat;
    const/4 v2, 0x1
    invoke-virtual { v1, v2 }, Landroid/widget/CompoundButton;->setChecked(Z)V
    const/4 v2, 0x0
    invoke-virtual { v1, v2 }, Landroid/view/View;->setEnabled(Z)V
    return-void
"""

// Local ACTIVE state, not a recurring subscription: expires 2100-01-01 UTC,
// willRenew=false. No receipt, transaction or remote entitlement is edited.
internal const val LOCAL_PRO_STATUS = """
    new-instance v0, Lb6f;
    sget-object v1, Lh4f;->ACTIVE:Lh4f;
    new-instance v2, Ljava/util/Date;
    const-wide v3, 0x3bb2cc3d800L
    invoke-direct { v2, v3, v4 }, Ljava/util/Date;-><init>(J)V
    const/4 v3, 0x0
    invoke-direct { v0, v1, v2, v3 }, Lb6f;-><init>(Lh4f;Ljava/util/Date;Z)V
    return-object v0
"""

@Suppress("unused")
val overrideLocalProChecksPatch = bytecodePatch(
    name = "Override local PRO checks",
    description = "Keeps local PRO mode on and opens up the PRO settings. " +
        "Hides the Switch to PRO toggle. Purchases and server-side perks aren't changed."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch)
    execute {
        val status = localStatusFingerprint.method
        val initial = initialStatusFingerprint.method
        val ui = localUiStateFingerprint.method
        val premium = premiumUiStateFingerprint.method
        val proSwitch = proSwitchFingerprint.method
        val switchStop = proSwitchFingerprint.instructionMatches.last().index
        val readIndex = initialStatusFingerprint.instructionMatches.first().index
        val read = initial.getInstruction<OneRegisterInstruction>(readIndex)
        if (status.implementation!!.registerCount != 11 || initial.implementation!!.registerCount != 4 ||
            readIndex != 4 || read.registerA != 2 || ui.implementation!!.registerCount != 4 ||
            premium.implementation!!.registerCount != 5 || proSwitch.implementation!!.registerCount != 7 ||
            switchStop != 16) {
            throw PatchException("Yanosik local PRO register layout changed; refusing to patch.")
        }
        status.addInstructions(0, LOCAL_PRO_STATUS)
        // Scope/context have already been stored. The patched static converter
        // ignores its remote input; seed the flow before any observer sees it.
        initial.replaceInstruction(readIndex, "const/4 v2, 0x0")
        initial.addInstructions(readIndex + 1, """
            invoke-static { p0, v2 }, Lr5f;->b(Lr5f;Lcom/revenuecat/purchases/EntitlementInfos;)Lh6f;
            move-result-object v2
        """)
        ui.addInstructions(0, """
            const/4 p1, 0x1
            const/4 p2, 0x1
            const/4 p3, 0x0
        """)
        premium.addInstructions(0, """
            const/4 p2, 0x1
            const/4 p3, 0x0
            const/4 p4, 0x0
        """)
        // Keep binding/null validation and the original row/separator hiding.
        // Stop before the YBR observer can show the purchase toggle again.
        proSwitch.addInstructions(switchStop, LOCK_LOCAL_PRO_SWITCH)
    }
}
