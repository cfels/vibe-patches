/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik.privacy

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.yanosik.shared.referenceVersionPatch
import org.w3c.dom.Document
import org.w3c.dom.Element

private val collectionMetadata = mapOf(
    "firebase_analytics_collection_deactivated" to "true",
    "firebase_analytics_collection_enabled" to "false",
    "google_analytics_automatic_screen_reporting_enabled" to "false",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_sessions_enabled" to "false",
    "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false"
)

// Disable OS entry points as well as the host SDK. Existing jobs, boot/update
// receivers and geofence PendingIntents must not revive tracking after an update.
internal fun disableTelemetryManifest(document: Document) {
    val application = document.getElementsByTagName("application").item(0) as Element
    val children = (0 until application.childNodes.length).mapNotNull {
        application.childNodes.item(it) as? Element
    }
    val proxiComponents = children.filter {
        it.tagName in setOf("service", "receiver", "provider") &&
            it.getAttribute("android:name").startsWith("cloud.proxi.")
    }
    check(proxiComponents.any { it.getAttribute("android:name") == "cloud.proxi.job.OnBootCompletedReceiver" }) {
        "Missing required ProxiCloud manifest entry points"
    }
    proxiComponents.forEach { it.setAttribute("android:enabled", "false") }
    for ((name, value) in collectionMetadata) {
        val matches = children.filter { it.tagName == "meta-data" && it.getAttribute("android:name") == name }
        check(matches.size <= 1) { "Duplicate telemetry metadata: $name" }
        val node = matches.singleOrNull() ?: document.createElement("meta-data").also {
            it.setAttribute("android:name", name)
            application.appendChild(it)
        }
        node.removeAttribute("android:resource")
        node.setAttribute("android:value", value)
    }
}

internal val disableTelemetryResourcesPatch = resourcePatch {
    dependsOn(referenceVersionPatch)
    execute {
        document("AndroidManifest.xml").use { disableTelemetryManifest(it) }
    }
}
