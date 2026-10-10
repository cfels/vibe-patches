package app.morphe.patches.yanosik

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class YanosikPatchTest {
    @Test
    fun `reference APK resolves all patches and serializes DEX without changing unrelated methods`() {
        val path = System.getProperty("yanosikApk")
        assumeTrue(!path.isNullOrBlank(), "Pass -PyanosikApk=/absolute/path/to/reference.apk")
        validateReferenceApk(File(path!!), createTempDirectory("yanosik-regression").toFile())
    }
}
