/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

internal val telemetryLifecycleClasses = setOf(
    "Lpl/neptis/libraries/analyticsproto/AnalyticsManager;",
    "Lpl/neptis/libraries/analitycs/usage/UsageAnalyticsService;",
    "Lpl/neptis/libraries/kioskanalytics/KioskAnalyticsService;",
    "Lpl/neptis/features/adsbanners/advertstatistics/AdvertStatisticsManager;",
    "Lpl/neptis/yanosik/mobi/android/common/services/statisticscan/StatisticScanService;",
    "Lpl/neptis/yanosik/mobi/android/common/services/analytics/AnalyticsService;"
)

internal val telemetryMethodDescriptors = telemetryLifecycleClasses.flatMap { clazz ->
    listOf("$clazz->onCreateAsync()V", "$clazz->onDestroyAsync()V")
}.toSet() + setOf(
    "Leff;->a(IILpl/neptis/libraries/network/model/analytics/IAnalyticsDataModel;)V",
    "Lwy;->g()V",
    "Lgp1;->e(Lxy;)V",
    "Lgp1;->o(Lz30;)V",
    "Lpl/neptis/libraries/kioskanalytics/KioskAnalyticsService;->onNewEventData(Lyaa;)V",
    "Lpl/neptis/libraries/kioskanalytics/KioskAnalyticsService;->sendIfPossible()V",
    "Lpl/neptis/features/adsbanners/advertstatistics/AdvertStatisticsManager;->scheduleSendJob(J)V",
    "Lpl/neptis/features/audioalerts/AudioAlertsService;->sendAudioNotificationRequest()V",
    "Lpl/neptis/features/audioalerts/AudioAlertsService;->sendAudioNotificationExecutedRequest(Ll11;)V",
    "Lpl/neptis/features/adsbanners/adsservice/AdvertService;->sendLocationAdvertRequest()V",
    "Lpl/neptis/features/adsbanners/adsservice/AdvertService;->analyticsSend(Lwva;)V",
    "Lpl/neptis/yanosik/mobi/android/common/services/statisticscan/StatisticScanService;->onTick(I)V",
    "Lpl/neptis/libraries/analyticsproto/AnalyticsManager;->sendAnalytics(Lbz;Lbm3;)Ljava/lang/Object;",
    "Lpl/neptis/libraries/analyticsproto/AnalyticsManager;->sendFlutterAnalytics(Lc57;Lbm3;)Ljava/lang/Object;",
    "Lpl/neptis/libraries/analitycs/usage/UsageAnalyticsService;->schedulePackageSending(Lw3l;)La4a;",
    "Lcloud/proxi/ProxiCloudSdk;->create(Landroid/content/Context;Ljava/lang/String;)Lcloud/proxi/ProxiCloudSdk;",
    "Lcloud/proxi/ProxiCloudSdk;->getInstance()Lcloud/proxi/ProxiCloudSdk;",
    "Lcloud/proxi/sdk/jobs/EventReactionsSyncWorker;->e(Lbm3;)Ljava/lang/Object;",
    "Lcloud/proxi/sdk/wifi/WifiScanWorker;->e(Lbm3;)Ljava/lang/Object;",
    "Lme/notinote/sdk/service/synchronize/workers/BeaconSampleDozeSendingWorker;->c()Lgsa;",
    "Lme/notinote/sdk/service/synchronize/workers/SynchronizeWorker;->c()Lgsa;",
    "Lme/notinote/sdk/service/network/communication/NetworkService;->c(Lme/notinote/sdk/service/network/communication/ProtobufRequest;)V",
    "Lme/notinote/sdk/service/wifi/WifiService;->a(Lme/notinote/sdk/service/wifi/WifiService;Ljava/util/List;)V",
    "Lme/notinote/sdk/service/control/storage/ProtobufBeaconStorage;->c(Ljava/util/List;Ldm3;)Ljava/lang/Object;",
    "Ldtn;->b()I", "Low4;->i()Z", "Lbva;->a()Ljava/lang/Boolean;",
    "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->sendUnsentReports()V",
    "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->log(Ljava/lang/String;)V",
    "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->recordException(Ljava/lang/Throwable;)V",
    "Lx5l;->b()Z", "Lx5l;->a()Z",
    "Lfp0;->e(Ljava/lang/String;Ljava/lang/Double;Landroid/os/Bundle;ZLjava/util/UUID;Lxgd;)V",
    "Lko0;->d(Lr47;)V",
    "Lcom/revenuecat/purchases/common/HTTPClient;->performRequest(Ljava/net/URL;Lcom/revenuecat/purchases/common/networking/Endpoint;Ljava/util/Map;Ljava/util/List;Ljava/util/Map;ZLjava/util/List;I)Lcom/revenuecat/purchases/common/networking/HTTPResult;",
    "Lcom/revenuecat/purchases/common/HTTPClient;->getConnection(Lcom/revenuecat/purchases/common/networking/HTTPRequest;J)Ljava/net/HttpURLConnection;",
    "Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsTracker;->trackEvent(Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsEntry;)V",
    "Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsTracker;->trackEventInCurrentThread\$purchases_defaultsBc8Release(Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsEntry;)V"
)

internal fun verifyTelemetryMethod(method: Method, original: Method) {
    val ops = method.implementation!!.instructions.toList()
    val originalOps = original.implementation!!.instructions.toList()
    fun outline(instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>) =
        instructions.map { it.opcode.name + if (it is ReferenceInstruction) ":${it.reference}" else "" }
    check(method.implementation!!.registerCount == original.implementation!!.registerCount)
    if (method.definingClass in setOf("Lpl/neptis/features/audioalerts/AudioAlertsService;",
        "Lpl/neptis/features/adsbanners/adsservice/AdvertService;")) {
        check(method.accessFlags == 18 && original.accessFlags == 18)
        val (registers, payloadReferences) = when (method.name) {
            "sendAudioNotificationRequest" -> 6 to listOf(
                "Liya;->getLocation()Lsva;", "Lsva;->a:D", "Lsva;->b:D",
                "Lsva;->c:Ljava/lang/Float;", "Lsva;->d:Ljava/lang/Float;",
                "Lob8;-><init>(FFII)V", "Lrt5;->e(Logf;)V")
            "sendAudioNotificationExecutedRequest" -> 10 to listOf(
                "campaign_id", "source", "Lhy;->AUDIO_NOTIFICATION_PLAYED:Lhy;",
                "Ll11;->a:J", "Ll11;->d:I", "Lu11;-><init>(JIJ)V", "Lrt5;->e(Logf;)V")
            "sendLocationAdvertRequest" -> 11 to listOf(
                "Lpl9;-><init>()V", "Liya;->getLocation()Lsva;", "Lsva;->a:D", "Lsva;->b:D",
                "Lsva;->b()I", "Lsva;->d:Ljava/lang/Float;", "Lcm8;-><init>(JIILzm3;)V",
                "Lpl9;->y(Lcm8;)V", "Lrt5;->e(Logf;)V")
            "analyticsSend" -> 10 to listOf(
                "Lwva;->c:J", "Lwva;->b:J", "Ld5j;->BANNER_VIEW:Ld5j;",
                "Lmt;-><init>(JLs5j;Ld5j;JI)V", "Lasm;->e(Ljava/lang/Object;Z)V")
            else -> error("Unexpected marketing telemetry target: ${method.name}")
        }
        check(method.implementation!!.registerCount == registers)
        val refs = originalOps.filterIsInstance<ReferenceInstruction>().map { it.reference.toString() }
        check(refs.containsAll(payloadReferences)) { "Marketing request no longer matches inspected payload" }
        check(ops.first().opcode == Opcode.RETURN_VOID && ops.first().codeUnits == 1)
        check(ops.first() !is ReferenceInstruction)
        check(outline(ops.drop(1)) == outline(originalOps))
        println("PASS serialized marketing telemetry: ${method.definingClass}->${method.name} returns before input/location/request reads")
        return
    }
    if (method.definingClass in telemetryLifecycleClasses && method.name in setOf("onCreateAsync", "onDestroyAsync")) {
        check(ops[0].opcode == Opcode.INVOKE_SUPER)
        check((ops[0] as ReferenceInstruction).reference.toString() == "Lz5;->${method.name}()V")
        check((ops[0] as FiveRegisterInstruction).registerC ==
            (originalOps[0] as FiveRegisterInstruction).registerC)
        check(ops[1].opcode == Opcode.RETURN_VOID)
        check(outline(ops.drop(2)) == outline(originalOps.drop(1)))
        return
    }
    val prefixSize = when {
        method.definingClass == "Lcom/revenuecat/purchases/common/HTTPClient;" -> {
            check(ops.take(4).map { it.opcode } == listOf(Opcode.NEW_INSTANCE, Opcode.CONST_STRING,
                Opcode.INVOKE_DIRECT, Opcode.THROW))
            check((ops[0] as ReferenceInstruction).reference.toString() == "Ljava/io/IOException;")
            check((ops[0] as OneRegisterInstruction).registerA == 0)
            check((ops[1] as ReferenceInstruction).reference.toString() == "RevenueCat requests disabled by Vibe")
            check((ops[1] as OneRegisterInstruction).registerA == 1)
            check((ops[2] as ReferenceInstruction).reference.toString() ==
                "Ljava/io/IOException;-><init>(Ljava/lang/String;)V")
            check((ops[2] as FiveRegisterInstruction).let { it.registerCount == 2 && it.registerC == 0 && it.registerD == 1 })
            check((ops[3] as OneRegisterInstruction).registerA == 0)
            check(method.accessFlags == if (method.name == "getConnection") 18 else 17)
            check(method.implementation!!.registerCount == if (method.name == "getConnection") 6 else 26)
            println("PASS RevenueCat ${method.name}: local IOException before original request/connection code")
            4
        }
        method.definingClass == "Lcloud/proxi/ProxiCloudSdk;" || method.definingClass.startsWith("Lcloud/proxi/sdk/") -> {
            val result = if (method.definingClass == "Lcloud/proxi/ProxiCloudSdk;") "Lc06;" else "Lksa;"
            check(ops.take(3).map { it.opcode } == listOf(Opcode.NEW_INSTANCE, Opcode.INVOKE_DIRECT, Opcode.RETURN_OBJECT))
            check((ops[0] as ReferenceInstruction).reference.toString() == result)
            check((ops[0] as OneRegisterInstruction).registerA == 0)
            check((ops[1] as ReferenceInstruction).reference.toString() == "$result-><init>()V")
            check((ops[1] as FiveRegisterInstruction).let { it.registerCount == 1 && it.registerC == 0 })
            check((ops[2] as OneRegisterInstruction).registerA == 0)
            3
        }
        method.returnType == "Lgsa;" -> {
            val field = if (method.definingClass.endsWith("/SynchronizeWorker;")) "g" else "e"
            check(ops.take(5).map { it.opcode } == listOf(Opcode.IGET_OBJECT, Opcode.NEW_INSTANCE,
                Opcode.INVOKE_DIRECT, Opcode.INVOKE_VIRTUAL, Opcode.RETURN_OBJECT))
            check((ops[0] as ReferenceInstruction).reference.toString() == "${method.definingClass}->$field:Lb2i;")
            check((ops[0] as TwoRegisterInstruction).let {
                it.registerA == 0 && it.registerB == method.implementation!!.registerCount - 1
            })
            check((ops[1] as ReferenceInstruction).reference.toString() == "Lksa;" &&
                (ops[1] as OneRegisterInstruction).registerA == 1)
            check((ops[2] as ReferenceInstruction).reference.toString() == "Lksa;-><init>()V")
            check((ops[2] as FiveRegisterInstruction).let { it.registerCount == 1 && it.registerC == 1 })
            check((ops[3] as ReferenceInstruction).reference.toString() == "Lb2i;->g(Ljava/lang/Object;)Z")
            check((ops[3] as FiveRegisterInstruction).let { it.registerCount == 2 && it.registerC == 0 && it.registerD == 1 })
            check((ops[4] as OneRegisterInstruction).registerA == 0)
            5
        }
        method.returnType in setOf("Z", "I") -> {
            check(ops[0].opcode == Opcode.CONST_4 && ops[1].opcode == Opcode.RETURN)
            check((ops[0] as NarrowLiteralInstruction).narrowLiteral == if (method.definingClass == "Ldtn;") 1 else 0)
            check((ops[0] as OneRegisterInstruction).registerA == 0 && (ops[1] as OneRegisterInstruction).registerA == 0)
            2
        }
        method.returnType != "V" -> {
            check(ops[1].opcode == Opcode.RETURN_OBJECT && (ops[1] as OneRegisterInstruction).registerA == 0)
            check((ops[0] as OneRegisterInstruction).registerA == 0)
            if (method.name == "schedulePackageSending") {
                check(ops[0].opcode == Opcode.CONST_4 && (ops[0] as NarrowLiteralInstruction).narrowLiteral == 0)
            } else {
                check(ops[0].opcode == Opcode.SGET_OBJECT)
                check((ops[0] as ReferenceInstruction).reference.toString() ==
                    if (method.name == "sendAnalytics" || method.definingClass.endsWith("/ProtobufBeaconStorage;"))
                        "Ldzk;->a:Ldzk;" else "Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;")
            }
            2
        }
        else -> { check(ops[0].opcode == Opcode.RETURN_VOID); 1 }
    }
    check(outline(ops.drop(prefixSize)) == outline(originalOps))
}
