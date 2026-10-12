/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.*

internal val mapThemeDescriptors = setOf(
    "Lb6i;->a()Lw5i;", "Lb6i;-><clinit>()V",
    "Lsrh;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;", "Lhyf;->invoke()Ljava/lang/Object;"
)

private const val SETTINGS = "Lapp/morphe/extension/yanosik/VibeSettings;"
private const val LISTENER = "Landroid/content/SharedPreferences\$OnSharedPreferenceChangeListener;"

internal fun verifyMapTheme(method: Method, original: Method) {
    val ops = method.implementation!!.instructions.toList()
    val (index, count) = when (method.definingClass) {
        "Lb6i;" -> if (method.name == "a") 28 to 9 else 8 to 2
        "Lsrh;" -> 980 to 6
        "Lhyf;" -> 64 to 6
        else -> error("Unexpected map theme target")
    }
    fun outline(instructions: List<Instruction>) = instructions.map {
        it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else ""
    }
    check(method.accessFlags == original.accessFlags)
    check(method.implementation!!.registerCount == original.implementation!!.registerCount)
    check(outline(ops.take(index) + ops.drop(index + count)) == outline(original.implementation!!.instructions.toList()))
    if (method.definingClass == "Lb6i;" && method.name == "a") {
        verifyMapModeControlFlow(ops)
    } else if (method.definingClass == "Lb6i;") {
        val call = ops[index] as FiveRegisterInstruction
        check(call.registerCount == 1 && call.registerC == 0)
        check((ops[index] as ReferenceInstruction).reference.toString() ==
                "$SETTINGS->mapPreferenceKeys(Ljava/util/Set;)Ljava/util/Set;")
        check(ops[index + 1].opcode == Opcode.MOVE_RESULT_OBJECT &&
                (ops[index + 1] as OneRegisterInstruction).registerA == 0)
    } else {
        val contextRegister = if (method.definingClass == "Lsrh;") 3 else 1
        val listenerRegister = if (method.definingClass == "Lsrh;") 2 else 0
        val action = if (method.definingClass == "Lsrh;") "registerMapTheme" else "unregisterMapTheme"
        check((ops[index] as ReferenceInstruction).reference.toString() == "La53;->A()Lyd7;")
        check((ops[index + 1] as OneRegisterInstruction).registerA == contextRegister)
        val field = ops[index + 2] as TwoRegisterInstruction
        check(field.registerA == contextRegister && field.registerB == contextRegister)
        check((field as ReferenceInstruction).reference.toString() == "Lyd7;->g:Ljava/lang/Object;")
        check((ops[index + 3] as ReferenceInstruction).reference.toString() == "Landroid/content/Context;")
        val call = ops[index + 4] as FiveRegisterInstruction
        check(call.registerCount == 2 && call.registerC == contextRegister && call.registerD == listenerRegister)
        check((call as ReferenceInstruction).reference.toString() ==
                "$SETTINGS->$action(Landroid/content/Context;$LISTENER)V")
        val originalListener = ops[index - 1] as FiveRegisterInstruction
        check(originalListener.registerD == listenerRegister)
        check(ops[index + 5].opcode == Opcode.NOP)
    }
    println("PASS serialized map theme: ${method.definingClass}->${method.name}; native body/registers and listener lifecycle preserved")
}

private fun verifyMapModeControlFlow(ops: List<Instruction>) {
    var address = 0
    val byAddress = ops.associateBy { op -> address.also { address += op.codeUnits } }
    for (dark in listOf(false, true)) for (auto in listOf(false, true))
        for (day in listOf(false, true)) for (traffic in listOf(false, true)) for (buildings in listOf(false, true)) {
            val registers = arrayOfNulls<Any>(5)
            var result: Any? = null
            var pc = 0
            var returned = false
            repeat(60) {
                if (returned) return@repeat
                val op = byAddress.getValue(pc)
                val next = pc + op.codeUnits
                when (op.opcode) {
                    Opcode.NEW_INSTANCE -> registers[(op as OneRegisterInstruction).registerA] = "model"
                    Opcode.SGET_OBJECT -> registers[(op as OneRegisterInstruction).registerA] =
                        (op as ReferenceInstruction).reference.toString()
                    Opcode.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT ->
                        registers[(op as OneRegisterInstruction).registerA] = result
                    Opcode.IGET_OBJECT -> {
                        val field = op as TwoRegisterInstruction
                        check((op as ReferenceInstruction).reference.toString() == "Lyd7;->g:Ljava/lang/Object;")
                        check(registers[field.registerB] == "preferences")
                        registers[field.registerA] = "context"
                    }
                    Opcode.INVOKE_STATIC, Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_DIRECT -> {
                        val call = op as FiveRegisterInstruction
                        when ((op as ReferenceInstruction).reference.toString()) {
                            "La53;->A()Lyd7;" -> result = "preferences"
                            "Lyd7;->n(Ljava/lang/Enum;)Z" -> {
                                check(registers[call.registerC] == "preferences")
                                result = when (registers[call.registerD]) {
                                    "Lk2f;->MAP_AUTO_DAY_NIGHT_MODE:Lk2f;" -> auto
                                    "Lk2f;->MAP_DAY_MODE:Lk2f;" -> day
                                    "Lk2f;->SHOW_TRAFFIC:Lk2f;" -> traffic
                                    "Lk2f;->MAP_BUILDINGS_3D:Lk2f;" -> buildings
                                    else -> error("Unexpected native preference")
                                }
                            }
                            "$SETTINGS->mapDark(Landroid/content/Context;)Z" -> {
                                check(call.registerCount == 1 && registers[call.registerC] == "context")
                                result = dark
                            }
                            "Lw5i;-><init>(Ldfb;ZZ)V" -> {
                                check(call.registerCount == 4 && registers[call.registerC] == "model")
                                val expected = if (dark) "NIGHT" else if (auto) "AUTO" else if (day) "DAY" else "NIGHT"
                                check(registers[call.registerD] == "Ldfb;->$expected:Ldfb;")
                                check(registers[call.registerE] == traffic && registers[call.registerF] == buildings)
                            }
                            else -> error("Unexpected map theme call")
                        }
                    }
                    Opcode.IF_EQZ -> {
                        pc = if (registers[(op as OneRegisterInstruction).registerA] == false)
                            pc + (op as OffsetInstruction).codeOffset else next
                        return@repeat
                    }
                    Opcode.GOTO -> { pc += (op as OffsetInstruction).codeOffset; return@repeat }
                    Opcode.RETURN_OBJECT -> {
                        check(registers[(op as OneRegisterInstruction).registerA] == "model")
                        returned = true
                    }
                    Opcode.CHECK_CAST, Opcode.NOP -> Unit
                    else -> error("Unexpected map theme opcode: ${op.opcode}")
                }
                pc = next
            }
            check(returned)
        }
    println("PASS all 32 serialized map theme combinations: dark override, original auto/day/night, traffic and 3D flags")
}
