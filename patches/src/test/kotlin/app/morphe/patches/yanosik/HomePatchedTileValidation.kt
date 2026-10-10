/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal fun verifyHomePatchedTileGuard(method: Method, original: Method) {
    check(method.definingClass == "Lso8;" && method.name == "n" && method.accessFlags == 17)
    check(method.implementation!!.registerCount == 10 && original.implementation!!.registerCount == 10)
    val ops = method.implementation!!.instructions.toList()
    val before = original.implementation!!.instructions.toList()
    val startIndex = 101
    val count = 35
    fun outline(instructions: List<Instruction>) = instructions.map {
        it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else ""
    }
    check(outline(ops.take(startIndex) + ops.drop(startIndex + count)) == outline(before)) {
        "Home tile patch changed code outside its renderer guard"
    }
    check(ops[startIndex + 33].opcode == Opcode.RETURN_VOID)
    check(ops[startIndex + 34].opcode == Opcode.NOP)
    check(ops[startIndex + count].opcode == Opcode.IF_EQZ &&
        (ops[startIndex + count] as OneRegisterInstruction).registerA == 7)
    val addresses = IntArray(ops.size)
    var address = 0
    val byAddress = ops.mapIndexed { index, op ->
        addresses[index] = address
        (address to op).also { address += op.codeUnits }
    }.toMap()
    for (action in (0..255).toList() + listOf(-1, Int.MIN_VALUE, Int.MAX_VALUE, null)) {
        val registers = Array<Any?>(10) { "original:$it" }.apply {
            this[2] = "binding"
            this[3] = 0
            this[4] = 8
            this[7] = action?.let { "service" }
        }
        val saved = registers.copyOf()
        val effects = linkedMapOf<String, Any?>()
        var pc = addresses[startIndex]
        var result: Any? = null
        var replaced = false
        var steps = 0
        while (pc != addresses[startIndex + count] && !replaced) {
            check(++steps <= 40)
            val op = byAddress.getValue(pc)
            val next = pc + op.codeUnits
            when (op.opcode) {
                Opcode.IF_EQZ -> {
                    pc = if (registers[(op as OneRegisterInstruction).registerA] == null)
                        pc + (op as OffsetInstruction).codeOffset else next
                }
                Opcode.IF_EQ, Opcode.IF_NE -> {
                    val branch = op as TwoRegisterInstruction
                    val equal = registers[branch.registerA] == registers[branch.registerB]
                    pc = if (if (op.opcode == Opcode.IF_EQ) equal else !equal)
                        pc + (op as OffsetInstruction).codeOffset else next
                }
                Opcode.CONST_16 -> {
                    registers[(op as OneRegisterInstruction).registerA] = (op as NarrowLiteralInstruction).narrowLiteral
                    pc = next
                }
                Opcode.CONST_STRING -> {
                    registers[(op as OneRegisterInstruction).registerA] =
                        ((op as ReferenceInstruction).reference as StringReference).string
                    pc = next
                }
                Opcode.IGET, Opcode.IGET_OBJECT -> {
                    val read = op as TwoRegisterInstruction
                    val reference = (op as ReferenceInstruction).reference.toString()
                    val receiver = registers[read.registerB]
                    registers[read.registerA] = when (reference) {
                        "Lco8;->c:I" -> { check(receiver == "service"); action }
                        "Lco8;->h:I" -> { check(receiver == "service"); -1234567 }
                        "Lpf;->b:Landroid/view/View;" -> { check(receiver == "binding"); "image" }
                        "Lpf;->c:Landroid/view/View;" -> { check(receiver == "binding"); "text" }
                        "Lpf;->d:Landroid/view/View;" -> { check(receiver == "binding"); "tile" }
                        "Lpf;->f:Landroid/view/View;" -> { check(receiver == "binding"); "badge" }
                        "Lpf;->g:Landroid/view/View;" -> { check(receiver == "binding"); "folder" }
                        else -> error("Unexpected patched tile field: $reference")
                    }
                    pc = next
                }
                Opcode.CHECK_CAST -> pc = next
                Opcode.INVOKE_VIRTUAL -> {
                    val invoke = op as FiveRegisterInstruction
                    val args = listOf(invoke.registerC, invoke.registerD, invoke.registerE,
                        invoke.registerF, invoke.registerG).take(invoke.registerCount).map { registers[it] }
                    val reference = (op as ReferenceInstruction).reference.toString()
                    result = when (reference) {
                        "Landroid/widget/TextView;->setTextColor(I)V" -> {
                            check(args == listOf("text", -1234567)); effects["color"] = args[1]; null
                        }
                        "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V" -> {
                            check(args == listOf("text", "Vibe Patched")); effects["label"] = args[1]; null
                        }
                        "Landroid/view/View;->getContext()Landroid/content/Context;" -> {
                            check(args == listOf("image")); "context"
                        }
                        "Landroid/content/Context;->getResources()Landroid/content/res/Resources;" -> {
                            check(args == listOf("context")); "resources"
                        }
                        "Landroid/content/Context;->getPackageName()Ljava/lang/String;" -> {
                            check(args == listOf("context")); "pl.neptis.yanosik.mobi.android"
                        }
                        "Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I" -> {
                            check(args == listOf("resources", "vibe_patched", "drawable", "pl.neptis.yanosik.mobi.android"))
                            0x7f999001
                        }
                        "Landroid/widget/ImageView;->setImageResource(I)V" -> {
                            check(args == listOf("image", 0x7f999001)); effects["image"] = args[1]; null
                        }
                        "Landroid/view/View;->setOnClickListener(Landroid/view/View\$OnClickListener;)V" -> {
                            check(args == listOf("tile", 0)); effects["listener"] = null; null
                        }
                        "Landroid/view/View;->setClickable(Z)V" -> {
                            check(args == listOf("tile", 0)); effects["clickable"] = false; null
                        }
                        "Landroid/view/View;->setVisibility(I)V" -> {
                            check(args in listOf(listOf("image", 0), listOf("badge", 8), listOf("folder", 8)))
                            effects["${args[0]} visibility"] = args[1]; null
                        }
                        else -> error("Unexpected patched tile call: $reference")
                    }
                    pc = next
                }
                Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT -> {
                    registers[(op as OneRegisterInstruction).registerA] = result
                    pc = next
                }
                Opcode.RETURN_VOID -> replaced = true
                Opcode.NOP -> pc = next
                else -> error("Patched tile guard escaped into application code: ${op.opcode}")
            }
        }
        check(replaced == (action == 68 || action == 70)) { "Incorrect patched tile action: $action" }
        if (replaced) {
            check(effects.keys == setOf("color", "label", "image", "image visibility",
                "listener", "clickable", "badge visibility", "folder visibility"))
            check(registers[2] == saved[2] && registers[7] == saved[7] && registers[8] == saved[8])
        } else {
            check(effects.isEmpty())
            for (slot in registers.indices.filter { it !in setOf(5, 6) }) {
                check(registers[slot] == saved[slot]) { "Normal home tile path clobbered v$slot" }
            }
        }
    }
    println("PASS serialized home PRO tile: Vibe Patched icon/label, inert tap; other actions/null unchanged")
}
