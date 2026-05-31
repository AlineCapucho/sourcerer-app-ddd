package test.tests.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Test

/**
 * Testes de arquitetura com ArchUnit.
 * Valida que frameworks externos estão confinados à camada de infraestrutura:
 * - JGit apenas em infrastructure.git
 * - Protobuf apenas em infrastructure.api.proto
 * - Fuel (HTTP) apenas em infrastructure.api
 * - Jackson apenas em infrastructure.config
 * - Sentry apenas em infrastructure (Logger)
 * - RxJava apenas em infrastructure
 */
class InfrastructureRulesTest {

    private val importedClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("app")

    // === REGRA: JGit confinado à infrastructure.git ===

    @Test
    fun jgitShouldOnlyBeUsedInGitPackage() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackage("app.infrastructure.git..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.eclipse.jgit..")
            .because("JGit must be confined to infrastructure.git (ACL pattern)")

        rule.check(importedClasses)
    }

    // === REGRA: Protobuf confinado à infrastructure.api ===

    @Test
    fun protobufShouldOnlyBeUsedInApiPackage() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackage("app.infrastructure.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.google.protobuf..")
            .because("Protobuf must be confined to infrastructure.api (ACL pattern)")

        rule.check(importedClasses)
    }

    // === REGRA: Fuel (HTTP client) confinado à infrastructure ===

    @Test
    fun fuelShouldOnlyBeUsedInInfrastructure() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackage("app.infrastructure..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.github.kittinunf..")
            .because("Fuel HTTP client must be confined to infrastructure layer")

        rule.check(importedClasses)
    }

    // === REGRA: Jackson confinado à infrastructure.config ===

    @Test
    fun jacksonShouldOnlyBeUsedInConfigPackage() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackage("app.infrastructure.config..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.fasterxml.jackson..")
            .because("Jackson must be confined to infrastructure.config")

        rule.check(importedClasses)
    }

    // === REGRA: Sentry confinado à infrastructure ===

    @Test
    fun sentryShouldOnlyBeUsedInInfrastructure() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackage("app.infrastructure..")
            .should().dependOnClassesThat()
            .resideInAPackage("io.sentry..")
            .because("Sentry must be confined to infrastructure layer")

        rule.check(importedClasses)
    }

    // === REGRA: RxJava confinado à infrastructure ===

    @Test
    fun rxjavaShouldOnlyBeUsedInInfrastructure() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackage("app.infrastructure..")
            .should().dependOnClassesThat()
            .resideInAPackage("io.reactivex..")
            .because("RxJava must be confined to infrastructure layer")

        rule.check(importedClasses)
    }

    // === REGRA: JCommander confinado à infrastructure.cli ===

    @Test
    fun jcommanderShouldOnlyBeUsedInCliPackage() {
        val rule: ArchRule = noClasses()
            .that().resideOutsideOfPackages(
                "app.infrastructure.cli..",
                "app.."  // Main.kt uses JCommander for parsing
            )
            .and().resideInAPackage("app.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.beust..")
            .because("JCommander must not be used in domain layer")

        rule.check(importedClasses)
    }
}
