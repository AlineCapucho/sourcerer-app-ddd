package app.domain.repository.repository

import app.domain.repository.entity.Commit

/**
 * Interface de repositório para a entidade Commit.
 * Define como os commits são persistidos.
 * Ao armazenar um dado, ao recuperar deve estar no mesmo estado.
 */
interface CommitRepository {
    /**
     * Envia commits processados ao servidor.
     */
    fun saveAll(commits: List<Commit>)

    /**
     * Remove commits do servidor (commits que não existem mais localmente).
     */
    fun deleteAll(commits: List<Commit>)
}
