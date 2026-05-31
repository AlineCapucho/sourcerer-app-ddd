package app.infrastructure.api.proto

import app.Protos
import app.domain.repository.valueobject.Fact

/**
 * ACL Mapper: Converte entre Value Objects de domínio Fact e Protobuf.
 * Isola o domínio da serialização Protobuf (complexidade acidental).
 */
object ProtoFactMapper {

    fun toFactGroupBytes(facts: List<Fact>): ByteArray {
        return Protos.FactGroup.newBuilder()
            .addAllFacts(facts.map { toProto(it) })
            .build()
            .toByteArray()
    }

    fun toProto(fact: Fact): Protos.Fact {
        return Protos.Fact.newBuilder()
            .setRepoRehash(fact.repoRehash)
            .setEmail(fact.authorEmail.value())
            .setCode(fact.code)
            .setKey(fact.key)
            .setValue1(fact.value)
            .setValue2(fact.value2)
            .setValue3(fact.value3)
            .build()
    }
}
