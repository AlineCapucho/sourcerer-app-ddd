package app.domain.user.repository

import app.domain.user.aggregate.UserAggregate

/**
 * Interface de repositório para o agregado User.
 * Define como os dados do usuário são persistidos.
 * Ao armazenar um dado, ao recuperar deve estar no mesmo estado.
 * Relação um pra um com o agregado.
 */
interface UserRepository {
    /**
     * Carrega o agregado do usuário a partir do armazenamento persistente.
     */
    fun load(): UserAggregate

    /**
     * Salva o agregado do usuário no armazenamento persistente.
     */
    fun save(aggregate: UserAggregate)

    /**
     * Reseta e salva (para re-setup).
     */
    fun resetAndSave()
}
