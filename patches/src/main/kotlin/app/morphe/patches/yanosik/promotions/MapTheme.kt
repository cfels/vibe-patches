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
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal object MapThemeFingerprint : Fingerprint(
    definingClass = "Lb6i;", name = "a", returnType = "Lw5i;", parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    filters = listOf(
        fieldAccess(smali = "Lk2f;->MAP_AUTO_DAY_NIGHT_MODE:Lk2f;"),
        fieldAccess(smali = "Lk2f;->MAP_DAY_MODE:Lk2f;"),
        methodCall(smali = "Lw5i;-><init>(Ldfb;ZZ)V")
    )
)

internal object MapThemeKeysFingerprint : Fingerprint(
    definingClass = "Lb6i;", name = "<clinit>", returnType = "V", parameters = emptyList(),
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        methodCall(smali = "Llv0;->L0([Ljava/lang/Object;)Ljava/util/Set;"),
        fieldAccess(smali = "Lb6i;->t:Ljava/util/Set;")
    )
)

internal object MapThemeRegisterFingerprint : Fingerprint(
    definingClass = "Lsrh;", name = "invokeSuspend", returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"), accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        fieldAccess(smali = "Lb6i;->f:Logj;"),
        methodCall(smali = "Landroid/content/SharedPreferences;->registerOnSharedPreferenceChangeListener(Landroid/content/SharedPreferences\$OnSharedPreferenceChangeListener;)V"),
        methodCall(smali = "Lb6i;->a()Lw5i;")
    )
)

internal object MapThemeUnregisterFingerprint : Fingerprint(
    definingClass = "Lhyf;", name = "invoke", returnType = "Ljava/lang/Object;", parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        fieldAccess(smali = "Lb6i;->f:Logj;"),
        methodCall(smali = "Landroid/content/SharedPreferences;->unregisterOnSharedPreferenceChangeListener(Landroid/content/SharedPreferences\$OnSharedPreferenceChangeListener;)V")
    )
)

internal object MapThemeContextFingerprint : Fingerprint(
    definingClass = "Lyd7;", name = "o", returnType = "Z", parameters = listOf("Ljava/lang/Enum;", "Z"),
    accessFlags = listOf(AccessFlags.PUBLIC),
    filters = listOf(
        fieldAccess(smali = "Lyd7;->g:Ljava/lang/Object;"),
        methodCall(smali = "Lyd7;->z(Landroid/content/Context;)Landroid/content/SharedPreferences;"),
        methodCall(smali = "Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z")
    )
)

private fun MutableMethod.requireLayout(registers: Int, size: Int, index: Int, reference: String) {
    if (implementation!!.registerCount != registers || implementation!!.instructions.size != size ||
        (getInstruction(index) as? ReferenceInstruction)?.reference.toString() != reference)
        throw PatchException("Yanosik map theme layout changed in $definingClass->$name; refusing to patch.")
}

internal fun installMapTheme(mode: MutableMethod, keys: MutableMethod, register: MutableMethod, unregister: MutableMethod) {
    mode.requireLayout(5, 30, 18, "La53;->A()Lyd7;")
    keys.requireLayout(4, 10, 8, "Lb6i;->t:Ljava/util/Set;")
    register.requireLayout(23, 1041, 979,
        "Landroid/content/SharedPreferences;->registerOnSharedPreferenceChangeListener(Landroid/content/SharedPreferences\$OnSharedPreferenceChangeListener;)V")
    unregister.requireLayout(30, 586, 63,
        "Landroid/content/SharedPreferences;->unregisterOnSharedPreferenceChangeListener(Landroid/content/SharedPreferences\$OnSharedPreferenceChangeListener;)V")
    val registration = register.getInstruction<FiveRegisterInstruction>(979)
    val cancellation = unregister.getInstruction<FiveRegisterInstruction>(63)
    val modeConstructor = mode.getInstruction<FiveRegisterInstruction>(28)
    val keyWrite = keys.getInstruction<OneRegisterInstruction>(8)
    if (registration.registerCount != 2 || registration.registerC != 3 || registration.registerD != 2 ||
        cancellation.registerCount != 2 || cancellation.registerC != 1 || cancellation.registerD != 0 ||
        modeConstructor.registerCount != 4 || modeConstructor.registerC != 0 || modeConstructor.registerD != 1 ||
        modeConstructor.registerE != 2 || modeConstructor.registerF != 3 || keyWrite.registerA != 0 ||
        mode.getInstruction(29).opcode != Opcode.RETURN_OBJECT)
        throw PatchException("Yanosik map theme operands changed; refusing to patch.")

    mode.addInstructionsWithLabels(28, """
        invoke-static {}, La53;->A()Lyd7;
        move-result-object v4
        iget-object v4, v4, Lyd7;->g:Ljava/lang/Object;
        check-cast v4, Landroid/content/Context;
        invoke-static { v4 }, $VIBE_SETTINGS_CLASS->mapDark(Landroid/content/Context;)Z
        move-result v4
        if-eqz v4, :morphe_original_map_mode
        sget-object v1, Ldfb;->NIGHT:Ldfb;
        :morphe_original_map_mode
        nop
    """)
    keys.addInstructions(8, """
        invoke-static { v0 }, $VIBE_SETTINGS_CLASS->mapPreferenceKeys(Ljava/util/Set;)Ljava/util/Set;
        move-result-object v0
    """)
    register.addInstructions(980, """
        invoke-static {}, La53;->A()Lyd7;
        move-result-object v3
        iget-object v3, v3, Lyd7;->g:Ljava/lang/Object;
        check-cast v3, Landroid/content/Context;
        invoke-static { v3, v2 }, $VIBE_SETTINGS_CLASS->registerMapTheme(Landroid/content/Context;Landroid/content/SharedPreferences${'$'}OnSharedPreferenceChangeListener;)V
        nop
    """)
    unregister.addInstructions(64, """
        invoke-static {}, La53;->A()Lyd7;
        move-result-object v1
        iget-object v1, v1, Lyd7;->g:Ljava/lang/Object;
        check-cast v1, Landroid/content/Context;
        invoke-static { v1, v0 }, $VIBE_SETTINGS_CLASS->unregisterMapTheme(Landroid/content/Context;Landroid/content/SharedPreferences${'$'}OnSharedPreferenceChangeListener;)V
        nop
    """)
}
