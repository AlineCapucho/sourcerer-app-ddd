package test.utils

import app.domain.repository.entity.Author
import app.domain.repository.valueobject.Fact
import app.domain.shared.valueobject.Email
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun getFact(code: Int, key: Int, authorEmail: String? = null,
            facts: List<Fact>): Fact {
    val fact = facts.find { fact -> fact.code == code && fact.key == key &&
        (authorEmail == null || fact.authorEmail.value() == authorEmail) }
    assertNotNull(fact)
    return fact!!
}

fun assertFactInt(code: Int, key: Int, value: Int, authorEmail: String? = null,
                  facts: List<Fact>) {
    val fact = getFact(code, key, authorEmail, facts)
    assertEquals(value, fact.value.toInt())
}

fun assertNoFact(code: Int, key: Int, authorEmail: String? = null,
                 facts: List<Fact>) {
    val fact = facts.find { fact -> fact.code == code && fact.key == key &&
        (authorEmail == null || fact.authorEmail.value() == authorEmail) }
    assertNull(fact)
}

fun assertFactDouble(code: Int, key: Int, value: Double, authorEmail: String? = null,
                     facts: List<Fact>) {
    val fact = getFact(code, key, authorEmail, facts)
    assertTrue(Math.abs(value - fact.value.toDouble()) < 0.1,
        "Expected approximately <$value>, actual <${fact.value}>")
}
