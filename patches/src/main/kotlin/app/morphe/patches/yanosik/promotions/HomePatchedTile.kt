/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

internal object HomePatchedTileFingerprint : Fingerprint(
    definingClass = "Lso8;", name = "n", returnType = "V",
    parameters = listOf("Ln8g;", "I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lgo8;->b:Ljava/util/List;"),
        methodCall(smali = "Lc03;->t0(Ljava/util/List;)Ljava/lang/Object;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lco8;->a:Ljava/lang/String;"),
        fieldAccess(opcode = Opcode.IGET, smali = "Lco8;->h:I")
    )
)

internal fun replaceHomeProTile(method: MutableMethod) {
    fun reference(index: Int) = (method.getInstruction(index) as? ReferenceInstruction)?.reference.toString()
    val binding = method.getInstruction<TwoRegisterInstruction>(11)
    if (method.implementation!!.registerCount != 10 || method.accessFlags != 17 ||
        reference(9) != "Lgo8;->b:Ljava/util/List;" ||
        reference(11) != "Lro8;->Z:Lpf;" || binding.registerA != 2 || binding.registerB != 8 ||
        method.getInstruction(12).opcode != Opcode.CONST_4 ||
        (method.getInstruction(12) as OneRegisterInstruction).registerA != 3 ||
        (method.getInstruction(12) as NarrowLiteralInstruction).narrowLiteral != 0 ||
        method.getInstruction(13).opcode != Opcode.CONST_16 ||
        (method.getInstruction(13) as OneRegisterInstruction).registerA != 4 ||
        (method.getInstruction(13) as NarrowLiteralInstruction).narrowLiteral != 8 ||
        reference(98) != "Lc03;->t0(Ljava/util/List;)Ljava/lang/Object;" ||
        method.getInstruction(99).opcode != Opcode.MOVE_RESULT_OBJECT ||
        (method.getInstruction(99) as OneRegisterInstruction).registerA != 7 ||
        method.getInstruction(100).opcode != Opcode.CHECK_CAST || reference(100) != "Lco8;" ||
        (method.getInstruction(100) as OneRegisterInstruction).registerA != 7 ||
        method.getInstruction(101).opcode != Opcode.IF_EQZ ||
        (method.getInstruction(101) as OneRegisterInstruction).registerA != 7) {
        throw PatchException("Yanosik home PRO tile renderer layout changed; refusing to patch.")
    }
    // The normal renderer overwrites v5/v6 before use. Null and all other actions
    // retain their original binding path; only the two PRO shortcuts return early.
    method.addInstructionsWithLabels(101, """
        if-eqz v7, :morphe_continue_home_tile
        iget v5, v7, Lco8;->c:I
        const/16 v6, 0x44
        if-eq v5, v6, :morphe_bind_patched_tile
        const/16 v6, 0x46
        if-ne v5, v6, :morphe_continue_home_tile
        :morphe_bind_patched_tile
        iget-object v0, v2, Lpf;->c:Landroid/view/View;
        check-cast v0, Landroid/widget/TextView;
        iget v1, v7, Lco8;->h:I
        invoke-virtual { v0, v1 }, Landroid/widget/TextView;->setTextColor(I)V
        const-string v1, "Vibe Patched"
        invoke-virtual { v0, v1 }, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
        iget-object v0, v2, Lpf;->b:Landroid/view/View;
        check-cast v0, Landroid/widget/ImageView;
        invoke-virtual { v0 }, Landroid/view/View;->getContext()Landroid/content/Context;
        move-result-object v1
        invoke-virtual { v1 }, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
        move-result-object v5
        invoke-virtual { v1 }, Landroid/content/Context;->getPackageName()Ljava/lang/String;
        move-result-object v6
        const-string v1, "vibe_patched"
        const-string v9, "drawable"
        invoke-virtual { v5, v1, v9, v6 }, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
        move-result v1
        invoke-virtual { v0, v1 }, Landroid/widget/ImageView;->setImageResource(I)V
        invoke-virtual { v0, v3 }, Landroid/view/View;->setVisibility(I)V
        iget-object v0, v2, Lpf;->d:Landroid/view/View;
        invoke-virtual { v0, v3 }, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        invoke-virtual { v0, v3 }, Landroid/view/View;->setClickable(Z)V
        iget-object v0, v2, Lpf;->f:Landroid/view/View;
        invoke-virtual { v0, v4 }, Landroid/view/View;->setVisibility(I)V
        iget-object v0, v2, Lpf;->g:Landroid/view/View;
        invoke-virtual { v0, v4 }, Landroid/view/View;->setVisibility(I)V
        return-void
        :morphe_continue_home_tile
        nop
    """)
}
