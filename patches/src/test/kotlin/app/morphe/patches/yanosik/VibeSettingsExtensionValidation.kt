/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val SETTINGS = "Lapp/morphe/extension/yanosik/VibeSettings;"
private const val COLORS = "Lapp/morphe/extension/yanosik/VibeColors;"
private const val SWITCH = "Lapp/morphe/extension/yanosik/VibeSwitch;"
private const val SWITCH_PART = "Lapp/morphe/extension/yanosik/VibeSwitch\$SwitchPart;"
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
    "$SETTINGS->applySystemBars(Landroid/app/Activity;)V",
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
    "$SWITCH-><init>(Landroid/content/Context;Z)V",
    "$SWITCH_PART-><init>(FZZ)V", "$SWITCH_PART->draw(Landroid/graphics/Canvas;)V",
    "$SWITCH_PART->getIntrinsicHeight()I", "$SWITCH_PART->getIntrinsicWidth()I",
    "$SWITCH_PART->getOpacity()I", "$SWITCH_PART->getPadding(Landroid/graphics/Rect;)Z",
    "$SWITCH_PART->isStateful()Z", "$SWITCH_PART->onStateChange([I)Z",
    "$SWITCH_PART->setAlpha(I)V", "$SWITCH_PART->setColorFilter(Landroid/graphics/ColorFilter;)V",
    "$GENERATED_R-><init>()V",
    "$OUTLINE_0->m(Landroid/view/Window;)Landroid/view/WindowInsetsController;",
    "$OUTLINE_1->m(Landroid/view/WindowInsetsController;II)V"
)

internal fun verifyVibeSettingsTargets(methods: Collection<Method>) {
    val required = setOf(
        "Lcom/google/android/material/textfield/TextInputLayout;->setDefaultHintTextColor(Landroid/content/res/ColorStateList;)V",
        "Lcom/google/android/material/textfield/TextInputLayout;->setHintTextColor(Landroid/content/res/ColorStateList;)V",
        "Lcom/google/android/material/textfield/TextInputLayout;->setBoxStrokeColorStateList(Landroid/content/res/ColorStateList;)V",
        "Lcom/google/android/material/textfield/TextInputLayout;->setEndIconTintList(Landroid/content/res/ColorStateList;)V"
    )
    val publicMethods = methods.filter { AccessFlags.PUBLIC.isSet(it.accessFlags) }.map {
        "${it.definingClass}->${it.name}(${it.parameterTypes.joinToString("")})${it.returnType}"
    }.toSet()
    check(publicMethods.containsAll(required)) { "Yanosik Material APIs missing: ${required - publicMethods}" }
    println("PASS original Yanosik login-field APIs")
}

internal fun verifyVibeSettingsExtension(methods: Collection<Method>) {
    fun descriptor(method: Method) = "${method.definingClass}->${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}"
    val byDescriptor = methods.associateBy(::descriptor)
    val outlines = vibeSettingsExtensionDescriptors.filter { it.startsWith(OUTLINE_0) || it.startsWith(OUTLINE_1) }.toSet()
    val presentOutlines = byDescriptor.keys.intersect(outlines)
    check(presentOutlines.isEmpty() || presentOutlines == outlines)
    val expected = vibeSettingsExtensionDescriptors - outlines + presentOutlines
    check(byDescriptor.keys == expected) {
        "Vibe settings extension method set changed: missing=${vibeSettingsExtensionDescriptors - byDescriptor.keys}, " +
                "added=${byDescriptor.keys - vibeSettingsExtensionDescriptors}"
    }
    val expectedClasses = setOf(SETTINGS, COLORS, SWITCH, SWITCH_PART, CLICK, TOGGLE, OBSERVER, GENERATED_R) +
            if (presentOutlines.isEmpty()) emptySet() else setOf(OUTLINE_0, OUTLINE_1)
    check(methods.map { it.definingClass }.toSet() == expectedClasses)
    fun references(key: String) = byDescriptor.getValue(key).implementation!!.instructions
        .filterIsInstance<ReferenceInstruction>().map { it.reference.toString() }.toList()
    check(references("$GENERATED_R-><init>()V") == listOf("Ljava/lang/Object;-><init>()V"))
    check(byDescriptor.getValue("$GENERATED_R-><init>()V").implementation!!.instructions.map { it.opcode } ==
            listOf(Opcode.INVOKE_DIRECT, Opcode.RETURN_VOID))
    if (presentOutlines.isEmpty()) {
        val barRefs = references("$SETTINGS->applySystemBars(Landroid/app/Activity;)V")
        check("Landroid/view/Window;->getInsetsController()Landroid/view/WindowInsetsController;" in barRefs)
        check("Landroid/view/WindowInsetsController;->setSystemBarsAppearance(II)V" in barRefs)
    } else {
        check(references("$OUTLINE_0->m(Landroid/view/Window;)Landroid/view/WindowInsetsController;") ==
                listOf("Landroid/view/Window;->getInsetsController()Landroid/view/WindowInsetsController;"))
        check(references("$OUTLINE_1->m(Landroid/view/WindowInsetsController;II)V") ==
                listOf("Landroid/view/WindowInsetsController;->setSystemBarsAppearance(II)V"))
    }
    val barHelper = "$SETTINGS->applySystemBars(Landroid/app/Activity;)V"
    check(barHelper in references("$SETTINGS->install(Landroid/app/Activity;)V"))
    check(barHelper in references("$OBSERVER->run()V"))
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
    check("Appearance" !in dialogRefs && "Done" in dialogRefs)
    check("Your choice is saved on this device." !in dialogRefs)
    check("$SWITCH-><init>(Landroid/content/Context;Z)V" in dialogRefs)
    check(dialogRefs.none { it.startsWith("Landroid/widget/Switch;") })
    val switchRefs = references("$SWITCH-><init>(Landroid/content/Context;Z)V")
    check("Landroid/widget/Switch;-><init>(Landroid/content/Context;)V" in switchRefs)
    check("$SWITCH->setThumbDrawable(Landroid/graphics/drawable/Drawable;)V" in switchRefs)
    check("$SWITCH->setTrackDrawable(Landroid/graphics/drawable/Drawable;)V" in switchRefs)
    check("$SWITCH_PART-><init>(FZZ)V" in switchRefs)
    val drawRefs = references("$SWITCH_PART->draw(Landroid/graphics/Canvas;)V")
    check("Landroid/graphics/Canvas;->drawCircle(FFFLandroid/graphics/Paint;)V" in drawRefs)
    check("Landroid/graphics/Canvas;->drawRoundRect(Landroid/graphics/RectF;FFLandroid/graphics/Paint;)V" in drawRefs)
    val treeRefs = references("$SETTINGS->applyTree(Landroid/view/View;)V")
    check("google_log_in" in treeRefs && "button" in treeRefs)
    check("Ljava/lang/String;->contentEquals(Ljava/lang/CharSequence;)Z" in treeRefs)
    val treeInstructions = byDescriptor.getValue("$SETTINGS->applyTree(Landroid/view/View;)V").implementation!!.instructions.toList()
    val googleMatch = treeInstructions.indexOfFirst {
        (it as? ReferenceInstruction)?.reference.toString() == "Ljava/lang/String;->contentEquals(Ljava/lang/CharSequence;)Z"
    }
    check(treeInstructions.drop(googleMatch + 1).take(3).map { it.opcode } ==
            listOf(Opcode.MOVE_RESULT, Opcode.IF_EQZ, Opcode.RETURN_VOID)) {
        "The matching Google button must return before any background/text/icon conversion"
    }
    check("setHintTextColor" in treeRefs && "setBoxStrokeColorStateList" in treeRefs)
    check("$SETTINGS->showSettings(Landroid/content/Context;)V" in references("$CLICK->onClick(Landroid/view/View;)V"))
    val allReferences = methods.flatMap { method -> method.implementation!!.instructions
        .filterIsInstance<ReferenceInstruction>().map { it.reference.toString() } }
    check(allReferences.none {
        it.startsWith("Ljava/net/") || it.startsWith("Lokhttp") || it.startsWith("Lcom/google/") ||
                it.startsWith("Lpl/neptis/") || it.contains("startActivity") || it.contains("setDefaultNightMode")
    }) { "Local settings must not call network/account/purchase code or change the global app theme" }
    println("PASS serialized Vibe settings extension: exact ${methods.size} methods/${expectedClasses.size} classes; Material 3 switch styling, Google button exception, home/login gate, persistent local toggle")
}
