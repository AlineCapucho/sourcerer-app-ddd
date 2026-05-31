package test.tests.domain

import app.domain.repository.valueobject.*
import app.domain.shared.valueobject.Email
import app.domain.user.valueobject.Credentials
import app.domain.user.valueobject.UserEmail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ValueObjectTest : Spek({
    given("Email value object") {
        it("validates non-blank email") {
            assertFailsWith<IllegalArgumentException> {
                Email("")
            }
        }

        it("validates email format") {
            assertFailsWith<IllegalArgumentException> {
                Email("invalid-email")
            }
        }

        it("normalizes to lowercase") {
            val email = Email("Test@Domain.COM")
            assertEquals("test@domain.com", email.value())
        }

        it("equality is based on value") {
            assertEquals(Email("test@domain.com"), Email("TEST@DOMAIN.COM"))
        }
    }

    given("DiffRange value object") {
        it("validates non-negative start") {
            assertFailsWith<IllegalArgumentException> {
                DiffRange(-1, 5)
            }
        }

        it("validates end >= start") {
            assertFailsWith<IllegalArgumentException> {
                DiffRange(5, 3)
            }
        }

        it("calculates line count") {
            val range = DiffRange(2, 7)
            assertEquals(5, range.lineCount())
        }
    }

    given("Rehash value object") {
        it("validates non-blank") {
            assertFailsWith<IllegalArgumentException> {
                Rehash("")
            }
        }

        it("validates 64 character length") {
            assertFailsWith<IllegalArgumentException> {
                Rehash("abc123")
            }
        }

        it("validates hex characters only") {
            assertFailsWith<IllegalArgumentException> {
                Rehash("g" + "a".repeat(63))
            }
        }

        it("accepts valid SHA-256 hex") {
            val hash = "a".repeat(64)
            val rehash = Rehash(hash)
            assertEquals(hash, rehash.value())
        }
    }

    given("CommitStats value object") {
        it("validates non-negative lines added") {
            assertFailsWith<IllegalArgumentException> {
                CommitStats(numLinesAdded = -1)
            }
        }

        it("validates non-negative lines deleted") {
            assertFailsWith<IllegalArgumentException> {
                CommitStats(numLinesDeleted = -1)
            }
        }

        it("creates valid stats") {
            val stats = CommitStats(numLinesAdded = 10, numLinesDeleted = 5,
                type = CommitStats.TYPE_LANGUAGE, tech = "kotlin")
            assertEquals(10, stats.numLinesAdded)
            assertEquals(5, stats.numLinesDeleted)
            assertEquals("kotlin", stats.tech)
        }
    }

    given("DiffContent value object") {
        it("extracts diffs from ranges") {
            val content = listOf("line0", "line1", "line2", "line3", "line4")
            val ranges = listOf(DiffRange(1, 3), DiffRange(4, 5))
            val diffContent = DiffContent(content, ranges)

            val diffs = diffContent.getAllDiffs()
            assertEquals(listOf("line1", "line2", "line4"), diffs)
        }

        it("returns empty for no ranges") {
            val content = listOf("line0", "line1")
            val diffContent = DiffContent(content, listOf())
            assertEquals(listOf<String>(), diffContent.getAllDiffs())
        }
    }

    given("Credentials value object") {
        it("validates non-blank username") {
            assertFailsWith<IllegalArgumentException> {
                Credentials("", "hash123")
            }
        }

        it("validates non-blank password hash") {
            assertFailsWith<IllegalArgumentException> {
                Credentials("user", "")
            }
        }

        it("reports valid credentials") {
            val creds = Credentials("user", "hash123")
            assertEquals(true, creds.isValid())
        }
    }

    given("UserEmail value object") {
        it("validates email format") {
            assertFailsWith<IllegalArgumentException> {
                UserEmail("invalid")
            }
        }

        it("equality based on address") {
            val email1 = UserEmail("test@domain.com", primary = true)
            val email2 = UserEmail("test@domain.com", primary = false)
            assertEquals(email1, email2)
        }

        it("toString includes status") {
            val email = UserEmail("test@domain.com", primary = true, verified = true)
            val str = email.toString()
            assert(str.contains("Primary"))
            assert(str.contains("Confirmed"))
        }
    }

    given("ProcessEntry value object") {
        it("validates non-negative id") {
            assertFailsWith<IllegalArgumentException> {
                ProcessEntry(id = -1, status = 100)
            }
        }
    }
})
