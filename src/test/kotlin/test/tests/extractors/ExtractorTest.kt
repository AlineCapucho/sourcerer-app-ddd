package test.tests.extractors

import app.domain.repository.valueobject.CommitStats
import app.domain.repository.valueobject.DiffContent
import app.domain.repository.valueobject.DiffFile
import app.domain.repository.valueobject.DiffRange
import app.domain.repository.valueobject.ChangeType
import app.infrastructure.extractor.DefaultCommitExtractionService
import app.infrastructure.extractor.LanguageDetector
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtractorTest : Spek({
    val extractionService = DefaultCommitExtractionService()

    given("file with known extension") {
        it("extracts language stats for python file") {
            val lines = listOf("x = 1", "y = 2", "z = x + y")
            val diffFile = DiffFile(
                path = "test.py",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 3)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            assertTrue(stats.isNotEmpty())
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("python", langStat!!.tech)
            assertEquals(3, langStat.numLinesAdded)
        }

        it("extracts language stats for kotlin file") {
            val lines = listOf("fun main() {", "    println(\"Hello\")", "}")
            val diffFile = DiffFile(
                path = "Main.kt",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 3)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("kotlin", langStat!!.tech)
            assertEquals(3, langStat.numLinesAdded)
        }

        it("extracts language stats for javascript file") {
            val lines = listOf("const x = 1;", "console.log(x);")
            val diffFile = DiffFile(
                path = "app.js",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 2)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("javascript", langStat!!.tech)
            assertEquals(2, langStat.numLinesAdded)
        }

        it("extracts language stats for typescript file") {
            val lines = listOf("const x: number = 1;", "export default x;")
            val diffFile = DiffFile(
                path = "app.ts",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 2)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("typescript", langStat!!.tech)
        }

        it("extracts language stats for java file") {
            val lines = listOf("public class Main {", "}")
            val diffFile = DiffFile(
                path = "Main.java",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 2)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("java", langStat!!.tech)
        }

        it("extracts language stats for ruby file") {
            val lines = listOf("def hello", "  puts 'hello'", "end")
            val diffFile = DiffFile(
                path = "hello.rb",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 3)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("ruby", langStat!!.tech)
        }

        it("extracts language stats for go file") {
            val lines = listOf("package main", "func main() {", "}")
            val diffFile = DiffFile(
                path = "main.go",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 3)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            val langStat = stats.find { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStat != null)
            assertEquals("go", langStat!!.tech)
        }
    }

    given("file with restricted extension") {
        it("ignores .min.js files") {
            val lines = listOf("var a=1;var b=2;")
            val diffFile = DiffFile(
                path = "bundle.min.js",
                changeType = ChangeType.ADD,
                new = DiffContent(lines, listOf(DiffRange(0, 1)))
            )

            val stats = extractionService.extractStats(listOf(diffFile))
            assertTrue(stats.isEmpty())
        }
    }

    given("multiple files in same commit") {
        it("extracts stats for each language") {
            val pyLines = listOf("x = 1")
            val jsLines = listOf("const y = 2;")
            val pyFile = DiffFile(
                path = "script.py",
                changeType = ChangeType.ADD,
                new = DiffContent(pyLines, listOf(DiffRange(0, 1)))
            )
            val jsFile = DiffFile(
                path = "app.js",
                changeType = ChangeType.ADD,
                new = DiffContent(jsLines, listOf(DiffRange(0, 1)))
            )

            val stats = extractionService.extractStats(listOf(pyFile, jsFile))
            val pyStats = stats.filter { it.tech == "python" }
            val jsStats = stats.filter { it.tech == "javascript" }
            assertEquals(1, pyStats.size)
            assertEquals(1, jsStats.size)
        }
    }

    given("tokenizer") {
        it("tokenizes code lines correctly") {
            val tokens = extractionService.tokenize("val x = foo.bar(1, 2)")
            assertTrue(tokens.contains("val"))
            assertTrue(tokens.contains("x"))
            assertTrue(tokens.contains("foo"))
            assertTrue(tokens.contains("bar"))
        }

        it("removes string literals") {
            val tokens = extractionService.tokenize("println(\"hello world\")")
            assertTrue(!tokens.contains("hello"))
            assertTrue(!tokens.contains("world"))
        }
    }

    given("language detection") {
        it("detects languages by extension") {
            assertEquals("python", LanguageDetector.detectByExtension("py"))
            assertEquals("kotlin", LanguageDetector.detectByExtension("kt"))
            assertEquals("java", LanguageDetector.detectByExtension("java"))
            assertEquals("javascript", LanguageDetector.detectByExtension("js"))
            assertEquals("typescript", LanguageDetector.detectByExtension("ts"))
            assertEquals("ruby", LanguageDetector.detectByExtension("rb"))
            assertEquals("go", LanguageDetector.detectByExtension("go"))
            assertEquals("rust", LanguageDetector.detectByExtension("rs"))
            assertEquals("swift", LanguageDetector.detectByExtension("swift"))
            assertEquals("csharp", LanguageDetector.detectByExtension("cs"))
            assertEquals("cpp", LanguageDetector.detectByExtension("cpp"))
            assertEquals("c", LanguageDetector.detectByExtension("c"))
            assertEquals("php", LanguageDetector.detectByExtension("php"))
            assertEquals("scala", LanguageDetector.detectByExtension("scala"))
            assertEquals("elixir", LanguageDetector.detectByExtension("ex"))
            assertEquals("dart", LanguageDetector.detectByExtension("dart"))
            assertEquals(null, LanguageDetector.detectByExtension("unknown"))
        }
    }
})
