/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object SettingsDonationObserverFingerprint : Fingerprint(
    definingClass = "Lpl/neptis/features/settings/AppPreferenceActivity;",
    name = "setupNoAdvSection\$lambda\$0\$2",
    returnType = "Ldzk;", parameters = listOf("Ldh;", "Lgzm;"),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(smali = "Lgzm;->p()Lazm;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Ldh;->F:Landroid/widget/RelativeLayout;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, smali = "Ldh;->G:Landroid/view/View;"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Ldzk;->a:Ldzk;")
    )
)

internal fun hideSettingsDonationObserver(method: MutableMethod) {
    val implementation = method.implementation
        ?: throw PatchException("Yanosik settings donation observer has no implementation.")
    val instructions = implementation.instructions.toList()
    fun reference(index: Int) = (instructions.getOrNull(index) as? ReferenceInstruction)?.reference.toString()
    if (implementation.registerCount != 3 || method.accessFlags != 26 || instructions.size != 13 ||
        instructions.first().opcode != Opcode.INVOKE_VIRTUAL || reference(0) != "Lgzm;->p()Lazm;" ||
        reference(2) != "Lazm;->YANOSIK_WITHOUT_ADVERTS:Lazm;" ||
        reference(7) != "Ldh;->F:Landroid/widget/RelativeLayout;" ||
        reference(9) != "Ldh;->G:Landroid/view/View;" || reference(11) != "Ldzk;->a:Ldzk;" ||
        instructions.last().opcode != Opcode.RETURN_OBJECT) {
        throw PatchException("Yanosik settings donation observer layout changed; refusing to patch.")
    }
    // setupNoAdvSection hides the coffee/PRO row and its divider before observing.
    // Stop this observer from revealing them again. v0 is local; both inputs survive.
    method.addInstructions(0, """
        sget-object v0, Ldzk;->a:Ldzk;
        return-object v0
    """)
}

internal fun hideSettingsDonationFooter(document: Document) {
    val elements = document.getElementsByTagName("*")
    val targets = (0 until elements.length).map { elements.item(it) as Element }
        .filter { it.getAttribute("android:id") == "@id/donateContainer" }
    val container = targets.singleOrNull()
        ?: throw PatchException("Expected one Yanosik About donation footer.")
    val children = (0 until container.childNodes.length).mapNotNull { container.childNodes.item(it) as? Element }
    if (container.tagName != "LinearLayout" ||
        (container.parentNode as? Element)?.tagName != "androidx.constraintlayout.widget.ConstraintLayout" ||
        children.size != 2 || children[0].tagName != "TextView" ||
        children[0].getAttribute("android:text") != "@string/about_coffee_text" ||
        children[1].tagName != "include" || children[1].getAttribute("android:id") != "@id/coffee_button_layout" ||
        children[1].getAttribute("layout") != "@layout/coffee_donate_button") {
        throw PatchException("Yanosik About donation footer layout changed; refusing to patch.")
    }
    // Retain the binding's child IDs. ConstraintLayout collapses this entire footer
    // while the version/update/legal/report controls above keep their normal layout.
    container.setAttribute("android:visibility", "gone")
}

internal val hideSettingsDonationResourcesPatch = resourcePatch {
    dependsOn(referenceVersionPatch)
    execute {
        document("res/layout/activity_about_program.xml").use { hideSettingsDonationFooter(it) }
    }
}
