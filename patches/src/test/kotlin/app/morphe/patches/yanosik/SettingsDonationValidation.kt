/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.yanosik.promotions.hideSettingsDonationFooter
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

internal const val settingsDonationObserverDescriptor =
    "Lpl/neptis/features/settings/AppPreferenceActivity;->setupNoAdvSection\$lambda\$0\$2(Ldh;Lgzm;)Ldzk;"

internal fun verifySettingsDonationObserver(method: Method, original: Method) {
    check(method.definingClass == "Lpl/neptis/features/settings/AppPreferenceActivity;" &&
        method.name == "setupNoAdvSection\$lambda\$0\$2" && method.parameterTypes == listOf("Ldh;", "Lgzm;") &&
        method.returnType == "Ldzk;" && method.accessFlags == 26 && original.accessFlags == 26)
    check(method.implementation!!.registerCount == 3 && original.implementation!!.registerCount == 3)
    val ops = method.implementation!!.instructions.toList()
    val before = original.implementation!!.instructions.toList()
    check(before.size == 13 && ops.size == 15)
    check(ops.take(2).map { it.opcode } == listOf(Opcode.SGET_OBJECT, Opcode.RETURN_OBJECT))
    check((ops[0] as ReferenceInstruction).reference.toString() == "Ldzk;->a:Ldzk;")
    check((ops[0] as OneRegisterInstruction).registerA == 0 &&
        (ops[1] as OneRegisterInstruction).registerA == 0)
    fun outline(instructions: List<Instruction>) = instructions.map {
        it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else ""
    }
    check(outline(ops.drop(2)) == outline(before)) { "Settings donation observer original body changed" }
    // The only reachable instructions return Unit without dereferencing either
    // input or touching a view, so cached/live subscription updates cannot show it.
    check(ops.take(2).none { it.opcode in setOf(Opcode.IGET_OBJECT, Opcode.INVOKE_VIRTUAL) })
    println("PASS serialized settings donation observer: Unit before subscription/view reads; body preserved")
}

private data class DonationLayoutNode(val name: String, val attributes: MutableMap<String, String>,
    val children: MutableList<DonationLayoutNode> = mutableListOf())

// Read the compiled layout independently of the patch's decoded DOM. Comparing
// resolved attributes also checks retained binding IDs and all unrelated controls.
private fun readDonationLayout(apk: File): DonationLayoutNode {
    val bytes = ZipFile(apk).use { zip ->
        val entry = requireNotNull(zip.getEntry("res/layout/activity_about_program.xml"))
        zip.getInputStream(entry).use { it.readBytes() }
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    fun short(offset: Int) = buffer.getShort(offset).toInt() and 0xffff
    fun int(offset: Int) = buffer.getInt(offset)
    check(short(0) == 3 && int(4) == bytes.size)
    var strings = emptyList<String>()
    var root: DonationLayoutNode? = null
    val stack = mutableListOf<DonationLayoutNode>()
    var pos = short(2)
    while (pos < bytes.size) {
        val size = int(pos + 4)
        check(size >= 8 && pos + size <= bytes.size)
        when (short(pos)) {
            1 -> {
                val utf8 = int(pos + 16) and 0x100 != 0
                val stringStart = pos + int(pos + 20)
                strings = (0 until int(pos + 8)).map { index ->
                    var cursor = stringStart + int(pos + short(pos + 2) + index * 4)
                    fun length8(): Int {
                        val first = bytes[cursor++].toInt() and 0xff
                        return if (first and 0x80 != 0)
                            ((first and 0x7f) shl 8) or (bytes[cursor++].toInt() and 0xff) else first
                    }
                    if (utf8) {
                        length8()
                        val length = length8()
                        String(bytes, cursor, length, Charsets.UTF_8)
                    } else {
                        var length = short(cursor)
                        cursor += 2
                        if (length and 0x8000 != 0) {
                            length = ((length and 0x7fff) shl 16) or short(cursor)
                            cursor += 2
                        }
                        String(bytes, cursor, length * 2, Charsets.UTF_16LE)
                    }
                }
            }
            0x102 -> {
                val attributes = mutableMapOf<String, String>()
                val start = pos + 16 + short(pos + 24)
                val stride = short(pos + 26)
                check(stride >= 20)
                repeat(short(pos + 28)) { index ->
                    val attr = start + index * stride
                    val namespace = int(attr)
                    val name = (if (namespace >= 0) "{${strings[namespace]}}" else "") + strings[int(attr + 4)]
                    val data = int(attr + 16)
                    val type = bytes[attr + 15].toInt() and 0xff
                    val value = if (type == 3) strings[data] else "typed:$type:$data"
                    check(attributes.put(name, value) == null)
                }
                val node = DonationLayoutNode(strings[int(pos + 20)], attributes)
                if (stack.isEmpty()) {
                    check(root == null)
                    root = node
                } else stack.last().children += node
                stack += node
            }
            0x103 -> stack.removeAt(stack.lastIndex)
        }
        pos += size
    }
    check(stack.isEmpty() && pos == bytes.size)
    return requireNotNull(root)
}

internal fun verifySettingsDonationResources(originalApk: File, resourcesApk: File) {
    val original = readDonationLayout(originalApk)
    val patched = readDonationLayout(resourcesApk)
    val android = "{http://schemas.android.com/apk/res/android}"
    // Verified stable public id in the supported 26.9.0 reference APK.
    val donorId = "typed:1:${0x7f0a0389}"
    fun all(node: DonationLayoutNode): List<DonationLayoutNode> = listOf(node) + node.children.flatMap(::all)
    val oldFooter = all(original).single { it.attributes["${android}id"] == donorId }
    val newFooter = all(patched).single { it.attributes["${android}id"] == donorId }
    check(oldFooter.name == "LinearLayout" && newFooter.name == "LinearLayout")
    check(oldFooter.children.size == 2 && oldFooter.children[0].name == "TextView" &&
        oldFooter.children[1].name == "include")
    // Android's visibility XML enum is gone=2 (attrs.xml); View.GONE=8 is
    // the runtime flag value, not the compiled XML attribute's enum value.
    val visibility = newFooter.attributes["${android}visibility"]
    check(visibility == "typed:16:2") {
        "Compiled About donation footer is not GONE: $visibility"
    }
    val oldVisibility = oldFooter.attributes["${android}visibility"]
    if (oldVisibility == null) newFooter.attributes.remove("${android}visibility")
    else newFooter.attributes["${android}visibility"] = oldVisibility
    check(patched == original) { "About controls/bindings/layout changed outside donation visibility" }
    println("PASS compiled About donation footer: GONE; binding children and all other layout content preserved")
}

internal fun verifySettingsDonationResourceFailures() {
    val fixture = """
        <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android">
            <Button android:id="@id/unrelatedSettingsControl" android:visibility="visible"/>
            <LinearLayout android:id="@id/donateContainer">
                <TextView android:text="@string/about_coffee_text"/>
                <include android:id="@id/coffee_button_layout" layout="@layout/coffee_donate_button"/>
            </LinearLayout>
        </androidx.constraintlayout.widget.ConstraintLayout>
    """.trimIndent()
    fun fresh(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(fixture.byteInputStream())
    fun footer(document: Document) = document.getElementsByTagName("LinearLayout").item(0) as Element
    fun include(document: Document) = document.getElementsByTagName("include").item(0) as Element
    val valid = fresh()
    hideSettingsDonationFooter(valid)
    check(footer(valid).getAttribute("android:visibility") == "gone")
    check((valid.getElementsByTagName("Button").item(0) as Element).getAttribute("android:visibility") == "visible")
    val mutations = listOf<Pair<String, (Document) -> Unit>>(
        "missing footer" to { document -> footer(document).let { it.parentNode.removeChild(it) }; Unit },
        "duplicate footer" to { document ->
            document.documentElement.appendChild(footer(document).cloneNode(true)); Unit
        },
        "different parent" to { document ->
            val wrapper = document.createElement("LinearLayout")
            val target = footer(document)
            document.documentElement.appendChild(wrapper)
            wrapper.appendChild(target); Unit
        },
        "different donation text" to { document ->
            (document.getElementsByTagName("TextView").item(0) as Element)
                .setAttribute("android:text", "@string/other_text")
        },
        "different binding id" to { document -> include(document).setAttribute("android:id", "@id/other_button") },
        "different included layout" to { document -> include(document).setAttribute("layout", "@layout/other_layout") },
        "missing child" to { document -> footer(document).removeChild(include(document)); Unit },
        "unexpected child" to { document -> footer(document).appendChild(document.createElement("Button")); Unit }
    )
    for ((label, mutate) in mutations) {
        val document = fresh()
        mutate(document)
        check(runCatching { hideSettingsDonationFooter(document) }.exceptionOrNull() is PatchException) {
            "Changed donation footer did not fail clearly: $label"
        }
    }
    println("PASS settings donation resource guards: valid scope and eight missing/changed-layout failures")
}
