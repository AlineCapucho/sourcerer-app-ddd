package app.domain.repository.service

import app.domain.repository.valueobject.Fact

/**
 * Domain Service: Análise de longevidade de código.
 * Operação sem estado (stateless) que realiza uma tarefa do domínio.
 *
 * Responsabilidade: Calcular métricas de sobrevivência de código
 * (quanto tempo as linhas de um autor permanecem no codebase).
 */
interface CodeLongevityService {
    /**
     * Calcula fatos de longevidade para o repositório.
     */
    fun calculateLongevityFacts(repoRehash: String): List<Fact>
}
