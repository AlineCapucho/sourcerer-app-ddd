package test.tests.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Test

/**
 * Testes de arquitetura com ArchUnit.
 * Valida os padrões DDD no modelo de domínio:
 * - Entidades residem no pacote entity
 * - Value Objects residem no pacote valueobject
 * - Repositórios (interfaces) residem no pacote repository
 * - Domain Services residem no pacote service
 * - Factories residem no pacote factory
 * - Eventos residem no pacote event
 */
class DomainModelRulesTest {

    private val importedClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("app.domain")

    // === REGRA: Entidades não dependem de frameworks externos ===

    @Test
    fun entitiesShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain entities must not depend on external frameworks")

        rule.check(importedClasses)
    }

    // === REGRA: Value Objects não dependem de frameworks externos ===

    @Test
    fun valueObjectsShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("..valueobject..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain value objects must not depend on external frameworks")

        rule.check(importedClasses)
    }

    // === REGRA: Interfaces de repositório não dependem de frameworks ===

    @Test
    fun repositoryInterfacesShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain repository interfaces must not depend on external frameworks")

        rule.check(importedClasses)
    }

    // === REGRA: Domain Services não dependem de frameworks ===

    @Test
    fun domainServicesShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..service..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain services must not depend on external frameworks")

        rule.check(importedClasses)
    }

    // === REGRA: Eventos de domínio não dependem de frameworks ===

    @Test
    fun domainEventsShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..event..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain events must not depend on external frameworks")

        rule.check(importedClasses)
    }

    // === REGRA: Factories não dependem de frameworks ===

    @Test
    fun factoriesShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..factory..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain factories must not depend on external frameworks")

        rule.check(importedClasses)
    }

    // === REGRA: Ports não dependem de frameworks ===

    @Test
    fun portsShouldNotDependOnFrameworks() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..port..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.eclipse.jgit..",
                "com.google.protobuf..",
                "com.github.kittinunf..",
                "com.fasterxml.jackson..",
                "io.reactivex..",
                "io.sentry..",
                "com.beust.."
            )
            .because("Domain ports must not depend on external frameworks")

        rule.check(importedClasses)
    }
}
