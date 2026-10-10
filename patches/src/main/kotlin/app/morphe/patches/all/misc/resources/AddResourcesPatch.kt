/*
 * Copyright 2025 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

package app.morphe.patches.all.misc.resources

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.morphe.patches.util.resource.StringResourceSanitizer.sanitizeAndroidResourceString
import app.morphe.util.forEachChildElement
import app.morphe.util.getNode
import app.morphe.util.inputStreamFromBundledResource
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.Locale
import java.util.logging.Logger

/**
 * If any added string resources replace existing strings in the target app.
 * If this is not enabled then ARSCLib still replaces the existing strings without any issues,
 * but it's not clear if the behavior is intentional or coincidentally works even though the
 * modified XML is not valid. Apktool crashes if duplicates are present and this must be enabled.
 */
private var patchStringsReplaceExisting = false

/**
 * Set patchStringsReplaceExisting
 */
fun setPatchStringsReplaceExisting(replaceExisting: Boolean) {
    patchStringsReplaceExisting = replaceExisting
}

// Locale mappings for the retained shared resource translations.
internal val localesAll = listOf(
    AppLocale("", ""), // Default English locale. Must be first.
    AppLocale("ar-rSA", "ar", false),
    AppLocale("de-rDE", "de", false),
    AppLocale("es-rES", "es", false),
    AppLocale("fi-rFI", "fi", false),
    AppLocale("fr-rFR", "fr", false),
    AppLocale("hi-rIN", "hi", false),
    AppLocale("hu-rHU", "hu", false),
    AppLocale("it-rIT", "it", false),
    AppLocale("ja-rJP", "ja", false),
    AppLocale("ko-rKR", "ko", false),
    AppLocale("ms-rMY", "ms", false),
    AppLocale("nl-rNL", "nl", false),
    AppLocale("pl-rPL", "pl", false),
    AppLocale("pt-rBR", "pt-rBR", false),
    AppLocale("pt-rPT", "pt-rPT", false),
    AppLocale("ru-rRU", "ru", false),
    AppLocale("th-rTH", "th", false),
    AppLocale("tr-rTR", "tr", false),
    AppLocale("uk-rUA", "uk", false),
    AppLocale("vi-rVN", "vi", false),
    AppLocale("zh-rCN", "zh-rCN", false),
    AppLocale("zh-rTW", "zh-rTW", false),

    AppLocale("af-rZA", "af", false),
    AppLocale("am-rET", "am", false),
    AppLocale("as-rIN", "as", false),
    AppLocale("az-rAZ", "az", false),
    AppLocale("be-rBY", "be", false),
    AppLocale("bg-rBG", "bg", false),
    AppLocale("bn-rBD", "bn", false),
    AppLocale("bs-rBA", "bs", false),
    AppLocale("ca-rES", "ca", false),
    AppLocale("cs-rCZ", "cs", false),
    AppLocale("da-rDK", "da", false),
    AppLocale("el-rGR", "el", false),
    AppLocale("et-rEE", "et", false),
    AppLocale("eu-rES", "eu", false),
    AppLocale("fa-rIR", "fa", false),
    AppLocale("fil-rPH", "tl", false),
    AppLocale("gl-rES", "gl", false),
    AppLocale("gu-rIN", "gu", false),
    AppLocale("hr-rHR", "hr", false),
    AppLocale("hy-rAM", "hy", false),
    AppLocale("in-rID", "in", false),
    AppLocale("is-rIS", "is", false),
    AppLocale("iw-rIL", "iw", false),
    AppLocale("ka-rGE", "ka", false),
    AppLocale("kk-rKZ", "kk", false),
    AppLocale("km-rKH", "km", false),
    AppLocale("kn-rIN", "kn", false),
    AppLocale("ky-rKG", "ky", false),
    AppLocale("lo-rLA", "lo", false),
    AppLocale("lt-rLT", "lt", false),
    AppLocale("lv-rLV", "lv", false),
    AppLocale("mk-rMK", "mk", false),
    AppLocale("ml-rIN", "ml", false),
    AppLocale("mn-rMN", "mn", false),
    AppLocale("mr-rIN", "mr", false),
    AppLocale("my-rMM", "my", false),
    AppLocale("nb-rNO", "nb", false),
    AppLocale("ne-rNP", "ne", false),
    AppLocale("or-rIN", "or", false),
    AppLocale("pa-rIN", "pa", false),
    AppLocale("ro-rRO", "ro", false),
    AppLocale("sk-rSK", "sk", false),
    AppLocale("si-rLK", "si", false),
    AppLocale("sl-rSI", "sl", false),
    AppLocale("sq-rAL", "sq", false),
    AppLocale("sr-rCS", "b+sr+Latn", false),
    AppLocale("sr-rSP", "sr", false),
    AppLocale("sv-rSE", "sv", false),
    AppLocale("sw-rKE", "sw", false),
    AppLocale("ta-rIN", "ta", false),
    AppLocale("te-rIN", "te", false),
    AppLocale("ur-rIN", "ur", false),
    AppLocale("uz-rUZ", "uz", false),
    AppLocale("zu-rZA", "zu", false),
    AppLocale("ga-rIE", "ga", false),
    AppLocale("kmr-rTR", "kmr", false),
    AppLocale("ckb-rIR", "ckb", false)
)

internal class AppLocale(
    private val srcLocale: String,
    private val destLocale: String,
    val isBuiltInLanguage: Boolean = true
) {
    fun isDefaultLocale() = srcLocale.isEmpty()

    fun getSrcLocaleFolderName() = getValuesFolderName(srcLocale)
    fun getDestLocaleFolderName() = getValuesFolderName(destLocale)

    override fun toString(): String {
        return "AppLocale(srcLocale='${getSrcLocaleFolderName()}', destLocale='${getDestLocaleFolderName()}', " +
                "isBuiltInLanguage=$isBuiltInLanguage)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppLocale

        if (srcLocale != other.srcLocale) return false
        if (destLocale != other.destLocale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = srcLocale.hashCode()
        result = 31 * result + destLocale.hashCode()
        return result
    }

    companion object {
        private fun getValuesFolderName(localeName: String): String {
            val folderName = "values"

            return if (localeName.isEmpty()) {
                folderName
            } else {
                "$folderName-$localeName"
            }
        }
    }
}

private enum class BundledResourceType {
    // Add more resource XML files as needed.
    ARRAYS,
    COLORS,
    STRINGS;

    override fun toString(): String {
        return super.toString().lowercase(Locale.US)
    }
}

private val appsToInclude = mutableSetOf<String>()

internal lateinit var locales : List<AppLocale>

internal fun setAddResourceLocale(appLocale: List<AppLocale>) {
    locales = appLocale
}

/**
 * Add all resources for the given app.
 */
internal fun addAppResources(appId: String) {
    appsToInclude.add(appId)
}

internal fun createResourceDestinationDirectoryIfNeeded(
    locale: AppLocale,
    logger: Logger,
    destSubPath: String,
    destFile: File
) {
    if (!destFile.exists()) {
        if (locale.isBuiltInLanguage) {
            // Either the user provided a bad APKM that doesn't have all languages,
            // or the application removed a language from the universal APK releases.
            logger.warning {
                "!!! Provided app does not contain all region localizations. " +
                        "Locale: $locale does not exist in provided app file: $destSubPath"
            }
        }

        destFile.parentFile?.mkdirs()
        if (!destFile.createNewFile()) throw IllegalStateException()
        destFile.writeText(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>"
        )
    }
}

/**
 * A destination resource file that is kept open while all apps add their resources to it.
 */
private class DestinationFile(
    val document: Document,
    val resourcesNode: Node,
    val existingNodes: MutableMap<Pair<String, String>, Node>
)

internal val addResourcesPatch = resourcePatch(
    description = "Add resources such as strings or arrays to the app."
) {

    val defaultResourcesAdded = mutableSetOf<String>()

    finalize {
        val logger = Logger.getLogger(AppLocale.Companion::class.java.name)

        // Default resources added by each app and by the apps before it.
        val appDefaultResources = mutableMapOf<String, Set<String>>()

        // Destination files of the locale being processed. Multiple apps add resources to the
        // same files, and keeping them open parses and writes each file only once.
        val destinationFiles = mutableMapOf<String, DestinationFile>()

        fun openDestinationFile(locale: AppLocale, destSubPath: String): DestinationFile {
            destinationFiles[destSubPath]?.let { return it }

            val destFile = this@finalize[destSubPath]
            createResourceDestinationDirectoryIfNeeded(locale, logger, destSubPath, destFile)

            val destDoc = document(destSubPath)
            val destResourceNode = destDoc.getNode("resources")

            val existingNodes = HashMap<Pair<String, String>, Node>()
            if (patchStringsReplaceExisting) {
                // Build lookup table once per destination file.
                val children = destResourceNode.childNodes
                for (i in 0 until children.length) {
                    val node = children.item(i)
                    if (node.nodeType == Node.ELEMENT_NODE) {
                        val el = node as Element
                        existingNodes[el.tagName to el.getAttribute("name")] = el
                    }
                }
            }

            return DestinationFile(destDoc, destResourceNode, existingNodes).also {
                destinationFiles[destSubPath] = it
            }
        }

        fun addResourcesFromFile(
            appId: String,
            locale: AppLocale,
            resourceType: BundledResourceType
        ) {
            val isDefaultLocale = locale.isDefaultLocale()
            val srcFolderName = locale.getSrcLocaleFolderName()
            val srcSubPath = "$srcFolderName/$appId/$resourceType.xml"
            val destSubPath = "res/${locale.getDestLocaleFolderName()}/$resourceType.xml"

            val srcStream = inputStreamFromBundledResource(
                "addresources", srcSubPath
            )

            if (srcStream == null) {
                // String files are expected but other resource types are optional.
                if (resourceType == BundledResourceType.STRINGS) {
                    throw IllegalArgumentException("Could not find: $srcSubPath")
                }
                return
            }

            srcStream.use {
                val destination = openDestinationFile(locale, destSubPath)
                val destDoc = destination.document
                val destResourceNode = destination.resourcesNode
                val existingNodes = destination.existingNodes
                val defaultResources = appDefaultResources[appId]

                document(srcStream).use { srcDoc ->
                    val localeStringsAdded = mutableSetOf<String>()

                    srcDoc.getElementsByTagName(
                        "resources"
                    ).item(0)?.forEachChildElement { srcNode ->
                        val resourceName = srcNode.getAttributeNode("name").value
                        if (resourceType == BundledResourceType.STRINGS) {
                            // Check for bad text strings that will fail resource compilation.
                            val textContent = srcNode.textContent
                            val sanitized = sanitizeAndroidResourceString(
                                resourceName, textContent, destSubPath
                            )
                            if (textContent != sanitized) {
                                srcNode.textContent = sanitized
                            }
                        }

                        if (!localeStringsAdded.add(resourceName)) {
                            logger.warning(
                                "Duplicate string resource is declared: $srcFolderName " +
                                        "resource: $resourceName"
                            )
                            return@forEachChildElement
                        }

                        if (isDefaultLocale) {
                            // Duplicate check already handled above.
                            defaultResourcesAdded.add(resourceName)
                        } else if (defaultResources?.contains(resourceName) != true) {
                            logger.fine {
                                "Ignoring removed default resource for locale " +
                                        "(Issue will be fixed after next Crowdin sync): " +
                                        "$srcFolderName resource: $resourceName"
                            }
                            return@forEachChildElement
                        }

                        // Remove existing resources with the same name.
                        // ARSCLib doesn't check for duplicates and uses the last added,
                        // but Apktool crashes if duplicates exist.
                        val key = srcNode.tagName to resourceName
                        if (patchStringsReplaceExisting) {
                            existingNodes[key]?.let { existing ->
                                destResourceNode.removeChild(existing)
                            }
                        }

                        // Import and append.
                        val imported = destDoc.importNode(srcNode, true)
                        destResourceNode.appendChild(imported)

                        if (patchStringsReplaceExisting) {
                            // Resources of the next apps replace this one.
                            existingNodes[key] = imported
                        }
                    }
                }
            }
        }

        locales.forEach { locale ->
            appsToInclude.forEach { app ->
                BundledResourceType.entries.forEach { type ->
                    addResourcesFromFile(app, locale, type)
                }

                if (locale.isDefaultLocale()) {
                    appDefaultResources[app] = defaultResourcesAdded.toSet()
                }
            }

            destinationFiles.values.forEach { it.document.close() }
            destinationFiles.clear()
        }
    }
}
