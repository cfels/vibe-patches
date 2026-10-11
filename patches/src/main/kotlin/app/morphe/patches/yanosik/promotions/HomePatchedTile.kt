/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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

internal const val VIBE_SETTINGS_CLASS = "Lapp/morphe/extension/yanosik/VibeSettings;"

internal object AccountThemeFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/features/login/AccountStartActivity;",
    name = "onCreate", returnType = "V", parameters = listOf("Landroid/os/Bundle;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(smali = "Lpl/neptis/features/login/AccountStartActivity;->A0(Landroid/os/Bundle;)V"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lch;->a:Landroid/widget/RelativeLayout;"),
        methodCall(smali = "Lo0;->setContentView(Landroid/view/View;)V")
    )
)

internal fun installAccountTheme(method: MutableMethod) {
    val view = method.getInstruction<TwoRegisterInstruction>(28)
    if (method.implementation!!.registerCount != 8 || method.accessFlags != 17 ||
        method.getInstruction<ReferenceInstruction>(15).reference.toString() !=
        "Lpl/neptis/features/login/AccountStartActivity;->A0(Landroid/os/Bundle;)V" ||
        method.getInstruction<ReferenceInstruction>(28).reference.toString() != "Lch;->a:Landroid/widget/RelativeLayout;" ||
        view.registerA != 0 || view.registerB != 0 ||
        method.getInstruction<ReferenceInstruction>(29).reference.toString() != "Lo0;->setContentView(Landroid/view/View;)V" ||
        method.getInstruction(30).opcode != Opcode.IF_NEZ ||
        (method.getInstruction(30) as OneRegisterInstruction).registerA != 7)
        throw PatchException("Yanosik account host lifecycle layout changed; refusing to patch.")
    method.addInstructions(30, "invoke-static { p0 }, $VIBE_SETTINGS_CLASS->install(Landroid/app/Activity;)V")
}

internal object HomeThemeFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/yanosik/mobi/android/dashboard/dashboardpack/activity/DashboardPackActivity;",
    name = "onCreate", returnType = "V", parameters = listOf("Landroid/os/Bundle;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT,
            smali = "Lpl/neptis/yanosik/mobi/android/dashboard/dashboardpack/activity/DashboardPackActivity;->U:Landroid/os/Handler;"),
        methodCall(smali = "Lo0;->setContentView(Landroid/view/View;)V")
    )
)

internal object HomeSidebarPaletteFingerprint : Fingerprint(
    definingClass = "Ly8;", name = "c", returnType = "J",
    parameters = listOf("Ll44;", "Ly93;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Ls90;->a:Lsb3;"),
        methodCall(smali = "Lyz7;->j(Lwgf;)Ljava/lang/Object;"),
        fieldAccess(opcode = Opcode.IGET_WIDE, smali = "Ll44;->b:J"),
        fieldAccess(opcode = Opcode.IGET_WIDE, smali = "Ll44;->a:J")
    )
)

// The map shortcut uses these two foreground palettes on its red Brand surface.
// Verify their original producers before referencing their identities in the guard.
internal object HomeMapTitlePaletteFingerprint : Fingerprint(
    definingClass = "Lt44;", name = "<clinit>", returnType = "V", parameters = emptyList(),
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Lg54;->b:Ll44;"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, smali = "Lt44;->b:Ll44;")
    )
)

internal object HomeMapDescriptionPaletteFingerprint : Fingerprint(
    definingClass = "Lx44;", name = "<clinit>", returnType = "V", parameters = emptyList(),
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Li54;->a:Ll44;"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, smali = "Lx44;->a:Ll44;")
    )
)

internal fun installHomeTheme(method: MutableMethod) {
    val call = method.getInstruction<ReferenceInstruction>(24)
    if (method.implementation!!.registerCount != 22 || method.accessFlags != 17 ||
        call.reference.toString() != "Lo0;->setContentView(Landroid/view/View;)V" ||
        method.getInstruction(25).opcode != Opcode.INVOKE_VIRTUAL ||
        method.getInstruction<ReferenceInstruction>(25).reference.toString() !=
        "Landroid/app/Activity;->getIntent()Landroid/content/Intent;")
        throw PatchException("Yanosik dashboard lifecycle layout changed; refusing to patch.")
    // p0 is v20; /range avoids the 4-bit invoke limit without modifying any registers.
    method.addInstructions(25, "invoke-static/range { p0 .. p0 }, $VIBE_SETTINGS_CLASS->install(Landroid/app/Activity;)V")
}

internal fun installHomeSidebarPalette(method: MutableMethod) {
    val instructions = method.implementation!!.instructions.toList()
    if (method.implementation!!.registerCount != 5 || method.accessFlags != 25 ||
        instructions.size != 27 || instructions[0].opcode != Opcode.INVOKE_VIRTUAL ||
        (instructions[23] as? ReferenceInstruction)?.reference.toString() != "Ll44;->b:J" ||
        (instructions[25] as? ReferenceInstruction)?.reference.toString() != "Ll44;->a:J")
        throw PatchException("Yanosik Compose palette layout changed; refusing to patch.")
    // LocalContext binds the guard to this composition's actual activity. The extension
    // accepts only the dashboard/login hosts, never map/navigation activities.
    // v0-v2 are unused at entry; p0/p1 are preserved on the normal continuing path.
    method.addInstructionsWithLabels(0, """
        check-cast p1, Lyz7;
        sget-object v0, Ls90;->b:Lb5j;
        invoke-virtual { p1, v0 }, Lyz7;->j(Lwgf;)Ljava/lang/Object;
        move-result-object v0
        check-cast v0, Landroid/content/Context;
        invoke-static { v0 }, $VIBE_SETTINGS_CLASS->useDark(Landroid/content/Context;)Z
        move-result v1
        if-eqz v1, :morphe_original_palette
        sget-object v0, Lt44;->b:Ll44;
        if-eq p0, v0, :morphe_map_foreground
        sget-object v0, Lx44;->a:Ll44;
        if-ne p0, v0, :morphe_dark_colors
        :morphe_map_foreground
        iget-wide v0, p0, Ll44;->a:J
        return-wide v0
        :morphe_dark_colors
        nop
        iget-wide v1, p0, Ll44;->b:J
        iget-wide v3, p0, Ll44;->a:J
        invoke-static { v3, v4, v1, v2 }, $VIBE_SETTINGS_CLASS->darkComposeColor(JJ)J
        move-result-wide v1
        return-wide v1
        :morphe_original_palette
        nop
    """)
}

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
        iget-object v1, v2, Lpf;->b:Landroid/view/View;
        iget-object v5, v2, Lpf;->d:Landroid/view/View;
        iget v6, v7, Lco8;->h:I
        invoke-static { v0, v1, v5, v6 }, $VIBE_SETTINGS_CLASS->bindTile(Landroid/view/View;Landroid/view/View;Landroid/view/View;I)V
        iget-object v0, v2, Lpf;->f:Landroid/view/View;
        invoke-virtual { v0, v4 }, Landroid/view/View;->setVisibility(I)V
        iget-object v0, v2, Lpf;->g:Landroid/view/View;
        invoke-virtual { v0, v4 }, Landroid/view/View;->setVisibility(I)V
        return-void
        :morphe_continue_home_tile
        nop
    """)
}
