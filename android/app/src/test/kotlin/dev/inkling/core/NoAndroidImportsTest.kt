package dev.inkling.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoAndroidImportsTest {
    @Test
    fun coreHasNoAndroidImports() {
        val dir = File("src/main/kotlin/dev/inkling/core")
        assertTrue("core dir missing at ${dir.absolutePath}", dir.isDirectory)
        val offenders = dir.walk().filter { it.extension == "kt" }
            .filter { f -> f.readLines().any { it.trimStart().startsWith("import android") || it.trimStart().startsWith("import androidx") } }
            .map { it.name }.toList()
        assertTrue("core files import Android: $offenders", offenders.isEmpty())
    }
}
