extension {
    name = "extensions/yanosik/vibe-settings.mpe"
}

android {
    namespace = "app.morphe.extension.yanosik"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// This extension uses Java/Android APIs only. Do not merge the plugin's default
// Kotlin runtime into an app that already supplies its own Kotlin implementation.
configurations.matching {
    it.name.endsWith("CompileClasspath") || it.name.endsWith("RuntimeClasspath")
}.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}
