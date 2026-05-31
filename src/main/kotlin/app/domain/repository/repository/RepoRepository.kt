package app.domain.repository.repository

import app.domain.repository.entity.Repo
import app.domain.repository.valueobject.ProcessEntry

/**
 * Interface de repositório para a entidade Repo.
 * Define como os repositórios são persistidos.
 * Ao armazenar um dado, ao recuperar deve estar no mesmo estado.
 * Relação um pra um com o agregado.
 */
interface RepoRepository {
    /**
     * Envia o repositório ao servidor e retorna a versão com histórico de commits.
     */
    fun postAndRetrieve(repo: Repo): Repo

    /**
     * Atualiza o status de processamento de um repositório.
     */
    fun updateProcessStatus(processEntry: ProcessEntry)
}
