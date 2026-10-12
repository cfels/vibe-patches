/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.yanosik.ad.disableStartupCampaignsPatch
import app.morphe.patches.yanosik.ad.disableVoiceAdsPatch
import app.morphe.patches.yanosik.ad.hideBannerAdsPatch
import app.morphe.patches.yanosik.promotions.suppressPremiumPromotionsPatch
import app.morphe.patches.yanosik.premium.overrideLocalProChecksPatch
import app.morphe.patches.yanosik.privacy.disableTelemetryPatch
import app.morphe.patches.yanosik.shared.PACKAGE_NAME
import app.morphe.patches.yanosik.shared.VERSION_NAME
import app.morphe.patches.yanosik.shared.requireReferenceVersion
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.ZipFile

private object YanosikValidationAssets

private val changedMethods = setOf(
    "Lss0;->b(Lgzc;)Z",
    "Lpl/neptis/features/adsbanners/banners/small/AdsSmallMainFragment;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V",
    "Lpl/neptis/features/adsbanners/banners/dashboard/AdsDashboardMainFragment;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V",
    "La5b;->a(Lah1;Lu1c;Ly93;I)V",
    "Ln1a;->b(Lah1;Lu1c;Ly93;I)V",
    "Lnhc;->j(Llp;Lu1c;Ly93;I)V",
    "Lpl/neptis/data/startappadvert/StartAppAdvertService;->refresh()V",
    "Lpl/neptis/yanosik/mobi/android/common/ui/activities/launcher/LauncherActivity;->x0(" +
        "Lpl/neptis/yanosik/mobi/android/common/ui/activities/launcher/LauncherActivity;JLdm3;)Ljava/lang/Object;",
    "Lzr;->R(Lsva;Lwq;Z)V",
    ADVERTISING_CAMPAIGN_VOICE_METHOD,
    "Lo6f;->a(Lh9f;Llv7;Lu1c;ZLy93;II)V",
    "Lm1a;->i(Lb4f;Ly93;I)V",
    "Lso8;->n(Ln8g;I)V",
    "Lpl/neptis/yanosik/mobi/android/dashboard/dashboardpack/activity/DashboardPackActivity;->onCreate(Landroid/os/Bundle;)V",
    "Ly8;->c(Ll44;Ly93;)J",
    "Lpl/neptis/features/login/AccountStartActivity;->onCreate(Landroid/os/Bundle;)V",
    "Lpl/neptis/user/ui/premium/PremiumActivity;->onCreate(Landroid/os/Bundle;)V",
    "Lr5f;->b(Lr5f;Lcom/revenuecat/purchases/EntitlementInfos;)Lh6f;",
    "Lr5f;-><init>(Lkp3;Landroid/content/Context;)V",
    "Ll9f;-><init>(ZZLh9f;)V",
    "La6f;-><init>(Lh6f;ZZLh9f;)V",
    "Lpl/neptis/features/settings/AppPreferenceActivity;->setupNoAdvSection()V"
) + telemetryMethodDescriptors + settingsDonationObserverDescriptor

private fun descriptor(method: Method) =
    "${method.definingClass}->${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}"

private fun instructions(method: Method) = method.implementation?.instructions?.toList().orEmpty()

// Compare instructions whose references can change pool indexes on serialization by their
// resolved descriptor, not raw index. This checks unchanged methods, not runtime semantics.
private fun semanticOutline(method: Method) = instructions(method).map {
    it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else ""
}

private fun verifyPromotionControlFlow(method: Method) {
    val ops = instructions(method)
    var address = 0
    val byAddress = ops.associateBy { instruction ->
        address.also { address += instruction.codeUnits }
    }
    val start = byAddress.entries.first { (_, op) ->
        op.opcode == Opcode.CONST_16 && (op as NarrowLiteralInstruction).narrowLiteral == 68
    }.key
    // Execute the actual serialized guard, including its encoded branch offsets.
    for (action in (0..100).toList() + listOf(-1, Int.MAX_VALUE)) {
        val registers = IntArray(24).apply { this[3] = action }
        var pc = start
        var returned: Boolean? = null
        repeat(10) {
            if (returned != null) return@repeat
            val op = byAddress.getValue(pc)
            val next = pc + op.codeUnits
            when (op.opcode) {
                Opcode.CONST_16, Opcode.CONST_4 -> {
                    registers[(op as OneRegisterInstruction).registerA] = (op as NarrowLiteralInstruction).narrowLiteral
                    pc = next
                }
                Opcode.IF_EQ, Opcode.IF_NE -> {
                    val instruction = op as TwoRegisterInstruction
                    val equal = registers[instruction.registerA] == registers[instruction.registerB]
                    val branch = if (op.opcode == Opcode.IF_EQ) equal else !equal
                    pc = if (branch) pc + (op as OffsetInstruction).codeOffset else next
                }
                Opcode.RETURN -> {
                    check(registers[(op as OneRegisterInstruction).registerA] == 1)
                    returned = true
                }
                Opcode.NOP -> returned = false
                else -> error("Guard escaped into application code: ${op.opcode}")
            }
        }
        check(returned == (action == 68 || action == 70)) { "Incorrect suppression for action $action" }
    }
}

private fun verifyPromotionCardControlFlow(method: Method, slots: Set<String>) {
    check(slots.size == 10 && "SUBSCRIPTION_MANAGEMENT" in slots)
    var address = 0
    val byAddress = instructions(method).associateBy { op ->
        address.also { address += op.codeUnits }
    }
    // Interpret the guard from the serialized DEX, using all enum slots present in
    // the reference artifact. No Compose call or parameter write may run here.
    for (slot in slots + setOf<String?>(null)) {
        val registers = arrayOfNulls<String>(31).apply { this[24] = slot?.let { "alert:$it" } }
        var pc = 0
        var suppressed: Boolean? = null
        repeat(10) {
            if (suppressed != null) return@repeat
            val op = byAddress.getValue(pc)
            val next = pc + op.codeUnits
            when (op.opcode) {
                Opcode.MOVE_OBJECT_FROM16 -> {
                    val move = op as TwoRegisterInstruction
                    registers[move.registerA] = registers[move.registerB]
                    pc = next
                }
                Opcode.IGET_OBJECT -> {
                    check((op as ReferenceInstruction).reference.toString() == "Lh9f;->a:Li9f;")
                    val read = op as TwoRegisterInstruction
                    registers[read.registerA] = registers[read.registerB]!!.removePrefix("alert:")
                    pc = next
                }
                Opcode.SGET_OBJECT -> {
                    check((op as ReferenceInstruction).reference.toString() ==
                        "Li9f;->SUBSCRIPTION_MANAGEMENT:Li9f;")
                    registers[(op as OneRegisterInstruction).registerA] = "SUBSCRIPTION_MANAGEMENT"
                    pc = next
                }
                Opcode.IF_EQZ -> {
                    pc = if (registers[(op as OneRegisterInstruction).registerA] == null)
                        pc + (op as OffsetInstruction).codeOffset else next
                }
                Opcode.IF_EQ -> {
                    val branch = op as TwoRegisterInstruction
                    pc = if (registers[branch.registerA] == registers[branch.registerB])
                        pc + (op as OffsetInstruction).codeOffset else next
                }
                Opcode.RETURN_VOID -> suppressed = true
                Opcode.NOP -> suppressed = false
                else -> error("Card guard escaped into Compose: ${op.opcode}")
            }
        }
        check(suppressed == (slot != null && slot != "SUBSCRIPTION_MANAGEMENT"))
        check(registers[24] == slot?.let { "alert:$it" }) { "Alert parameter was overwritten" }
    }
    println("PASS serialized PRO card guard: nine promo slots suppressed; management/null preserved")
}

private fun verifyPremiumScreenControlFlow(method: Method) {
    val ops = instructions(method)
    var address = 0
    val byAddress = ops.associateBy { op -> address.also { address += op.codeUnits } }
    val start = ops.take(2).sumOf { it.codeUnits }
    for (target in listOf(null, "PRO", "YBR", "unknown", "DONATE", "VOUCHER")) {
        val registers = arrayOfNulls<Any>(11).apply { this[9] = "activity" }
        var pc = start
        var pending: Any? = null
        var finished = false
        var suppressed: Boolean? = null
        repeat(20) {
            if (suppressed != null) return@repeat
            val op = byAddress.getValue(pc)
            val next = pc + op.codeUnits
            when (op.opcode) {
                Opcode.INVOKE_VIRTUAL -> {
                    val call = op as FiveRegisterInstruction
                    when ((op as ReferenceInstruction).reference.toString()) {
                        "Landroid/app/Activity;->getIntent()Landroid/content/Intent;" -> {
                            check(registers[call.registerC] == "activity")
                            pending = "intent"
                        }
                        "Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;" -> {
                            check(registers[call.registerC] == "intent")
                            check(registers[call.registerD] == "pl.neptis.user.ui.premium.EXTRA_TARGET")
                            pending = target
                        }
                        "Ljava/lang/String;->equals(Ljava/lang/Object;)Z" ->
                            pending = registers[call.registerC] == registers[call.registerD]
                        "Landroid/app/Activity;->finish()V" -> {
                            check(registers[call.registerC] == "activity")
                            finished = true
                        }
                        else -> error("Screen guard escaped into application code")
                    }
                    pc = next
                }
                Opcode.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT -> {
                    registers[(op as OneRegisterInstruction).registerA] = pending
                    pc = next
                }
                Opcode.CONST_STRING -> {
                    registers[(op as OneRegisterInstruction).registerA] =
                        ((op as ReferenceInstruction).reference as StringReference).string
                    pc = next
                }
                Opcode.IF_NEZ -> pc = if (registers[(op as OneRegisterInstruction).registerA] == true)
                    pc + (op as OffsetInstruction).codeOffset else next
                Opcode.RETURN_VOID -> { check(finished); suppressed = true }
                Opcode.NOP -> { check(!finished); suppressed = false }
                else -> error("Unexpected screen guard opcode: ${op.opcode}")
            }
        }
        check(suppressed == (target != "DONATE" && target != "VOUCHER"))
    }
    println("PASS serialized PRO screen guard: closes PRO/YBR/default; donation/voucher preserved")
}

fun validateReferenceApk(apk: File, workspace: File) {
    require(apk.isFile) { "Reference APK not found: $apk" }
    val opcodes = Opcodes.getDefault()
    val originalDex = DexFileFactory.loadDexContainer(apk, opcodes)
    val originals = originalDex.dexEntryNames.flatMap { name ->
        originalDex.getEntry(name)!!.dexFile.classes.flatMap { it.methods }
    }.associateBy(::descriptor)
    check(changedMethods.all { it in originals }) { "Missing Yanosik reference methods" }
    verifyVoiceAdvertisingRoutes(originals.values)
    verifyVibeSettingsTargets(originals.values)

    val slots = originalDex.dexEntryNames.flatMap { name ->
        originalDex.getEntry(name)!!.dexFile.classes.filter { it.type == "Li9f;" }
            .flatMap { it.fields }.filter { it.type == "Li9f;" }.map { it.name }
    }.toSet()
    val patches = setOf(suppressPremiumPromotionsPatch, hideBannerAdsPatch,
        disableStartupCampaignsPatch, disableVoiceAdsPatch, overrideLocalProChecksPatch, disableTelemetryPatch)
    check(patches.map { it.name }.distinct().size == 6)
    check(patches.all { patch ->
        patch.compatibility!!.single().let {
            it.packageName == PACKAGE_NAME && it.targets.single().version == VERSION_NAME
        }
    }) { "Incorrect patch metadata" }

    Patcher(PatcherConfig(apk, workspace)).use { patcher ->
        patcher += patches
        runBlocking {
            patcher().collect { result ->
                check(result.exception == null) { result.exception!!.stackTraceToString() }
                println("PASS patch: ${result.patch.name ?: "version guard"}")
            }
        }
        val result = patcher.get()
        // Preserve the compiled resource APK for independent binary-manifest
        // inspection. Telemetry is the first Yanosik patch that edits resources.
        val resourcesApk = requireNotNull(requireNotNull(result.resources).resourcesApk)
        resourcesApk.copyTo(File(workspace, "validated-resources.apk"), overwrite = true)
        verifyTelemetryManifest(apk, resourcesApk)
        verifySettingsDonationResources(apk, resourcesApk)
        ZipFile(resourcesApk).use { zip ->
            val entries = zip.entries().asSequence().filter { it.name.endsWith("/vibe_patched.png") }.toList()
            check(entries.size == 1) { "Missing or duplicate compiled Vibe Patched icon" }
            val icon = checkNotNull(YanosikValidationAssets::class.java.getResourceAsStream("/yanosik/vibe_patched.png"))
                .use { it.readBytes() }
            check(zip.getInputStream(entries.single()).use { it.readBytes() }.contentEquals(icon)) {
                "Compiled Vibe Patched icon differs from the bundled asset"
            }
            println("PASS compiled Vibe Patched drawable")
        }
        val changed = mutableSetOf<String>()
        val extensionMethods = mutableListOf<Method>()
        var untouched = 0
        for (output in result.dexFiles) {
            val bytes = output.stream.use { it.readBytes() }
            // Force serialization then decode the actual generated DEX again.
            val saved = File(workspace, "validated-${output.name}").apply { writeBytes(bytes) }
            val dex = DexFileFactory.loadDexFile(saved, opcodes)
            for (method in dex.classes.flatMap { it.methods }) {
                val key = descriptor(method)
                if (key in vibeSettingsExtensionDescriptors) {
                    check(key !in originals) { "Extension unexpectedly replaces an existing app method: $key" }
                    extensionMethods += method
                    continue
                }
                val original = originals[key] ?: error("Unexpected added method: $key")
                if (key in changedMethods) {
                    changed += key
                    val ops = instructions(method)
                    when {
                        key == "Lso8;->n(Ln8g;I)V" -> verifyHomePatchedTileGuard(method, original)
                        key == "Ly8;->c(Ll44;Ly93;)J" -> verifyHomeSidebarPalette(method, original)
                        key.contains("/DashboardPackActivity;->onCreate") -> verifyHomeThemeInstall(method, original)
                        key.contains("/AccountStartActivity;->onCreate") -> verifyAccountThemeInstall(method, original)
                        key == "Lzr;->R(Lsva;Lwq;Z)V" || key == ADVERTISING_CAMPAIGN_VOICE_METHOD ->
                            verifyVoiceAdsMethod(method, original)
                        key in telemetryMethodDescriptors -> verifyTelemetryMethod(method, original)
                        key == settingsDonationObserverDescriptor -> verifySettingsDonationObserver(method, original)
                        key.contains("/premium/PremiumActivity;->onCreate") -> {
                            check(semanticOutline(method).take(2) == semanticOutline(original).take(2))
                            verifyPremiumScreenControlFlow(method)
                            check(semanticOutline(method).drop(18) == semanticOutline(original).drop(2))
                            check(method.implementation!!.registerCount == 11)
                        }
                        key.endsWith("->setupNoAdvSection()V") -> {
                            val originalOutline = semanticOutline(original)
                            check(semanticOutline(method).take(16) == originalOutline.take(16))
                            check(semanticOutline(method).drop(22) == originalOutline.drop(16))
                            check(ops.subList(16, 22).map { it.opcode } == listOf(
                                Opcode.IGET_OBJECT, Opcode.CONST_4, Opcode.INVOKE_VIRTUAL,
                                Opcode.CONST_4, Opcode.INVOKE_VIRTUAL, Opcode.RETURN_VOID))
                            val read = ops[16] as TwoRegisterInstruction
                            check(read.registerA == 1 && read.registerB == 0)
                            check((ops[16] as ReferenceInstruction).reference.toString() ==
                                "Ldh;->D:Landroidx/appcompat/widget/SwitchCompat;")
                            for ((index, value) in listOf(17 to 1, 19 to 0)) {
                                check((ops[index] as OneRegisterInstruction).registerA == 2)
                                check((ops[index] as NarrowLiteralInstruction).narrowLiteral == value)
                            }
                            for ((index, ref) in listOf(
                                18 to "Landroid/widget/CompoundButton;->setChecked(Z)V",
                                20 to "Landroid/view/View;->setEnabled(Z)V")) {
                                check((ops[index] as ReferenceInstruction).reference.toString() == ref)
                                val call = ops[index] as FiveRegisterInstruction
                                check(call.registerCount == 2 && call.registerC == 1 && call.registerD == 2)
                            }
                        }
                        key.startsWith("Lr5f;->b(") -> {
                            check(ops.take(8).map { it.opcode } == listOf(Opcode.NEW_INSTANCE,
                                Opcode.SGET_OBJECT, Opcode.NEW_INSTANCE, Opcode.CONST_WIDE,
                                Opcode.INVOKE_DIRECT, Opcode.CONST_4, Opcode.INVOKE_DIRECT, Opcode.RETURN_OBJECT))
                            check((ops[1] as ReferenceInstruction).reference.toString() == "Lh4f;->ACTIVE:Lh4f;")
                            check((ops[3] as WideLiteralInstruction).wideLiteral == 4102444800000L)
                            check((ops[5] as NarrowLiteralInstruction).narrowLiteral == 0)
                            check((ops[6] as ReferenceInstruction).reference.toString() ==
                                "Lb6f;-><init>(Lh4f;Ljava/util/Date;Z)V")
                            check((ops[6] as FiveRegisterInstruction).let {
                                it.registerC == 0 && it.registerD == 1 && it.registerE == 2 && it.registerF == 3
                            })
                            check((ops[7] as OneRegisterInstruction).registerA == 0)
                            check(semanticOutline(method).drop(8) == semanticOutline(original))
                        }
                        key.startsWith("Lr5f;-><init>") -> {
                            check(semanticOutline(method).take(4) == semanticOutline(original).take(4))
                            check(ops[4].opcode == Opcode.CONST_4 && (ops[4] as NarrowLiteralInstruction).narrowLiteral == 0)
                            check((ops[4] as OneRegisterInstruction).registerA == 2)
                            check(ops[5].opcode == Opcode.INVOKE_STATIC)
                            check((ops[5] as ReferenceInstruction).reference.toString() ==
                                "Lr5f;->b(Lr5f;Lcom/revenuecat/purchases/EntitlementInfos;)Lh6f;")
                            check((ops[5] as FiveRegisterInstruction).let { it.registerC == 1 && it.registerD == 2 })
                            check(ops[6].opcode == Opcode.MOVE_RESULT_OBJECT && (ops[6] as OneRegisterInstruction).registerA == 2)
                            check(semanticOutline(method).drop(7) == semanticOutline(original).drop(5))
                        }
                        key.startsWith("Ll9f;") || key.startsWith("La6f;") -> {
                            val values = if (key.startsWith("Ll9f;")) listOf(1, 1, 0) else listOf(1, 0, 0)
                            val registerStart = if (key.startsWith("Ll9f;")) 1 else 2
                            ops.take(3).forEachIndexed { index, op ->
                                check(op.opcode == Opcode.CONST_4)
                                check((op as NarrowLiteralInstruction).narrowLiteral == values[index])
                                check((op as OneRegisterInstruction).registerA == registerStart + index)
                            }
                            check(semanticOutline(method).drop(3) == semanticOutline(original))
                            check(method.implementation!!.registerCount == original.implementation!!.registerCount)
                        }
                        key.startsWith("Lss0;") -> {
                            val names = ops.take(16).map { it.opcode }
                            check(names.contains(Opcode.IF_EQ) && names.contains(Opcode.IF_NE) && names.contains(Opcode.RETURN))
                            check(semanticOutline(method).containsAll(semanticOutline(original)))
                            check(method.implementation!!.registerCount == original.implementation!!.registerCount)
                            verifyPromotionControlFlow(method)
                        }
                        key.startsWith("Lo6f;") -> {
                            verifyPromotionCardControlFlow(method, slots)
                            check(semanticOutline(method).drop(7) == semanticOutline(original))
                            check(method.implementation!!.registerCount == original.implementation!!.registerCount)
                        }
                        key.contains("/launcher/LauncherActivity;->x0(") -> {
                            check(ops.take(2).map { it.opcode } == listOf(Opcode.CONST_4, Opcode.RETURN_OBJECT))
                            check((ops[0] as NarrowLiteralInstruction).narrowLiteral == 0)
                            check((ops[0] as OneRegisterInstruction).registerA ==
                                (ops[1] as OneRegisterInstruction).registerA)
                            check(semanticOutline(method).drop(2) == semanticOutline(original))
                        }
                        key.endsWith("->refresh()V") ||
                            key.startsWith("La5b;") || key.startsWith("Ln1a;") || key.startsWith("Lnhc;") ||
                            key.startsWith("Lm1a;") -> {
                            check(ops.first().opcode == Opcode.RETURN_VOID)
                            check(semanticOutline(method).drop(1) == semanticOutline(original))
                            check(method.implementation!!.registerCount == original.implementation!!.registerCount)
                        }
                        else -> {
                            check(ops.take(3).map { it.opcode } == listOf(Opcode.CONST_16, Opcode.INVOKE_VIRTUAL, Opcode.RETURN_VOID))
                            check((ops[1] as ReferenceInstruction).reference.toString() ==
                                "Landroid/view/View;->setVisibility(I)V")
                            check(method.implementation!!.registerCount == 6)
                        }
                    }
                } else {
                    check(semanticOutline(method) == semanticOutline(original)) { "Unrelated method changed: $key" }
                    untouched++
                }
            }
        }
        check(changed == changedMethods) { "Missing patched methods: ${changedMethods - changed}" }
        verifyVibeSettingsExtension(extensionMethods)
        println("PASS DEX serialization: ${changed.size} intended methods, $untouched unchanged method outlines")
    }
    // Separate patcher: ensure a renamed/missing target fails, with no silent success.
    val missing = bytecodePatch {
        execute {
            app.morphe.patcher.Fingerprint(
                definingClass = "Lpl/neptis/NonexistentYanosikPromotion;",
                name = "onViewCreated", returnType = "V"
            ).method
        }
    }
    Patcher(PatcherConfig(apk, File(workspace, "missing-target"))).use { patcher ->
        patcher += setOf(missing)
        var failed = false
        runBlocking { patcher().collect { failed = failed || it.exception != null } }
        check(failed) { "Missing fingerprint did not fail" }
    }
    println("PASS missing-target failure")
    verifySettingsDonationResourceFailures()
    for ((packageName, version) in listOf(PACKAGE_NAME to "26.8.0", "other.app" to VERSION_NAME)) {
        check(runCatching { requireReferenceVersion(packageName, version) }.exceptionOrNull()?.message
            ?.contains("Yanosik patches require") == true)
    }
    println("PASS incompatible package and version rejection")
    // Source APK must stay readable and contains all its original DEX entries.
    ZipFile(apk).use { zip -> check(originalDex.dexEntryNames.all { zip.getEntry(it) != null }) }
}

fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: ValidateYanosik <reference.apk> <workspace-directory>" }
    validateReferenceApk(File(args[0]), File(args[1]).apply { mkdirs() })
}
