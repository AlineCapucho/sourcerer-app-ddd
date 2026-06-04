package app.application.usecase

import app.domain.repository.aggregate.RepoAggregate
import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.entity.Repo
import app.domain.repository.event.AuthorsDiscoveredEvent
import app.domain.repository.event.FactsCalculatedEvent
import app.domain.repository.event.RepoHashingCompletedEvent
import app.domain.repository.factory.CommitFactory
import app.domain.repository.factory.RepoFactory
import app.domain.repository.port.GitRepositoryPort
import app.domain.repository.port.ServerApiPort
import app.domain.repository.repository.AuthorDistanceRepository
import app.domain.repository.repository.AuthorRepository
import app.domain.repository.repository.CommitRepository
import app.domain.repository.repository.FactRepository
import app.domain.repository.repository.RepoRepository
import app.domain.repository.valueobject.ProcessEntry
import app.domain.shared.event.EventDispatcher
import app.domain.user.valueobject.LocalRepo

/**
 * Use Case: Processar (hash) um repositório local.
 * Coordena as interações entre a camada de domínio e a camada de infraestrutura.
 * Orquestra todo o pipeline de hashing de um repositório.
 */
class HashRepositoryUseCase(
    private val gitPort: GitRepositoryPort,
    private val apiPort: ServerApiPort,
    private val repoRepository: RepoRepository,
    private val commitRepository: CommitRepository,
    private val authorRepository: AuthorRepository,
    private val factRepository: FactRepository,
    private val authorDistanceRepository: AuthorDistanceRepository,
    private val serviceFactory: ServiceFactory,
    private val eventDispatcher: EventDispatcher,
    private val commitHasherEnabled: Boolean = true,
    private val factHasherEnabled: Boolean = true,
    private val longevityEnabled: Boolean = false,
    private val metaHasherEnabled: Boolean = true,
    private val distancesEnabled: Boolean = true
) {
    fun execute(localRepo: LocalRepo, userEmails: List<String>) {
        val processEntryId = localRepo.processEntryId

        gitPort.open(localRepo.path)
        try {
            updateProcess(processEntryId, PROCESS_STATUS_START)

            // 1. Buscar rehashes e autores do repositório
            val (rehashes, authors, commitsCount) =
                gitPort.fetchRehashesAndAuthors()

            // 2. Calcular rehash do repo e criar entidade
            val remoteOrigin = gitPort.getRemoteOrigin()
            val repoRehash = calculateRepoRehash(
                rehashes.last(), localRepo, remoteOrigin
            )
            val serverRepo = RepoFactory.create(
                rehash = repoRehash,
                initialCommitRehash = rehashes.last(),
                meta = localRepo.meta,
                processEntryId = processEntryId ?: 0
            )

            // 3. Enviar repo ao servidor e receber histórico
            val repoData = apiPort.postRepo(serverRepo).getOrThrow()
            val repoWithHistory = reconstructRepoFromServer(repoData, serverRepo)
            val aggregate = RepoAggregate(repoWithHistory)

            // 4. Registrar e enviar autores
            aggregate.registerAuthors(authors)
            postAuthorsToServer(authors, repoWithHistory.rehash)
            aggregate.addEvent(AuthorsDiscoveredEvent(repoWithHistory.rehash, authors))

            // 5. Filtrar emails para hashing
            val allEmails = authors.map { it.email.value() }.toHashSet()
            val filteredEmails = if (localRepo.hashAllContributors) {
                allEmails
            } else {
                aggregate.filterKnownEmails(allEmails, userEmails.toHashSet())
            }

            // 6. Criar services com dados dinâmicos (emails e totalCommits)
            val commitExtractionService = serviceFactory.createCommitExtractionService()
            val factCalculationService = serviceFactory.createFactCalculationService(
                emails = filteredEmails,
                totalCommits = rehashes.size
            )
            val metaHashingService = serviceFactory.createMetaHashingService()
            val authorDistanceService = serviceFactory.createAuthorDistanceService(
                allEmails = allEmails,
                userEmails = userEmails.toHashSet()
            )

            // 7. Deletar commits que não existem mais localmente
            val deletedCommits = aggregate.findDeletedCommits(rehashes)
            if (deletedCommits.isNotEmpty()) {
                commitRepository.deleteAll(deletedCommits)
            }

            // 8. Processar commits via stream
            val knownCommitRehashes = repoWithHistory.commits
                .map { it.rehash }.toHashSet()

            val errors = mutableListOf<Throwable>()
            val onError: (Throwable) -> Unit = { e -> errors.add(e) }

            val commitStream = gitPort.crawlCommits(
                repoRehash = repoWithHistory.rehash,
                totalCommitCount = rehashes.size,
                filteredEmails = filteredEmails
            )

            val processedCommits = mutableListOf<Commit>()

            commitStream.subscribe(
                onNext = { commit ->
                    try {
                        // Filtrar commits já conhecidos
                        if (knownCommitRehashes.contains(commit.rehash)) return@subscribe
                        if (!filteredEmails.contains(commit.author.email.value())) return@subscribe

                        // Extrair stats
                        if (commitHasherEnabled) {
                            val stats = commitExtractionService.extractStats(commit.diffs)
                            commit.registerStats(stats)
                            processedCommits.add(commit)

                            // Adicionar coautores
                            commit.coauthors.forEach { coauthor ->
                                processedCommits.add(commit.copyForCoauthor(coauthor))
                            }

                            // Send in batches of 1000 to avoid accumulating
                            // all commits in memory (matches original buffering).
                            if (processedCommits.size >= 1000) {
                                commitRepository.saveAll(processedCommits.toList())
                                processedCommits.clear()
                            }
                        }

                        // Acumular fatos
                        if (factHasherEnabled) {
                            factCalculationService.processCommit(commit)
                        }
                    } catch (e: Throwable) {
                        onError(e)
                    }
                },
                onError = onError,
                onComplete = {
                    try {
                        // Enviar commits restantes
                        if (commitHasherEnabled && processedCommits.isNotEmpty()) {
                            commitRepository.saveAll(processedCommits.toList())
                            processedCommits.clear()
                        }

                        // Calcular e enviar fatos
                        if (factHasherEnabled) {
                            val facts = factCalculationService
                                .calculateFacts(repoWithHistory.rehash)
                            if (facts.isNotEmpty()) {
                                factRepository.saveAll(facts)
                            }
                            aggregate.addEvent(
                                FactsCalculatedEvent(repoWithHistory.rehash, facts.size)
                            )
                        }
                    } catch (e: Throwable) {
                        errors.add(e)
                    }
                }
            )

            // 9. Meta hasher
            if (metaHasherEnabled) {
                try {
                    val metaFacts = metaHashingService.calculateMetaFacts(
                        repoRehash = repoWithHistory.rehash,
                        authors = authors,
                        commitsCount = commitsCount,
                        userEmails = userEmails
                    )
                    if (metaFacts.isNotEmpty()) {
                        factRepository.saveAll(metaFacts)
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }

            // 10. Distâncias entre autores
            if (distancesEnabled) {
                try {
                    val pathStream = gitPort.crawlPaths()
                    pathStream.subscribe(
                        onNext = { pathData ->
                            authorDistanceService.registerContribution(
                                app.domain.shared.valueobject.Email(pathData.email),
                                pathData.paths,
                                pathData.timestamp
                            )
                        },
                        onError = onError,
                        onComplete = {
                            try {
                                val distances = authorDistanceService
                                    .calculateDistances(repoWithHistory.rehash)
                                if (distances.isNotEmpty()) {
                                    authorDistanceRepository.saveAll(distances)
                                }
                            } catch (e: Throwable) {
                                errors.add(e)
                            }
                        }
                    )
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }

            // 10.5. Code Longevity
            if (longevityEnabled) {
                try {
                    val codeLongevityService = serviceFactory.createCodeLongevityService()
                    val longevityFacts = codeLongevityService.calculateLongevityFacts(
                        repoRehash = repoWithHistory.rehash,
                        repoPath = localRepo.path,
                        emails = filteredEmails.toHashSet()
                    )
                    if (longevityFacts.isNotEmpty()) {
                        factRepository.saveAll(longevityFacts)
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }

            // 11. Verificar erros
            if (errors.isNotEmpty()) {
                throw HashingException(errors)
            }

            // 12. Sucesso
            updateProcess(processEntryId, PROCESS_STATUS_COMPLETE)
            aggregate.addEvent(
                RepoHashingCompletedEvent(repoWithHistory.rehash, true)
            )

            // Despachar eventos
            aggregate.events.forEach { eventDispatcher.dispatch(it) }
            aggregate.clearEvents()

        } catch (e: EmptyRepoException) {
            updateProcess(processEntryId, PROCESS_STATUS_FAIL, PROCESS_ERROR_EMPTY_REPO)
            throw e
        } catch (e: Throwable) {
            updateProcess(processEntryId, PROCESS_STATUS_FAIL)
            throw e
        } finally {
            gitPort.close()
        }
    }

    private fun reconstructRepoFromServer(
        repoData: app.domain.repository.port.RepoData,
        serverRepo: Repo
    ): Repo {
        val repo = Repo(
            rehash = serverRepo.rehash,
            initialCommitRehash = serverRepo.initialCommitRehash
        )
        repo.defineMeta(serverRepo.meta)
        repo.defineProcessEntryId(serverRepo.processEntryId)
        repo.defineEmailFilter(repoData.emails)

        val commits = repoData.commits.map { commitData ->
            CommitFactory.reconstruct(
                rehash = commitData.rehash,
                authorName = commitData.authorName,
                authorEmail = commitData.authorEmail,
                dateTimestamp = commitData.dateTimestamp,
                repoRehash = commitData.repoRehash,
                treeRehash = commitData.treeRehash
            )
        }
        repo.loadServerHistory(commits)
        return repo
    }

    private fun postAuthorsToServer(authors: Set<Author>, repoRehash: String) {
        val authorsList = authors.toList()
        authorsList.forEach { it.associateToRepo(repoRehash) }
        authorsList.chunked(1000).forEach { batch ->
            authorRepository.saveAll(batch)
        }
    }

    private fun calculateRepoRehash(
        initialCommitRehash: String,
        localRepo: LocalRepo,
        remoteOrigin: String
    ): String {
        val username = try { System.getProperty("user.name") } catch (e: Exception) { "" }
        var repoRehash = initialCommitRehash
        repoRehash += if (remoteOrigin.isNotBlank()) {
            remoteOrigin
        } else {
            localRepo.path + username
        }
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(repoRehash)
    }

    private fun updateProcess(processEntryId: Int?, status: Int, errorCode: Int = 0) {
        if (processEntryId == null) return
        val entry = ProcessEntry(id = processEntryId, status = status, errorCode = errorCode)
        repoRepository.updateProcessStatus(entry)
    }

    companion object {
        const val PROCESS_STATUS_START = 100
        const val PROCESS_STATUS_COMPLETE = 200
        const val PROCESS_STATUS_FAIL = 1000
        const val PROCESS_ERROR_EMPTY_REPO = 8
    }
}

class HashingException(val errors: List<Throwable>) :
    RuntimeException("Hashing failed with ${errors.size} errors")

class EmptyRepoException(message: String) : RuntimeException(message)
