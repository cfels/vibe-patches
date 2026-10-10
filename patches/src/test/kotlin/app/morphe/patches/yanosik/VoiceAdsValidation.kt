/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal const val ADVERTISING_CAMPAIGN_VOICE_METHOD =
    "Lpl/neptis/features/audioalerts/AudioAlertsService;->playAudioNotification(Ll11;)V"
private const val POI_VOICE_METHOD = "Lzr;->R(Lsva;Lwq;Z)V"

private fun voiceDescriptor(method: Method) =
    "${method.definingClass}->${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}"

internal fun verifyVoiceAdsMethod(method: Method, original: Method) {
    val descriptor = voiceDescriptor(method)
    check(descriptor in setOf(POI_VOICE_METHOD, ADVERTISING_CAMPAIGN_VOICE_METHOD))
    check(descriptor == voiceDescriptor(original))
    check(method.accessFlags == if (descriptor == POI_VOICE_METHOD) 17 else 18)
    check(method.accessFlags == original.accessFlags)
    check(method.implementation!!.registerCount == original.implementation!!.registerCount)
    val ops = method.implementation!!.instructions.toList()
    val before = original.implementation!!.instructions.toList()
    check(ops.first().opcode == Opcode.RETURN_VOID && ops.first().codeUnits == 1)
    fun outline(methodOps: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>) = methodOps.map {
        it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else ""
    }
    check(outline(ops.drop(1)) == outline(before))
    // Execution terminates at address zero for any inputs. No player lookup,
    // coroutine launch, timestamp update or completion-report callback is reached.
    check(ops.first() !is ReferenceInstruction)
    println("PASS serialized voice ad guard: $descriptor returns before any player or report call")
}

internal fun verifyVoiceAdvertisingRoutes(originalMethods: Collection<Method>) {
    fun references(method: Method) = method.implementation?.instructions?.toList().orEmpty()
        .filterIsInstance<ReferenceInstruction>()
    fun callers(target: String) = originalMethods.flatMap { method ->
        references(method).filter { it.reference.toString() == target }.map { voiceDescriptor(method) }
    }.groupingBy { it }.eachCount()
    // All static DEX calls into the private POI playback chain lead through the
    // patched public dispatcher. Both normal and stop-POI calls share that guard.
    check(callers(POI_VOICE_METHOD) == mapOf("Lhr;->a(Lsva;Ljava/util/List;)Ljava/util/Map;" to 2))
    check(callers("Lzr;->T(Lwq;Z)V") == mapOf(POI_VOICE_METHOD to 2))
    check(callers("Lzr;->V(Lwq;Ljava/lang/String;)V") == mapOf("Lzr;->T(Lwq;Z)V" to 2))
    check(callers("Lzr;->S(Ljava/lang/String;)V") == mapOf("Lzr;->V(Lwq;Ljava/lang/String;)V" to 1))
    check(callers(ADVERTISING_CAMPAIGN_VOICE_METHOD) ==
        mapOf("Lpl/neptis/features/audioalerts/AudioAlertsService;->tryToPlay()V" to 1))
    val advertProducers = originalMethods.filter { method ->
        method.definingClass != "Lhpi;" && references(method).any {
            it.opcode == Opcode.SGET_OBJECT && it.reference.toString() == "Lzoi;->ADVERT:Lzoi;"
        }
    }.map(::voiceDescriptor).toSet()
    check(advertProducers == setOf("Lzr;->S(Ljava/lang/String;)V", ADVERTISING_CAMPAIGN_VOICE_METHOD)) {
        "Uncovered ADVERT playback producer: $advertProducers"
    }
    // The shared player still exposes road-warning playback through ALERT. Its
    // code, and all navigation/critical-warning consumers, remain outside targets.
    check(originalMethods.any { voiceDescriptor(it) == "Lcqi;->c([Lxoi;)V" && references(it).any { op ->
        op.opcode == Opcode.SGET_OBJECT && op.reference.toString() == "Lzoi;->ALERT:Lzoi;"
    } })
    check(originalMethods.any { method -> references(method).any {
        it.opcode == Opcode.SGET_OBJECT && it.reference.toString() == "Lzoi;->NAVIGATION:Lzoi;" &&
            method.definingClass != "Lhpi;"
    } })
    println("PASS original DEX voice routes: both ADVERT producers guarded; ALERT/NAVIGATION paths retained")
}
