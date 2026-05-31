package test.tests.hashers

import app.domain.repository.FactCodes
import app.domain.repository.entity.Author
import app.domain.shared.valueobject.Email
import app.infrastructure.extractor.DefaultMetaHashingService
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import test.utils.assertFactInt
import test.utils.assertNoFact
import kotlin.test.assertTrue

class MetaHasherTest : Spek({
    val repoRehash = "rehash"
    val metaService = DefaultMetaHashingService()

    given("repo for team size and commit share facts") {
        val authors = hashSetOf(
            Author(email = Email("ivanov@gmail.com"), name = "Alexander Ivanov"),
            Author(email = Email("maxim95@sourcerer.io"), name = "Maxim Zayac"),
            Author(email = Email("lyablonskaya@sourcerer.io"), name = "Lubov Yablonskaya"),
            Author(email = Email("aleks@riseup.net"), name = "Alexander Ivanov"),
            Author(email = Email("roman.romov@gmail.com"), name = "Roman Romov"),
            Author(email = Email("john@mail.mail"), name = "John Brown"),
            Author(email = Email("john123@mail.mail"), name = "Johnny Brown")
        )
        val commitsCount = hashMapOf(
            "ivanov@gmail.com" to 10,
            "john123@mail.mail" to 10,
            "john@mail.mail" to 10,
            "lyablonskaya@sourcerer.io" to 10,
            "maxim95@sourcerer.io" to 10,
            "roman.romov@gmail.com" to 10,
            "aleks@riseup.net" to 10
        )

        it("calculates team size") {
            val userEmails = listOf("john123@mail.mail", "john@mail.mail")
            val facts = metaService.calculateMetaFacts(
                repoRehash = repoRehash,
                authors = authors,
                commitsCount = commitsCount,
                userEmails = userEmails
            )

            assertFactInt(FactCodes.REPO_TEAM_SIZE, 0, 7,
                "john123@mail.mail", facts)
            assertFactInt(FactCodes.REPO_TEAM_SIZE, 0, 7,
                "john@mail.mail", facts)
        }

        it("calculates commit share for user") {
            val userEmails = listOf("john123@mail.mail")
            val facts = metaService.calculateMetaFacts(
                repoRehash = repoRehash,
                authors = authors,
                commitsCount = commitsCount,
                userEmails = userEmails
            )

            // john123 has 10 out of 70 total commits
            val shareFact = facts.find {
                it.code == FactCodes.COMMIT_SHARE &&
                    it.authorEmail.value() == "john123@mail.mail"
            }
            assertTrue(shareFact != null)
            val share = shareFact!!.value.toDouble()
            assertTrue(share > 0.1 && share < 0.2) // ~0.142
        }

        it("does not send commit share when user is not contributor") {
            val facts = metaService.calculateMetaFacts(
                repoRehash = repoRehash,
                authors = authors,
                commitsCount = commitsCount,
                userEmails = listOf()
            )

            val sharesFacts = facts.filter { it.code == FactCodes.COMMIT_SHARE }
            assertTrue(sharesFacts.isEmpty())
        }

        it("calculates colleagues count") {
            val userEmails = listOf("john123@mail.mail")
            val facts = metaService.calculateMetaFacts(
                repoRehash = repoRehash,
                authors = authors,
                commitsCount = commitsCount,
                userEmails = userEmails
            )

            val colleaguesFact = facts.find {
                it.code == FactCodes.COLLEAGUES &&
                    it.authorEmail.value() == "john123@mail.mail"
            }
            assertTrue(colleaguesFact != null)
            // 6 other contributors
            assertTrue(colleaguesFact!!.value.toInt() == 6)
        }
    }
})
