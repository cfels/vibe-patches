/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.*

private fun themeOutline(instructions: List<Instruction>) = instructions.map {
    it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else ""
}

internal fun verifyAccountThemeInstall(method: Method, original: Method) {
    check(method.definingClass == "Lpl/neptis/features/login/AccountStartActivity;")
    check(method.name == "onCreate" && method.accessFlags == 17 && method.parameterTypes == listOf("Landroid/os/Bundle;"))
    check(method.implementation!!.registerCount == 8)
    val ops = method.implementation!!.instructions.toList()
    val call = ops[30]
    check(call.opcode == Opcode.INVOKE_STATIC)
    check((call as ReferenceInstruction).reference.toString() ==
            "Lapp/morphe/extension/yanosik/VibeSettings;->install(Landroid/app/Activity;)V")
    check((call as FiveRegisterInstruction).registerCount == 1 && call.registerC == 6)
    check((ops[29] as ReferenceInstruction).reference.toString() == "Lo0;->setContentView(Landroid/view/View;)V")
    check(ops[31].opcode == Opcode.IF_NEZ && (ops[31] as OneRegisterInstruction).registerA == 7)
    check(themeOutline(ops.take(30) + ops.drop(31)) == themeOutline(original.implementation!!.instructions.toList()))
    println("PASS serialized login/start host theme: after original content view; form, authentication and lifecycle bodies unchanged")
}

internal fun verifyHomeThemeInstall(method: Method, original: Method) {
    check(method.definingClass == "Lpl/neptis/yanosik/mobi/android/dashboard/dashboardpack/activity/DashboardPackActivity;")
    check(method.name == "onCreate" && method.accessFlags == 17)
    check(method.implementation!!.registerCount == 22)
    val ops = method.implementation!!.instructions.toList()
    val injected = ops[25]
    check(injected.opcode == Opcode.INVOKE_STATIC_RANGE)
    check((injected as ReferenceInstruction).reference.toString() ==
            "Lapp/morphe/extension/yanosik/VibeSettings;->install(Landroid/app/Activity;)V")
    check((injected as RegisterRangeInstruction).startRegister == 20 && injected.registerCount == 1)
    check(themeOutline(ops.take(25) + ops.drop(26)) == themeOutline(original.implementation!!.instructions.toList()))
    check((ops[24] as ReferenceInstruction).reference.toString() == "Lo0;->setContentView(Landroid/view/View;)V")
    println("PASS serialized dashboard theme installation: after original content view; lifecycle and registers unchanged")
}

internal fun verifyHomeSidebarPalette(method: Method, original: Method) {
    check(method.definingClass == "Ly8;" && method.name == "c" && method.accessFlags == 25)
    check(method.parameterTypes == listOf("Ll44;", "Ly93;") && method.returnType == "J")
    check(method.implementation!!.registerCount == 5)
    val ops = method.implementation!!.instructions.toList()
    check(ops.size == 48)
    check(themeOutline(ops.drop(21)) == themeOutline(original.implementation!!.instructions.toList()))
    val pcs = IntArray(ops.size)
    var address = 0
    val byAddress = ops.mapIndexed { index, op ->
        pcs[index] = address
        (address to op).also { address += op.codeUnits }
    }.toMap()
    // All combinations: exact dashboard/non-dashboard activity and persisted toggle.
    // useDark must be the sole gate; test both its possible results and both color operands.
    for (enabled in listOf(false, true)) for (palette in listOf("ordinary", "map title", "map description")) {
        val registers = arrayOf<Any?>("saved0", "saved1", "saved2", palette, "composer")
        val effects = mutableListOf<String>()
        var pc = 0
        var result: Any? = null
        var returned = false
        var steps = 0
        while (pc != pcs[21] && !returned) {
            check(++steps <= 21)
            val op = byAddress.getValue(pc)
            val next = pc + op.codeUnits
            when (op.opcode) {
                Opcode.CHECK_CAST -> {
                    val cast = op as OneRegisterInstruction
                    val type = (op as ReferenceInstruction).reference.toString()
                    check((cast.registerA == 4 && type == "Lyz7;" && registers[4] == "composer") ||
                            (cast.registerA == 0 && type == "Landroid/content/Context;" && registers[0] == "context"))
                    pc = next
                }
                Opcode.SGET_OBJECT -> {
                    registers[(op as OneRegisterInstruction).registerA] = when (
                        val field = (op as ReferenceInstruction).reference.toString()
                    ) {
                        "Ls90;->b:Lb5j;" -> "context local"
                        "Lt44;->b:Ll44;" -> "map title"
                        "Lx44;->a:Ll44;" -> "map description"
                        else -> error("Unexpected palette identity: $field")
                    }
                    pc = next
                }
                Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_STATIC -> {
                    val invoke = op as FiveRegisterInstruction
                    val args = listOf(invoke.registerC, invoke.registerD, invoke.registerE,
                        invoke.registerF, invoke.registerG).take(invoke.registerCount).map { registers[it] }
                    val reference = (op as ReferenceInstruction).reference.toString()
                    result = when (reference) {
                        "Lyz7;->j(Lwgf;)Ljava/lang/Object;" -> {
                            check(args == listOf("composer", "context local")); effects += "context"; "context"
                        }
                        "Lapp/morphe/extension/yanosik/VibeSettings;->useDark(Landroid/content/Context;)Z" -> {
                            check(args == listOf("context")); effects += "gate"; enabled
                        }
                        "Lapp/morphe/extension/yanosik/VibeSettings;->darkComposeColor(JJ)J" -> {
                            check(args == listOf("light low", "light high", "dark low", "dark high"))
                            effects += "palette"; "converted"
                        }
                        else -> error("Unexpected theme call: $reference")
                    }
                    pc = next
                }
                Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT -> {
                    registers[(op as OneRegisterInstruction).registerA] = result; pc = next
                }
                Opcode.MOVE_RESULT_WIDE -> {
                    val register = (op as OneRegisterInstruction).registerA
                    registers[register] = result; registers[register + 1] = "converted high"; pc = next
                }
                Opcode.IGET_WIDE -> {
                    val read = op as TwoRegisterInstruction
                    check(registers[read.registerB] == palette)
                    val field = (op as ReferenceInstruction).reference.toString()
                    val color = when (field) { "Ll44;->a:J" -> "light"; "Ll44;->b:J" -> "dark"; else -> error(field) }
                    registers[read.registerA] = "$color low"; registers[read.registerA + 1] = "$color high"; pc = next
                }
                Opcode.IF_EQZ -> pc = if (registers[(op as OneRegisterInstruction).registerA] == false)
                    pc + (op as OffsetInstruction).codeOffset else next
                Opcode.IF_EQ, Opcode.IF_NE -> {
                    val branch = op as TwoRegisterInstruction
                    val equal = registers[branch.registerA] == registers[branch.registerB]
                    val taken = if (op.opcode == Opcode.IF_EQ) equal else !equal
                    pc = if (taken) pc + (op as OffsetInstruction).codeOffset else next
                }
                Opcode.RETURN_WIDE -> {
                    val register = (op as OneRegisterInstruction).registerA
                    if (palette == "ordinary") {
                        check(registers[register] == "converted" && registers[register + 1] == "converted high")
                    } else {
                        check(registers[register] == "light low" && registers[register + 1] == "light high")
                        effects += "white foreground"
                    }
                    returned = true
                }
                Opcode.NOP -> pc = next
                else -> error("Unexpected theme instruction: ${op.opcode}")
            }
        }
        check(returned == enabled)
        check(effects == if (enabled) listOf("context", "gate", if (palette == "ordinary") "palette" else "white foreground")
                else listOf("context", "gate"))
        if (!enabled) check(registers[3] == palette && registers[4] == "composer")
    }
    println("PASS serialized scoped Compose palette: activity/preference gate, exact Map foreground identities, wide arguments and original continuing path")
}
