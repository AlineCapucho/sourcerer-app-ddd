package app.domain.user.aggregate

import app.domain.user.entity.User
import app.domain.user.valueobject.Credentials
import app.domain.user.valueobject.LocalRepo
import app.domain.user.valueobject.UserEmail

/**
 * Agregado de Usuário.
 * Raiz do agregado: User.
 * Conjunto de objetos associados com propósito de mudança de dados.
 */
class UserAggregate(
    val user: User
) {
    private val _localRepos: MutableSet<LocalRepo> = mutableSetOf()
    private var _credentials: Credentials? = null

    val localRepos: Set<LocalRepo> get() = _localRepos.toSet()
    val credentials: Credentials? get() = _credentials

    /**
     * Define as credenciais do usuário.
     */
    fun defineCredentials(credentials: Credentials) {
        _credentials = credentials
    }

    /**
     * Verifica se as credenciais são válidas.
     */
    fun hasValidCredentials(): Boolean = _credentials?.isValid() ?: false

    /**
     * Adiciona um repositório local ao rastreamento.
     */
    fun addLocalRepo(localRepo: LocalRepo) {
        _localRepos.remove(localRepo) // Remove para atualizar campos
        _localRepos.add(localRepo)
    }

    /**
     * Remove um repositório local do rastreamento.
     */
    fun removeLocalRepo(path: String) {
        _localRepos.removeAll { it.path == path }
    }

    /**
     * Retorna a lista de repositórios locais.
     */
    fun getLocalRepos(): List<LocalRepo> = _localRepos.toList()

    /**
     * Verifica se é a primeira execução (sem credenciais configuradas).
     */
    fun isFirstLaunch(): Boolean = _credentials == null
}
