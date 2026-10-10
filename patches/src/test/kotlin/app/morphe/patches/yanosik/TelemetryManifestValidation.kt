/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.yanosik

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

private data class ManifestNode(val name: String, val attributes: MutableMap<String, String>,
    val children: MutableList<ManifestNode> = mutableListOf())

// Independent binary-XML reader: compare resolved strings/typed values, not
// string-pool indexes or serialization order. No target Android code is run.
private fun readBinaryManifest(apk: File): ManifestNode {
    val bytes = ZipFile(apk).use { zip -> zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() } }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    fun short(offset: Int) = buffer.getShort(offset).toInt() and 0xffff
    fun int(offset: Int) = buffer.getInt(offset)
    check(short(0) == 3 && int(4) == bytes.size)
    var strings = emptyList<String>()
    var root: ManifestNode? = null
    val stack = mutableListOf<ManifestNode>()
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
                        return if (first and 0x80 != 0) ((first and 0x7f) shl 8) or (bytes[cursor++].toInt() and 0xff) else first
                    }
                    if (utf8) {
                        length8() // UTF-16 character count precedes UTF-8 byte count.
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
                    val name = (if (namespace >= 0 && strings[namespace] == "http://schemas.android.com/apk/res/android") "android:" else "") + strings[int(attr + 4)]
                    val data = int(attr + 16)
                    val type = bytes[attr + 15].toInt() and 0xff
                    val value = when (type) {
                        3 -> strings[data]
                        0x12 -> if (data == 0) "false" else "true"
                        else -> "typed:$type:$data"
                    }
                    check(attributes.put(name, value) == null)
                }
                val node = ManifestNode(strings[int(pos + 20)], attributes)
                if (stack.isEmpty()) root = node else stack.last().children += node
                stack += node
            }
            0x103 -> stack.removeAt(stack.lastIndex)
        }
        pos += size
    }
    check(stack.isEmpty())
    return requireNotNull(root)
}

internal fun verifyTelemetryManifest(originalApk: File, resourcesApk: File) {
    val original = readBinaryManifest(originalApk)
    val patched = readBinaryManifest(resourcesApk)
    val metadata = mapOf(
        "firebase_analytics_collection_deactivated" to "true",
        "firebase_analytics_collection_enabled" to "false",
        "google_analytics_automatic_screen_reporting_enabled" to "false",
        "firebase_crashlytics_collection_enabled" to "false",
        "firebase_sessions_enabled" to "false",
        "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
        "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false"
    )
    val app = patched.children.single { it.name == "application" }
    fun isProxi(node: ManifestNode) = node.name in setOf("service", "receiver", "provider") &&
        node.attributes["android:name"]?.startsWith("cloud.proxi.") == true
    val components = app.children.filter(::isProxi)
    check(components.size == 16) { "Expected all sixteen ProxiCloud Android entry points" }
    check(components.all { it.attributes["android:enabled"] == "false" })
    for ((name, value) in metadata) {
        val node = app.children.single { it.name == "meta-data" && it.attributes["android:name"] == name }
        check(node.attributes["android:value"] == value) { "Incorrect compiled telemetry metadata: $name" }
    }
    fun normalize(node: ManifestNode): ManifestNode {
        if (isProxi(node)) node.attributes.remove("android:enabled")
        if (node.name == "application") node.children.removeAll {
            it.name == "meta-data" && it.attributes["android:name"] in metadata
        }
        node.children.forEach(::normalize)
        return node
    }
    check(normalize(original) == normalize(patched)) { "Unrelated compiled manifest content changed" }
    println("PASS compiled telemetry manifest: 16 ProxiCloud components disabled; 7 collection flags; unrelated manifest preserved")
}
