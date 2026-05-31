package app.domain.repository.repository

import app.domain.repository.valueobject.Fact

/**
 * Interface de repositório para Fatos.
 * Define como os fatos/estatísticas são persistidos.
 */
interface FactRepository {
    /**
     * Envia fatos calculados ao servidor.
     */
    fun saveAll(facts: List<Fact>)
}
