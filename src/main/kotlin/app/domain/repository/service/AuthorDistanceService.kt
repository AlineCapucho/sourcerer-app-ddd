package app.domain.repository.service

import app.domain.repository.valueobject.AuthorDistance
import app.domain.shared.valueobject.Email

/**
 * Domain Service: Cálculo de distância/colaboração entre autores.
 * Operação sem estado (stateless) que realiza uma tarefa do domínio.
 * Operações que consideram mais de uma entidade.
 *
 * Responsabilidade: Calcular métricas de proximidade entre autores
 * baseado nos caminhos de arquivos que modificaram.
 */
interface AuthorDistanceService {
    /**
     * Registra uma contribuição de um autor em determinados caminhos.
     */
    fun registerContribution(email: Email, paths: List<String>, timestamp: Long)

    /**
     * Calcula as distâncias entre autores para o repositório.
     */
    fun calculateDistances(repoRehash: String): List<AuthorDistance>
}
