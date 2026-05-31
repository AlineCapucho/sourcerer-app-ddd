package app.domain.repository.service

import app.domain.repository.entity.Commit
import app.domain.repository.valueobject.Fact

/**
 * Domain Service: Cálculo de fatos sobre desenvolvedores.
 * Operação sem estado (stateless) que realiza uma tarefa do domínio.
 *
 * Responsabilidade: Calcular fatos/estatísticas sobre padrões de
 * desenvolvimento (horários, naming conventions, indentação, etc.)
 * a partir de uma sequência de commits.
 */
interface FactCalculationService {
    /**
     * Processa um commit para acumular dados de fatos.
     */
    fun processCommit(commit: Commit)

    /**
     * Gera a lista final de fatos calculados para o repositório.
     */
    fun calculateFacts(repoRehash: String): List<Fact>
}
