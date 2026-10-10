/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

internal object PremiumPromotionDispatcherFingerprint : Fingerprint(
    definingClass = "Lss0;",
    name = "b",
    returnType = "Z",
    parameters = listOf("Lgzc;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, smali = "Lgzc;->x:I"),
        string("app_view")
    )
)

internal object PremiumPromotionCardFingerprint : Fingerprint(
    definingClass = "Lo6f;",
    name = "a",
    returnType = "V",
    parameters = listOf("Lh9f;", "Llv7;", "Lu1c;", "Z", "Ly93;", "I", "I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lh9f;->a:Li9f;"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Lc24;->PROMO:Lc24;")
    )
)

internal object SidebarPremiumCardFingerprint : Fingerprint(
    definingClass = "Lm1a;", name = "i", returnType = "V",
    parameters = listOf("Lb4f;", "Ly93;", "I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(smali = "Lyz7;->c0(I)Lyz7;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lb4f;->c:Lhdi;")
    )
)

internal object PremiumActivityFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/user/ui/premium/PremiumActivity;",
    name = "onCreate", returnType = "V", parameters = listOf("Landroid/os/Bundle;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(smali = "Lpl/neptis/user/ui/premium/PremiumActivity;->v0(Landroid/os/Bundle;)V"),
        string("pl.neptis.user.ui.premium.EXTRA_TARGET")
    )
)

// The first two original calls initialize Activity/Hilt. Close PRO/YBR/default
// screens after that lifecycle setup, preserving explicit donation/voucher flows.
internal const val PREMIUM_SCREEN_GUARD = """
    invoke-virtual { p0 }, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
    move-result-object v0
    const-string v1, "pl.neptis.user.ui.premium.EXTRA_TARGET"
    invoke-virtual { v0, v1 }, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    const-string v1, "DONATE"
    invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-nez v1, :morphe_continue_premium_screen
    const-string v1, "VOUCHER"
    invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-nez v1, :morphe_continue_premium_screen
    invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
    return-void
    :morphe_continue_premium_screen
    nop
"""

// Run before Compose opens its restart group. v0/v1 are overwritten by the
// original prologue on the preserved path; p0 is the unchanged alert parameter.
internal const val PREMIUM_PROMOTION_CARD_GUARD = """
    move-object/from16 v0, p0
    if-eqz v0, :morphe_continue_promo_card
    iget-object v0, v0, Lh9f;->a:Li9f;
    sget-object v1, Li9f;->SUBSCRIPTION_MANAGEMENT:Li9f;
    if-eq v0, v1, :morphe_continue_promo_card
    return-void
    :morphe_continue_promo_card
    nop
"""

// Inserted after the first action-code read, before the original const-string v4.
// v4 is dead here and overwritten by that instruction on every continuing path.
internal const val PREMIUM_PROMOTION_GUARD = """
    const/16 v4, 0x44
    if-eq v3, v4, :morphe_skip_premium_promotion
    const/16 v4, 0x46
    if-ne v3, v4, :morphe_continue_dispatch
    :morphe_skip_premium_promotion
    const/4 v4, 0x1
    return v4
    :morphe_continue_dispatch
    nop
"""

@Suppress("unused")
val suppressPremiumPromotionsPatch = bytecodePatch(
    name = "Suppress Premium promotions",
    description = "Hides PRO promo cards and upgrade shortcuts, including the sidebar card, " +
        "and closes the PRO page. Donation and voucher pages stay available."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch)
    execute {
        val sidebar = SidebarPremiumCardFingerprint.method
        val activity = PremiumActivityFingerprint.method
        if (sidebar.implementation!!.registerCount != 24 || activity.implementation!!.registerCount != 11 ||
            PremiumActivityFingerprint.instructionMatches.first().index != 1) {
            throw PatchException("Yanosik PRO screen/sidebar register layout changed; refusing to patch.")
        }
        val fingerprint = PremiumPromotionDispatcherFingerprint
        val method = fingerprint.method
        val readIndex = fingerprint.instructionMatches.first().index
        val read = method.getInstruction<TwoRegisterInstruction>(readIndex)
        val next = method.getInstruction(readIndex + 1)
        if (read.registerA != 3 || next.opcode != Opcode.CONST_STRING ||
            (next as OneRegisterInstruction).registerA != 4) {
            throw PatchException("Yanosik PRO dispatcher register layout changed; refusing to patch.")
        }
        method.addInstructionsWithLabels(readIndex + 1, PREMIUM_PROMOTION_GUARD)

        val card = PremiumPromotionCardFingerprint.method
        val implementation = card.implementation
            ?: throw PatchException("Yanosik PRO card renderer has no implementation.")
        // Exact reference prologue: moves p0 into v1, then p1 into v7.
        val first = card.getInstruction<TwoRegisterInstruction>(0)
        if (!AccessFlags.STATIC.isSet(card.accessFlags) || implementation.registerCount != 31 ||
            card.getInstruction(0).opcode != Opcode.MOVE_OBJECT_FROM16 ||
            first.registerA != 1 || first.registerB != 24) {
            throw PatchException("Yanosik PRO card register layout changed; refusing to patch.")
        }
        card.addInstructionsWithLabels(0, PREMIUM_PROMOTION_CARD_GUARD)
        sidebar.addInstructions(0, "return-void")
        activity.addInstructionsWithLabels(2, PREMIUM_SCREEN_GUARD)
    }
}
