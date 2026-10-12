/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val bannerViewFingerprints = listOf("small/AdsSmallMainFragment", "dashboard/AdsDashboardMainFragment")
    .map { surface ->
        Fingerprint(
            definingClass = "Lpl/neptis/features/adsbanners/banners/$surface;",
            name = "onViewCreated",
            accessFlags = listOf(AccessFlags.PUBLIC),
            returnType = "V",
            parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
            filters = listOf(methodCall(name = "getViewModel"))
        )
    }

// Version 26.9.0 renders its current map/navigation banners in Compose, not
// AdsSmallMainFragment. Both Lah1 renderers accept the dedicated Banner model:
// a5b also installs the impression callback; n1a is used by the navigation sheet.
// nhc.j is the dashboard's separate Advert card. Never suppress their callers,
// which also render road alerts, search, route controls and other useful content.
internal val composeBannerFingerprints = listOf(
    Fingerprint(
        definingClass = "La5b;", name = "a", returnType = "V",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
        parameters = listOf("Lah1;", "Lu1c;", "Ly93;", "I"),
        filters = listOf(
            methodCall(smali = "Lyz7;->c0(I)Lyz7;"),
            fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lah1;->a:Ln24;"),
            methodCall(smali = "Luvg;->a(Ln24;Lu1c;Lq24;ZLy93;I)V")
        )
    ),
    Fingerprint(
        definingClass = "Ln1a;", name = "b", returnType = "V",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
        parameters = listOf("Lah1;", "Lu1c;", "Ly93;", "I"),
        filters = listOf(
            methodCall(smali = "Lyz7;->c0(I)Lyz7;"),
            fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Lah1;->a:Ln24;"),
            methodCall(smali = "Luvg;->a(Ln24;Lu1c;Lq24;ZLy93;I)V")
        )
    ),
    Fingerprint(
        definingClass = "Lnhc;", name = "j", returnType = "V",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
        parameters = listOf("Llp;", "Lu1c;", "Ly93;", "I"),
        filters = listOf(
            methodCall(smali = "Lyz7;->c0(I)Lyz7;"),
            fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Llp;->c:Ljava/lang/String;"),
            methodCall(smali = "Lt8d;->a(Ljava/lang/Object;Ljava/lang/String;Lu1c;Lnv7;Ldl3;Ly93;II)V")
        )
    )
)

internal const val HIDE_BANNER_VIEW = """
    const/16 v0, 0x8
    invoke-virtual { p1, v0 }, Landroid/view/View;->setVisibility(I)V
    return-void
"""

@Suppress("unused")
val hideBannerAdsPatch = bytecodePatch(
    name = "No banner ads",
    description = "Removes banners from the map, navigation and home screen, including the empty ad space. " +
        "Road alerts and route controls stay."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch)
    execute {
        // Resolve every target before modifying any. Six registers means p1=v4,
        // and v0 is a local: both fit the non-range invoke's four-bit register operands.
        val methods = bannerViewFingerprints.map { it.method }
        val composeMethods = composeBannerFingerprints.map { it.method }
        methods.forEach { method ->
            if (method.implementation!!.registerCount != 6) {
                throw PatchException("Yanosik banner register layout changed in ${method.definingClass}.")
            }
        }
        methods.forEach { it.addInstructions(0, HIDE_BANNER_VIEW) }
        // Exit before opening a Compose group, creating an image/layout node or
        // installing callbacks. Returning here also suppresses cached/refresh ads
        // and the initial placeholder, without changing coroutine/entitlement state.
        composeMethods.forEach { it.addInstructions(0, "return-void") }
    }
}
