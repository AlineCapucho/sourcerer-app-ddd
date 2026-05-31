package app.infrastructure.ui

import app.application.usecase.GitRepositoryValidator
import app.application.usecase.HashRepositoryUseCase
import app.domain.repository.port.ServerApiPort
import app.domain.user.repository.UserRepository

/**
 * Console user interface.
 * Usa state machine pattern para fluxo de interação.
 */
class ConsoleUi(
    val apiPort: ServerApiPort,
    val userRepository: UserRepository,
    val gitValidator: GitRepositoryValidator,
    val hashRepositoryUseCaseFactory: () -> HashRepositoryUseCase
) : Context {
    var state: ConsoleState = OpenState(this)

    init {
        changeState(state)
    }

    override fun changeState(state: ConsoleState) {
        this.state = state
        state.doAction()
        state.next()
    }
}
