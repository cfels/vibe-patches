/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2257
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.all.misc.installer

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.all.misc.fix.changepackageinstaller.changePackageInstallerPatch

@Suppress("unused")
val changeInstallerSource = resourcePatch(
    name = "Pick install source",
    description = "Sets the store the app sees as its installer, like Google Play.",
    default = false
) {
    val packageInstallerName = stringOption(
        key = "packageInstallerName",
        default = "com.android.vending",
        values = mapOf(
            "Google Play" to "com.android.vending",
            "Samsung Galaxy Store" to "com.sec.android.app.samsungapps",
            "Amazon Appstore" to "com.amazon.venezia",
            "Huawei AppGallery" to "com.huawei.appmarket",
            "Xiaomi GetApps" to "com.xiaomi.mipicks"
        ),
        title = "App store to show"
    )

    dependsOn(
        changePackageInstallerPatch({ packageInstallerName.value!! })
    )
}
