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
     *
     * @param repoRehash Hash do repositório para identificação dos fatos.
     * @param repoPath Caminho do repositório Git no disco.
     * @param emails Conjunto de emails dos autores a serem considerados.
     */
    fun calculateLongevityFacts(
        repoRehash: String,
        repoPath: String,
        emails: HashSet<String>
    ): List<Fact>
}
