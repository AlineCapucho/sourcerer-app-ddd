package test.tests.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Test

/**
 * Testes de arquitetura com ArchUnit.
 * Valida as regras de Bounded Contexts:
 * - Contextos delimitados com divisão explícita
 * - Baixo acoplamento entre módulos separados
 * - Shared Kernel acessível por todos os contextos
 */
class BoundedContextRulesTest {

    private val importedClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("app.domain")

    // === REGRA: Contexto User não depende diretamente do contexto Repository (exceto shared) ===

    @Test
    fun userContextShouldNotDependOnRepositoryEntities() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain.user..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "app.domain.repository.entity..",
                "app.domain.repository.aggregate..",
                "app.domain.repository.service..",
                "app.domain.repository.event..",
                "app.domain.repository.factory..",
                "app.domain.repository.port.."
            )
            .because("User bounded context should not depend on Repository " +
                "context internals (only shared kernel allowed)")

        rule.check(importedClasses)
    }

    // === REGRA: Shared Kernel é acessível por todos os contextos ===

    @Test
    fun sharedKernelShouldBeAccessibleByAllContexts() {
        // This is a positive test - shared kernel classes should be usable
        // We verify that shared kernel doesn't depend on specific contexts
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("app.domain.shared..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "app.domain.repository..",
                "app.domain.user.."
            )
            .because("Shared Kernel must not depend on specific bounded contexts")

        rule.check(importedClasses)
    }

    // === REGRA: Agregados só são acessados via raiz ===

    @Test
    fun aggregatesShouldOnlyResideInAggregatePackage() {
        val rule: ArchRule = classes()
            .that().haveSimpleNameEndingWith("Aggregate")
            .should().resideInAPackage("..aggregate..")
            .because("Aggregates should be in the aggregate package")

        rule.check(importedClasses)
    }

    // === REGRA: Interfaces de repositório devem ser interfaces ===

    @Test
    fun repositoryInterfacesShouldBeInterfaces() {
        val rule: ArchRule = classes()
            .that().resideInAPackage("app.domain.repository.repository..")
            .should().beInterfaces()
            .because("Repository definitions in domain must be interfaces, " +
                "implementations belong in infrastructure")

        rule.check(importedClasses)
    }

    // === REGRA: Domain Services devem ser interfaces ===

    @Test
    fun domainServicesShouldBeInterfaces() {
        val rule: ArchRule = classes()
            .that().resideInAPackage("app.domain.repository.service..")
            .should().beInterfaces()
            .because("Domain service definitions must be interfaces, " +
                "implementations belong in infrastructure")

        rule.check(importedClasses)
    }

    // === REGRA: Eventos de domínio devem implementar DomainEvent ===

    @Test
    fun domainEventsShouldImplementDomainEventInterface() {
        val rule: ArchRule = classes()
            .that().resideInAPackage("app.domain.repository.event..")
            .should().implement(app.domain.shared.event.DomainEvent::class.java)
            .because("All domain events must implement the DomainEvent interface")

        rule.check(importedClasses)
    }
}
