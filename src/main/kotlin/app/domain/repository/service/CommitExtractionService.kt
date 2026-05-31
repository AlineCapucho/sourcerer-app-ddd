package app.domain.repository.service

import app.domain.repository.valueobject.CommitStats
import app.domain.repository.valueobject.DiffFile

/**
 * Domain Service: Extração de estatísticas de commits.
 * Operação sem estado (stateless) que realiza uma tarefa do domínio.
 * Qualquer transformação ou processo que não é uma responsabilidade
 * natural de uma entidade ou objeto de valor.
 *
 * Responsabilidade: Extrair estatísticas de linguagem e biblioteca
 * a partir dos diffs de um commit.
 */
interface CommitExtractionService {
    /**
     * Extrai estatísticas de tecnologia (linguagens e bibliotecas)
     * a partir de uma lista de arquivos modificados.
     */
    fun extractStats(files: List<DiffFile>): List<CommitStats>

    /**
     * Tokeniza uma linha de código para análise.
     */
    fun tokenize(line: String): List<String>
}
