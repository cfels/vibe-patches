/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val SETTINGS = "Lapp/morphe/extension/yanosik/VibeSettings;"
private const val COLORS = "Lapp/morphe/extension/yanosik/VibeColors;"
private const val CLICK = "Lapp/morphe/extension/yanosik/VibeSettings\$TileClick;"
private const val TOGGLE = "Lapp/morphe/extension/yanosik/VibeSettings\$DarkToggle;"
private const val OBSERVER = "Lapp/morphe/extension/yanosik/VibeSettings\$ThemeObserver;"
private const val GENERATED_R = "Lapp/morphe/extension/yanosik/R;"
private const val OUTLINE_0 = "Lapp/morphe/extension/yanosik/VibeSettings\$\$ExternalSyntheticApiModelOutline0;"
private const val OUTLINE_1 = "Lapp/morphe/extension/yanosik/VibeSettings\$\$ExternalSyntheticApiModelOutline1;"

internal val vibeSettingsExtensionDescriptors = setOf(
    "$COLORS-><init>()V", "$COLORS->compose(JJ)J", "$COLORS->isDarkText(I)Z",
    "$COLORS->isLightSurface(I)Z", "$COLORS->text(I)I",
    "$SETTINGS-><init>()V", "$SETTINGS->activity(Landroid/content/Context;)Landroid/app/Activity;",
    "$SETTINGS->applyTree(Landroid/view/View;)V",
    "$SETTINGS->bindTile(Landroid/view/View;Landroid/view/View;Landroid/view/View;I)V",
    "$SETTINGS->darkComposeColor(JJ)J", "$SETTINGS->install(Landroid/app/Activity;)V",
    "$SETTINGS->preferences(Landroid/content/Context;)Landroid/content/SharedPreferences;",
    "$SETTINGS->showSettings(Landroid/content/Context;)V", "$SETTINGS->useDark(Landroid/content/Context;)Z",
    "$CLICK-><init>(Landroid/app/AlertDialog;)V", "$CLICK->onClick(Landroid/view/View;)V",
    "$TOGGLE-><init>(Landroid/content/Context;Landroid/app/Activity;Landroid/app/AlertDialog;)V",
    "$TOGGLE->onCheckedChanged(Landroid/widget/CompoundButton;Z)V",
    "$OBSERVER-><init>(Landroid/app/Activity;Landroid/view/View;)V",
    "$OBSERVER->onGlobalLayout()V", "$OBSERVER->onViewAttachedToWindow(Landroid/view/View;)V",
    "$OBSERVER->onViewDetachedFromWindow(Landroid/view/View;)V", "$OBSERVER->run()V",
    "$GENERATED_R-><init>()V",
    "$OUTLINE_0->m(Landroid/view/Window;)Landroid/view/WindowInsetsController;",
    "$OUTLINE_1->m(Landroid/view/WindowInsetsController;II)V"
)

internal fun verifyVibeSettingsExtension(methods: Collection<Method>) {
    fun descriptor(method: Method) = "${method.definingClass}->${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}"
    val byDescriptor = methods.associateBy(::descriptor)
    check(byDescriptor.keys == vibeSettingsExtensionDescriptors) {
        "Vibe settings extension method set changed: missing=${vibeSettingsExtensionDescriptors - byDescriptor.keys}, " +
                "added=${byDescriptor.keys - vibeSettingsExtensionDescriptors}"
    }
    check(methods.size == 26 && methods.map { it.definingClass }.toSet() ==
            setOf(SETTINGS, COLORS, CLICK, TOGGLE, OBSERVER, GENERATED_R, OUTLINE_0, OUTLINE_1))
    fun references(key: String) = byDescriptor.getValue(key).implementation!!.instructions
        .filterIsInstance<ReferenceInstruction>().map { it.reference.toString() }.toList()
    check(references("$GENERATED_R-><init>()V") == listOf("Ljava/lang/Object;-><init>()V"))
    check(byDescriptor.getValue("$GENERATED_R-><init>()V").implementation!!.instructions.map { it.opcode } ==
            listOf(Opcode.INVOKE_DIRECT, Opcode.RETURN_VOID))
    check(references("$OUTLINE_0->m(Landroid/view/Window;)Landroid/view/WindowInsetsController;") ==
            listOf("Landroid/view/Window;->getInsetsController()Landroid/view/WindowInsetsController;"))
    check(references("$OUTLINE_1->m(Landroid/view/WindowInsetsController;II)V") ==
            listOf("Landroid/view/WindowInsetsController;->setSystemBarsAppearance(II)V"))
    val gate = "$SETTINGS->useDark(Landroid/content/Context;)Z"
    val gateRefs = references(gate)
    check("pl.neptis.yanosik.mobi.android.dashboard.dashboardpack.activity.DashboardPackActivity" in gateRefs)
    check("pl.neptis.features.login.AccountStartActivity" in gateRefs)
    check("dark_home_sidebar" in gateRefs)
    check("Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z" in gateRefs)
    val gateInstructions = byDescriptor.getValue(gate).implementation!!.instructions.toList()
    val preferenceIndex = gateInstructions.indexOfFirst {
        (it as? ReferenceInstruction)?.reference.toString() ==
                "Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z"
    }
    val preferenceCall = gateInstructions[preferenceIndex] as FiveRegisterInstruction
    check(preferenceCall.registerCount == 3)
    check(gateInstructions.take(preferenceIndex).any {
        it.opcode == Opcode.CONST_4 && (it as OneRegisterInstruction).registerA == preferenceCall.registerE &&
                (it as NarrowLiteralInstruction).narrowLiteral == 1
    }) { "Dark mode must default on" }
    check("vibe_settings" in references("$SETTINGS->preferences(Landroid/content/Context;)Landroid/content/SharedPreferences;"))
    val toggleRefs = references("$TOGGLE->onCheckedChanged(Landroid/widget/CompoundButton;Z)V")
    val commit = toggleRefs.indexOf("Landroid/content/SharedPreferences\$Editor;->commit()Z")
    val recreate = toggleRefs.indexOf("Landroid/app/Activity;->recreate()V")
    check(commit >= 0 && recreate > commit && "dark_home_sidebar" in toggleRefs)
    val tileRefs = references("$SETTINGS->bindTile(Landroid/view/View;Landroid/view/View;Landroid/view/View;I)V")
    check("Vibe settings" in tileRefs)
    check("Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V" in tileRefs &&
            tileRefs.any { it.startsWith("Landroid/graphics/drawable/LayerDrawable;-><init>") })
    check(tileRefs.none { it.contains("setPadding") || it.contains("setLayoutParams") })
    check(tileRefs.none { it.contains("setBackground") || it.contains("setTypeface") })
    val dialogRefs = references("$SETTINGS->showSettings(Landroid/content/Context;)V")
    check("Vibe settings" in dialogRefs && "Dark mode" in dialogRefs)
    check("Appearance" in dialogRefs && "Done" in dialogRefs)
    check("Your choice is saved on this device." !in dialogRefs)
    check("$SETTINGS->showSettings(Landroid/content/Context;)V" in references("$CLICK->onClick(Landroid/view/View;)V"))
    val allReferences = methods.flatMap { method -> method.implementation!!.instructions
        .filterIsInstance<ReferenceInstruction>().map { it.reference.toString() } }
    check(allReferences.none {
        it.startsWith("Ljava/net/") || it.startsWith("Lokhttp") || it.startsWith("Lcom/google/") ||
                it.startsWith("Lpl/neptis/") || it.contains("startActivity") || it.contains("setDefaultNightMode")
    }) { "Local settings must not call network/account/purchase code or change the global app theme" }
    println("PASS serialized Vibe settings extension: exact 26 methods/8 classes; home/login gate, default on, persistent local toggle")
}
