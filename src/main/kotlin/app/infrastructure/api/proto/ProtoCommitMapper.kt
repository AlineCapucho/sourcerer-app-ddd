package app.infrastructure.api.proto

import app.Protos
import app.domain.repository.entity.Commit

/**
 * ACL Mapper: Converte entre entidades de domínio Commit e Protobuf.
 * Isola o domínio da serialização Protobuf (complexidade acidental).
 */
object ProtoCommitMapper {

    fun toCommitGroupBytes(commits: List<Commit>): ByteArray {
        return Protos.CommitGroup.newBuilder()
            .addAllCommits(commits.map { toProto(it) })
            .build()
            .toByteArray()
    }

    fun toProto(commit: Commit): Protos.Commit {
        return Protos.Commit.newBuilder()
            .setRehash(commit.rehash)
            .setRepoRehash(commit.repoRehash)
            .setTreeRehash(commit.treeRehash)
            .setAuthorName(commit.author.name)
            .setAuthorEmail(commit.author.email.value())
            .setDate(commit.dateTimestamp)
            .setIsQommit(commit.isQommit)
            .setNumLinesAdded(commit.numLinesAdded)
            .setNumLinesDeleted(commit.numLinesDeleted)
            .addAllStats(commit.stats.map { stat ->
                Protos.CommitStats.newBuilder()
                    .setNumLinesAdded(stat.numLinesAdded)
                    .setNumLinesDeleted(stat.numLinesDeleted)
                    .setType(stat.type)
                    .setTech(stat.tech)
                    .build()
            })
            .build()
    }
}
