package test.tests.utils

import app.infrastructure.config.FileHelper
import app.infrastructure.config.FileHelper.toPath
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.nio.file.Paths
import kotlin.test.assertEquals

class FileHelperTest : Spek({
    given("relative path test") {
        it("resolves absolute paths correctly") {
            val home = System.getProperty("user.home")
            assertEquals(Paths.get("/Users/user/repo"), "/Users/user/repo".toPath())
            assertEquals(Paths.get("/Users/user/repo"),
                "/Users/user/../user/repo/../repo".toPath())
            assertEquals(Paths.get("$home/test"), "~/test".toPath())
            assertEquals(Paths.get("$home/test1"), "~/test/../test1".toPath())
        }
    }

    given("file extension extraction") {
        it("extracts common extensions") {
            assertEquals("py", FileHelper.getFileExtension("script.py"))
            assertEquals("kt", FileHelper.getFileExtension("Main.kt"))
            assertEquals("js", FileHelper.getFileExtension("app.js"))
            assertEquals("ts", FileHelper.getFileExtension("index.ts"))
        }

        it("handles .min.js as specific extension") {
            assertEquals(".min.js", FileHelper.getFileExtension("bundle.min.js"))
        }

        it("handles files without extension") {
            assertEquals("", FileHelper.getFileExtension("Makefile"))
        }

        it("handles nested paths") {
            assertEquals("java", FileHelper.getFileExtension("src/main/App.java"))
        }
    }
})
