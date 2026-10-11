/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patches.yanosik.shared.COMPATIBILITY_YANOSIK
import app.morphe.patches.yanosik.shared.referenceVersionPatch

private const val PROTO = "Lpl/neptis/libraries/analyticsproto/AnalyticsManager;"
private const val USAGE = "Lpl/neptis/libraries/analitycs/usage/UsageAnalyticsService;"
private const val KIOSK = "Lpl/neptis/libraries/kioskanalytics/KioskAnalyticsService;"
private const val ADVERT = "Lpl/neptis/features/adsbanners/advertstatistics/AdvertStatisticsManager;"
private const val SCAN = "Lpl/neptis/yanosik/mobi/android/common/services/statisticscan/StatisticScanService;"
private const val LEGACY = "Lpl/neptis/yanosik/mobi/android/common/services/analytics/AnalyticsService;"
private const val PROXI = "Lcloud/proxi/ProxiCloudSdk;"
private const val CRASH = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;"
private const val AUDIO_ADS = "Lpl/neptis/features/audioalerts/AudioAlertsService;"
private const val BANNER_ADS = "Lpl/neptis/features/adsbanners/adsservice/AdvertService;"
private const val REVENUE_HTTP = "Lcom/revenuecat/purchases/common/HTTPClient;"
private const val REVENUE_DIAGNOSTICS = "Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsTracker;"
private const val REVENUE_DISABLED = """
    new-instance v0, Ljava/io/IOException;
    const-string v1, "RevenueCat requests disabled by Vibe"
    invoke-direct { v0, v1 }, Ljava/io/IOException;-><init>(Ljava/lang/String;)V
    throw v0
"""

private class Target(
    val fingerprint: Fingerprint,
    val prefix: String,
    val afterSuper: Boolean = false
)

private fun callTarget(
    clazz: String, name: String, params: List<String>, result: String, flags: Int,
    anchor: String, prefix: String = "return-void", afterSuper: Boolean = false
) = Target(Fingerprint(
    definingClass = clazz, name = name, parameters = params, returnType = result,
    accessFlags = AccessFlags.getAccessFlagsForMethod(flags).toList(), filters = listOf(methodCall(smali = anchor))
), prefix, afterSuper)

private val telemetryTargets = buildList {
    // All RevenueCat base/fallback URLs use this client. Fail locally through
    // the SDK's normal IOException callbacks before constructing or opening a
    // request; don't fabricate a purchase, response or remote entitlement.
    add(callTarget(REVENUE_HTTP, "performRequest", listOf("Ljava/net/URL;",
        "Lcom/revenuecat/purchases/common/networking/Endpoint;", "Ljava/util/Map;",
        "Ljava/util/List;", "Ljava/util/Map;", "Z", "Ljava/util/List;", "I"),
        "Lcom/revenuecat/purchases/common/networking/HTTPResult;", 17,
        "$REVENUE_HTTP->performCall(Ljava/net/URL;ZLcom/revenuecat/purchases/common/networking/Endpoint;Ljava/util/Map;Ljava/util/List;Ljava/util/Map;Z)Lcom/revenuecat/purchases/common/networking/HTTPResult;",
        REVENUE_DISABLED))
    add(callTarget(REVENUE_HTTP, "getConnection",
        listOf("Lcom/revenuecat/purchases/common/networking/HTTPRequest;", "J"),
        "Ljava/net/HttpURLConnection;", 18,
        "Ljava/net/URL;->openConnection()Ljava/net/URLConnection;", REVENUE_DISABLED))
    add(callTarget(REVENUE_DIAGNOSTICS, "trackEvent",
        listOf("Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsEntry;"), "V", 17,
        "$REVENUE_DIAGNOSTICS->checkAndClearDiagnosticsFileIfTooBig(Llv7;)V"))
    add(callTarget(REVENUE_DIAGNOSTICS, "trackEventInCurrentThread\$purchases_defaultsBc8Release",
        listOf("Lcom/revenuecat/purchases/common/diagnostics/DiagnosticsEntry;"), "V", 17,
        "Lcom/revenuecat/purchases/utils/EventsFileHelper;->appendEvent(Lcom/revenuecat/purchases/utils/Event;)V"))
    // Keep base lifecycle bookkeeping. Skip both subclass startup and shutdown:
    // legacy teardown otherwise dereferences a handler that was never initialized.
    for (clazz in listOf(PROTO, USAGE, KIOSK, ADVERT, SCAN, LEGACY)) {
        for (name in listOf("onCreateAsync", "onDestroyAsync")) {
            add(callTarget(clazz, name, emptyList(), "V", if (clazz == LEGACY) 17 else 1,
                "Lz5;->$name()V", afterSuper = true))
        }
    }
    add(callTarget("Leff;", "a", listOf("I", "I", "Lpl/neptis/libraries/network/model/analytics/IAnalyticsDataModel;"),
        "V", 9, "Ljava/lang/System;->currentTimeMillis()J"))
    add(callTarget("Lwy;", "g", emptyList(), "V", 1, "Lasm;->e(Ljava/lang/Object;Z)V"))
    add(callTarget("Lgp1;", "e", listOf("Lxy;"), "V", 1,
        "Landroid/database/sqlite/SQLiteDatabase;->beginTransaction()V"))
    add(callTarget("Lgp1;", "o", listOf("Lz30;"), "V", 18, "Lrt5;->e(Logf;)V"))
    add(callTarget(KIOSK, "onNewEventData", listOf("Lyaa;"), "V", 18,
        "Lce8;-><init>(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lbm3;I)V"))
    add(callTarget(KIOSK, "sendIfPossible", emptyList(), "V", 18, "Lrt5;->e(Logf;)V"))
    add(callTarget(ADVERT, "scheduleSendJob", listOf("J"), "V", 18,
        "Lc1;-><init>(JLpl/neptis/features/adsbanners/advertstatistics/AdvertStatisticsManager;Lbm3;)V"))
    // Ad targeting sends position/movement even when no ad is displayed. Stop
    // these dedicated producers before location reads and request construction;
    // preserve shared map, route, traffic, account and user-report transports.
    add(Target(Fingerprint(definingClass = AUDIO_ADS, name = "sendAudioNotificationRequest",
        parameters = emptyList(), returnType = "V", accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
        filters = listOf(
            methodCall(smali = "Liya;->getLocation()Lsva;"),
            methodCall(smali = "Lob8;-><init>(FFII)V"),
            methodCall(smali = "Lrt5;->e(Logf;)V")
        )), "return-void"))
    add(Target(Fingerprint(definingClass = AUDIO_ADS, name = "sendAudioNotificationExecutedRequest",
        parameters = listOf("Ll11;"), returnType = "V", accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
        filters = listOf(
            string("campaign_id"), string("source"),
            methodCall(smali = "Leff;->a(IILpl/neptis/libraries/network/model/analytics/IAnalyticsDataModel;)V"),
            methodCall(smali = "Lu11;-><init>(JIJ)V"),
            methodCall(smali = "Lrt5;->e(Logf;)V")
        )), "return-void"))
    add(Target(Fingerprint(definingClass = BANNER_ADS, name = "sendLocationAdvertRequest",
        parameters = emptyList(), returnType = "V", accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
        filters = listOf(
            methodCall(smali = "Lpl9;-><init>()V"),
            methodCall(smali = "Liya;->getLocation()Lsva;"),
            methodCall(smali = "Lcm8;-><init>(JIILzm3;)V"),
            methodCall(smali = "Lpl9;->y(Lcm8;)V"),
            methodCall(smali = "Lrt5;->e(Logf;)V")
        )), "return-void"))
    add(Target(Fingerprint(definingClass = BANNER_ADS, name = "analyticsSend",
        parameters = listOf("Lwva;"), returnType = "V", accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
        filters = listOf(
            fieldAccess(opcode = Opcode.SGET_OBJECT, smali = "Ld5j;->BANNER_VIEW:Ld5j;"),
            methodCall(smali = "Lmt;-><init>(JLs5j;Ld5j;JI)V"),
            methodCall(smali = "Lasm;->e(Ljava/lang/Object;Z)V")
        )), "return-void"))
    add(callTarget(SCAN, "onTick", listOf("I"), "V", 1, "Lz5;->getHandler()Lgd9;"))
    // Suspend Unit methods must return Unit, not null or COROUTINE_SUSPENDED.
    add(callTarget(PROTO, "sendAnalytics", listOf("Lbz;", "Lbm3;"), "Ljava/lang/Object;", 18,
        "Liba;-><init>(Logf;)V", "sget-object v0, Ldzk;->a:Ldzk;\nreturn-object v0"))
    add(callTarget(PROTO, "sendFlutterAnalytics", listOf("Lc57;", "Lbm3;"), "Ljava/lang/Object;", 18,
        "Liba;-><init>(Logf;)V", "sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;\nreturn-object v0"))
    add(callTarget(USAGE, "schedulePackageSending", listOf("Lw3l;"), "La4a;", 18,
        "Ltof;->o(Lgna;)Lmna;", "const/4 v0, 0x0\nreturn-object v0"))
    // SDK's existing inert implementation keeps host callbacks safe. No SDK
    // initialization, consent changes, worker scheduling or beacon scanning.
    for ((name, params) in listOf("create" to listOf("Landroid/content/Context;", "Ljava/lang/String;"),
        "getInstance" to emptyList())) {
        add(callTarget(PROXI, name, params, PROXI, 9, "Lihf;-><init>(Landroid/content/Context;Ljava/lang/String;)V",
            "new-instance v0, Lc06;\ninvoke-direct {v0}, Lc06;-><init>()V\nreturn-object v0"))
    }
    // Persisted WorkManager jobs run through the shared AndroidX service, so
    // disabling SDK factories and manifest components alone cannot stop them.
    for (clazz in listOf("Lcloud/proxi/sdk/jobs/EventReactionsSyncWorker;",
        "Lcloud/proxi/sdk/wifi/WifiScanWorker;")) {
        add(callTarget(clazz, "e", listOf("Lbm3;"), "Ljava/lang/Object;", 17,
            "Lcloud/proxi/ProxiCloudSdk;->getComponent()Lk63;",
            "new-instance v0, Lksa;\ninvoke-direct {v0}, Lksa;-><init>()V\nreturn-object v0"))
    }
    // Notinote's passive Wi-Fi/beacon samples are separate from ProxiCloud.
    // Leave local tag/button callbacks intact; stop storage, sends and old jobs.
    for ((clazz, field) in listOf(
        "Lme/notinote/sdk/service/synchronize/workers/BeaconSampleDozeSendingWorker;" to "e",
        "Lme/notinote/sdk/service/synchronize/workers/SynchronizeWorker;" to "g")) {
        add(callTarget(clazz, "c", emptyList(), "Lgsa;", 17,
            "Lme/notinote/sdk/util/Log;->d(Ljava/lang/String;)V",
            "iget-object v0, p0, $clazz->$field:Lb2i;\n" +
                "new-instance v1, Lksa;\ninvoke-direct {v1}, Lksa;-><init>()V\n" +
                "invoke-virtual {v0, v1}, Lb2i;->g(Ljava/lang/Object;)Z\nreturn-object v0"))
    }
    add(callTarget("Lme/notinote/sdk/service/network/communication/NetworkService;", "c",
        listOf("Lme/notinote/sdk/service/network/communication/ProtobufRequest;"), "V", 17,
        "Lme/notinote/sdk/service/network/communication/CommunicationFactory;->a(Lme/notinote/sdk/service/network/communication/NetworkService;Lme/notinote/sdk/service/network/communication/ProtobufRequest;)Lme/notinote/sdk/service/network/communication/SocketCommunication;"))
    add(callTarget("Lme/notinote/sdk/service/wifi/WifiService;", "a",
        listOf("Lme/notinote/sdk/service/wifi/WifiService;", "Ljava/util/List;"), "V", 25,
        "Lme/notinote/sdk/service/wakelock/GeneralWakeLock;->a(J)V"))
    add(callTarget("Lme/notinote/sdk/service/control/storage/ProtobufBeaconStorage;", "c",
        listOf("Ljava/util/List;", "Ldm3;"), "Ljava/lang/Object;", 17,
        "Lme/notinote/sdk/data/repositories/DataRepository;->h(Ljava/util/ArrayList;Ldm3;)Ljava/lang/Object;",
        "sget-object v0, Ldzk;->a:Ldzk;\nreturn-object v0"))
    add(callTarget("Ldtn;", "b", emptyList(), "I", 17, "Lyen;->i0()Z",
        "const/4 v0, 0x1\nreturn v0")) // Firebase: permanently deactivated
    add(callTarget("Low4;", "i", emptyList(), "Z", 131073, "Low6;->k()Z",
        "const/4 v0, 0x0\nreturn v0")) // Crashlytics data-collection arbiter
    add(Target(Fingerprint(definingClass = "Lbva;", name = "a", parameters = emptyList(),
        returnType = "Ljava/lang/Boolean;", accessFlags = AccessFlags.getAccessFlagsForMethod(17).toList(),
        filters = listOf(string("firebase_sessions_enabled"))),
        "sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;\nreturn-object v0"))
    add(callTarget(CRASH, "sendUnsentReports", emptyList(), "V", 1, "Lcnj;->d(Ljava/lang/Object;)V"))
    add(callTarget(CRASH, "log", listOf("Ljava/lang/String;"), "V", 1,
        "Ljava/lang/System;->currentTimeMillis()J"))
    add(Target(Fingerprint(definingClass = CRASH, name = "recordException", returnType = "V",
        parameters = listOf("Ljava/lang/Throwable;"), accessFlags = listOf(AccessFlags.PUBLIC),
        filters = listOf(string("A null value was passed to recordException. Ignoring."))), "return-void"))
    // Facebook auto-event/advertising-ID gates and explicit/cached app events.
    add(callTarget("Lx5l;", "b", emptyList(), "Z", 25, "La47;->a()Z", "const/4 v0, 0x0\nreturn v0"))
    add(Target(Fingerprint(definingClass = "Lx5l;", name = "a", parameters = emptyList(),
        returnType = "Z", accessFlags = AccessFlags.getAccessFlagsForMethod(17).toList(), filters = listOf(string("auto_log_app_events_enabled"))),
        "const/4 v0, 0x0\nreturn v0"))
    add(Target(Fingerprint(definingClass = "Lfp0;", name = "e", returnType = "V", accessFlags = AccessFlags.getAccessFlagsForMethod(17).toList(),
        parameters = listOf("Ljava/lang/String;", "Ljava/lang/Double;", "Landroid/os/Bundle;", "Z", "Ljava/util/UUID;", "Lxgd;"),
        filters = listOf(methodCall(smali = "Leo0;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Landroid/os/Bundle;ZZLjava/util/UUID;Lxgd;)V"))), "return-void"))
    add(Target(Fingerprint(definingClass = "Lko0;", name = "d", parameters = listOf("Lr47;"),
        returnType = "V", accessFlags = AccessFlags.getAccessFlagsForMethod(25).toList(), filters = listOf(string("com.facebook.sdk.APP_EVENTS_FLUSHED"))), "return-void"))
}

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Stops Yanosik's usage analytics, ad statistics and location targeting, Wi-Fi/app scans, ProxiCloud tracking, " +
        "Notinote sample uploads, Firebase analytics and crash uploads, Facebook app events, and RevenueCat requests and diagnostics. " +
        "RevenueCat purchases and subscription syncing are disabled too. Online navigation and account " +
        "requests still send the data they need; this isn't an offline mode or a network-wide blocker."
) {
    compatibleWith(COMPATIBILITY_YANOSIK)
    dependsOn(referenceVersionPatch, disableTelemetryResourcesPatch)
    execute {
        // Resolve every required target before mutating any method.
        val resolved = telemetryTargets.map { it to it.fingerprint.method }
        resolved.forEach { (target, method) ->
            if (target.afterSuper) {
                check(method.implementation!!.instructions.first().opcode == Opcode.INVOKE_SUPER) {
                    "Telemetry lifecycle must start with its original base call: ${method.definingClass}->${method.name}"
                }
                method.addInstruction(1, "return-void")
            } else method.addInstructions(0, target.prefix)
        }
    }
}
