package app.infrastructure.api.proto

import app.Protos
import app.domain.repository.entity.Repo
import app.domain.repository.port.CommitData
import app.domain.repository.port.RepoData

/**
 * ACL Mapper: Converte entre entidades de domínio e Protobuf.
 * Isola o domínio da serialização Protobuf (complexidade acidental).
 */
object ProtoRepoMapper {

    fun toProtoBytes(repo: Repo): ByteArray {
        return Protos.Repo.newBuilder()
            .setRehash(repo.rehash)
            .setInitialCommitRehash(repo.initialCommitRehash)
            .addAllEmails(repo.emails)
            .setMeta(Protos.RepoMeta.newBuilder()
                .setHosterId(repo.meta.hosterId)
                .setService(repo.meta.service)
                .setName(repo.meta.name)
                .setOwnerName(repo.meta.ownerName)
                .setDescription(repo.meta.description)
                .setHtmlUrl(repo.meta.htmlUrl)
                .setCloneUrl(repo.meta.cloneUrl)
                .build())
            .setProcessEntryId(repo.processEntryId)
            .build()
            .toByteArray()
    }

    fun fromProtoBytes(bytes: ByteArray): RepoData {
        val proto = Protos.Repo.parseFrom(bytes)
        return RepoData(
            rehash = proto.rehash,
            initialCommitRehash = proto.initialCommitRehash,
            emails = proto.emailsList,
            commits = proto.commitsList.map { c ->
                CommitData(
                    rehash = c.rehash,
                    repoRehash = c.repoRehash,
                    authorName = c.authorName,
                    authorEmail = c.authorEmail,
                    dateTimestamp = c.date,
                    treeRehash = c.treeRehash
                )
            },
            processEntryId = proto.processEntryId
        )
    }
}
