import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar

group = "app.morphe"

patches {
    about {
        name = "Vibe Patch"
        description = "Yanosik patches and some universal stuff"
        source = "https://github.com/cfels/vibe-patches"
        author = "Moxiu"
        contact = "https://moxiu.vacpro.fyi/contact.html"
        website = "https://moxiu.vacpro.fyi"
        license = "GNU General Public License v3.0, with additional GPL section 7 requirements"
    }
}

val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")
val yanosikApk = providers.gradleProperty("yanosikApk")

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.morphe.patches.library)

    patchListGeneratorClasspath(libs.gson)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks {
    // buildAndroid adds DEX to the jar task's output in place. Rename that
    // archive itself rather than copying it into a second installable bundle.
    withType<Jar>().configureEach {
        archiveBaseName.set("vibe-patch")
    }

    register("buildVibePatch") {
        description = "Builds the Vibe Patch Android bundle (alias for buildAndroid)"
        group = "build"
        dependsOn("buildAndroid")
    }

    test {
        useJUnitPlatform()
        yanosikApk.orNull?.let { systemProperty("yanosikApk", it) }
    }

    register<JavaExec>("checkStringResources") {
        description = "Checks resource strings for invalid formatting"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.patches.util.resource.CheckStringResourcesKt")
    }

    val metadataClasses = register<Zip>("metadataClasses") {
        from(sourceSets["main"].output.classesDirs)
        destinationDirectory.set(layout.buildDirectory.dir("metadata"))
        archiveFileName.set("patches.jar")
    }

    register<JavaExec>("generatePatchesList") {
        description = "Generates patch metadata from JVM classes without building Android bundles"
        dependsOn(metadataClasses)
        classpath = files(metadataClasses.flatMap { it.archiveFile }) +
            configurations["runtimeClasspath"] + patchListGeneratorClasspath
        workingDir(projectDir)
        mainClass.set(rootProject.file(".github/scripts/ExportPatchMetadata.java").absolutePath)
        args(metadataClasses.flatMap { it.archiveFile }.get().asFile.absolutePath, project.version.toString())
    }

    register<JavaExec>("validateYanosik") {
        description = "Applies Yanosik patches to a reference APK and validates serialized DEX"
        group = "verification"
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("app.morphe.patches.yanosik.ValidateYanosikKt")
        doFirst {
            val apk = yanosikApk.orNull
                ?: throw GradleException("validateYanosik requires -PyanosikApk=/absolute/path/to/reference.apk")
            setArgs(listOf(apk, layout.buildDirectory.dir("yanosik-validation").get().asFile.absolutePath))
        }
    }
}
