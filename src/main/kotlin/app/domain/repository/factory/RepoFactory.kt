package app.domain.repository.factory

import app.domain.repository.entity.Repo
import app.domain.repository.valueobject.RepoMeta

/**
 * Factory para criação de instâncias complexas de Repo.
 * Responsabilidade de criar instâncias complexas e agregados em um objeto separado.
 * Interface que encapsula toda a criação.
 */
object RepoFactory {

    /**
     * Cria um Repo a partir de dados locais e do cálculo de rehash.
     */
    fun create(
        rehash: String,
        initialCommitRehash: String,
        meta: RepoMeta = RepoMeta(),
        processEntryId: Int = 0
    ): Repo {
        val repo = Repo(
            rehash = rehash,
            initialCommitRehash = initialCommitRehash
        )
        repo.defineMeta(meta)
        repo.defineProcessEntryId(processEntryId)
        return repo
    }
}
