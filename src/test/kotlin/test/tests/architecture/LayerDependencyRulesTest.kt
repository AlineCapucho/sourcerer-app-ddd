package test.tests.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.Test

/**
 * Testes de arquitetura com ArchUnit.
 * Valida as regras de dependência entre camadas DDD:
 * - Domain não depende de Application nem Infrastructure
 * - Application não depende de Infrastructure
 * - Infrastructure pode depender de Domain e Application
 */
class LayerDependencyRulesTest {

    private val importedClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("app")

    // === REGRA 1: Domain não depende de Infrastructure ===

    @Test
    fun domainShouldNotDependOnInfrastructure() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("app.infrastructure..")
            .because("Domain layer must not depend on Infrastructure layer (DDD rule)")

        rule.check(importedClasses)
    }

    // === REGRA 2: Domain não depende de Application ===

    @Test
    fun domainShouldNotDependOnApplication() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("app.application..")
            .because("Domain layer must not depend on Application layer (DDD rule)")

        rule.check(importedClasses)
    }

    // === REGRA 3: Application não depende de Infrastructure ===

    @Test
    fun applicationShouldNotDependOnInfrastructure() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("app.infrastructure..")
            .because("Application layer must not depend on Infrastructure layer (DDD rule)")

        rule.check(importedClasses)
    }

    // === REGRA 4: Arquitetura em camadas completa ===

    @Test
    fun layeredArchitectureShouldBeRespected() {
        val rule = layeredArchitecture()
            .layer("Domain").definedBy("app.domain..")
            .layer("Application").definedBy("app.application..")
            .layer("Infrastructure").definedBy("app.infrastructure..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                "Application", "Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers(
                "Infrastructure")
            .because("Layered architecture: Domain is the core, " +
                "Application orchestrates, Infrastructure implements")

        rule.check(importedClasses)
    }
}
