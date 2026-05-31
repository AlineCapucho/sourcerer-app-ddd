package app.infrastructure.api

import app.BuildConfig
import app.Protos
import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.entity.Repo
import app.domain.repository.port.*
import app.domain.repository.valueobject.AuthorDistance
import app.domain.repository.valueobject.Fact
import app.domain.repository.valueobject.ProcessEntry
import app.domain.user.valueobject.UserEmail
import app.infrastructure.Logger
import app.infrastructure.api.proto.ProtoCommitMapper
import app.infrastructure.api.proto.ProtoFactMapper
import app.infrastructure.api.proto.ProtoRepoMapper
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.google.protobuf.InvalidProtocolBufferException
import java.security.InvalidParameterException

/**
 * ACL: Adaptador que implementa ServerApiPort.
 * Isola o domínio de detalhes de protocolo HTTP e serialização Protobuf.
 */
class ServerApiAdapter : ServerApiPort {
    companion object {
        private val HEADER_VERSION_CODE = "app-version-code"
        private val HEADER_CONTENT_TYPE = "Content-Type"
        private val HEADER_CONTENT_TYPE_PROTO = "application/octet-stream"
        private val HEADER_COOKIE = "Cookie"
        private val HEADER_SET_COOKIE = "Set-Cookie"
        private val KEY_TOKEN = "Token="
    }

    val fuelManager = FuelManager()
    private var token = ""
    private var username = ""
    private var password = ""

    private fun cookieRequestInterceptor() = { req: Request ->
        if (token.isNotEmpty()) {
            req.header(Pair(HEADER_COOKIE, KEY_TOKEN + token))
        }
        req
    }

    private fun cookieResponseInterceptor() = { _: Request, res: Response ->
        val newToken = res.headers[HEADER_SET_COOKIE]
            ?.find { it.startsWith(KEY_TOKEN) }
        if (newToken != null && newToken.isNotBlank()) {
            token = newToken.substringAfter(KEY_TOKEN).substringBefore(';')
        }
        res
    }

    init {
        fuelManager.basePath = BuildConfig.API_BASE_PATH
        fuelManager.addRequestInterceptor { cookieRequestInterceptor() }
        fuelManager.addResponseInterceptor { cookieResponseInterceptor() }
    }

    private fun post(path: String): Request = fuelManager.request(Method.POST, path)
    private fun get(path: String): Request = fuelManager.request(Method.GET, path)
    private fun delete(path: String): Request = fuelManager.request(Method.DELETE, path)

    private fun getVersionCodeHeader() = Pair(HEADER_VERSION_CODE, BuildConfig.VERSION_CODE.toString())
    private fun getContentTypeHeader() = Pair(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_PROTO)

    override fun authorize(username: String, password: String): ApiResult<Unit> {
        this.username = username
        this.password = password
        val request = post("/auth").authenticate(username, password)
            .header(getVersionCodeHeader())
        return makeRequest(request, "getToken") {}
    }

    override fun getUser(): ApiResult<UserData> {
        val request = get("/user")
        return makeRequest(request, "getUser") { body ->
            val proto = Protos.User.parseFrom(body)
            UserData(
                emails = proto.emailsList.map { e ->
                    UserEmail(address = e.email, primary = e.primary, verified = e.verified)
                },
                repos = proto.reposList.map { r ->
                    RepoData(rehash = r.rehash, initialCommitRehash = r.initialCommitRehash)
                }
            )
        }
    }

    override fun postUser(emails: List<UserEmail>, repos: List<Repo>): ApiResult<Unit> {
        val proto = Protos.User.newBuilder()
            .addAllEmails(emails.map { e ->
                Protos.UserEmail.newBuilder()
                    .setEmail(e.address)
                    .setPrimary(e.primary)
                    .setVerified(e.verified)
                    .build()
            })
            .build()
        val request = post("/user").header(getContentTypeHeader()).body(proto.toByteArray())
        return makeRequest(request, "postUser") {}
    }

    override fun postRepo(repo: Repo): ApiResult<RepoData> {
        val protoBytes = ProtoRepoMapper.toProtoBytes(repo)
        val request = post("/repo").header(getContentTypeHeader()).body(protoBytes)
        return makeRequest(request, "getRepo") { body ->
            ProtoRepoMapper.fromProtoBytes(body)
        }
    }

    override fun postCommits(commits: List<Commit>): ApiResult<Unit> {
        val protoBytes = ProtoCommitMapper.toCommitGroupBytes(commits)
        val request = post("/commits").header(getContentTypeHeader()).body(protoBytes)
        return makeRequest(request, "postCommits") {}
    }

    override fun deleteCommits(commits: List<Commit>): ApiResult<Unit> {
        val protoBytes = ProtoCommitMapper.toCommitGroupBytes(commits)
        val request = delete("/commits").header(getContentTypeHeader()).body(protoBytes)
        return makeRequest(request, "deleteCommits") {}
    }

    override fun postFacts(facts: List<Fact>): ApiResult<Unit> {
        val protoBytes = ProtoFactMapper.toFactGroupBytes(facts)
        val request = post("/facts").header(getContentTypeHeader()).body(protoBytes)
        return makeRequest(request, "postFacts") {}
    }

    override fun postAuthors(authors: List<Author>): ApiResult<Unit> {
        val proto = Protos.AuthorGroup.newBuilder()
            .addAllAuthors(authors.map { a ->
                Protos.Author.newBuilder()
                    .setEmail(a.email.value())
                    .setName(a.name)
                    .setRepoRehash(a.repoRehash)
                    .build()
            })
            .build()
        val request = post("/authors").header(getContentTypeHeader()).body(proto.toByteArray())
        return makeRequest(request, "postAuthors") {}
    }

    override fun postProcessCreate(requestNumEntries: Int): ApiResult<ProcessData> {
        val proto = Protos.Process.newBuilder()
            .setRequestNumEntries(requestNumEntries)
            .build()
        val request = post("/process/create").header(getContentTypeHeader()).body(proto.toByteArray())
        return makeRequest(request, "postProcessCreate") { body ->
            val p = Protos.Process.parseFrom(body)
            ProcessData(
                id = p.id,
                entries = p.entriesList.map { e ->
                    ProcessEntry(id = e.id, status = e.status, errorCode = e.errorCode)
                }
            )
        }
    }

    override fun postProcess(entries: List<ProcessEntry>): ApiResult<Unit> {
        val proto = Protos.Process.newBuilder()
            .addAllEntries(entries.map { e ->
                Protos.ProcessEntry.newBuilder()
                    .setId(e.id)
                    .setStatus(e.status)
                    .setErrorCode(e.errorCode)
                    .build()
            })
            .build()
        val request = post("/process").header(getContentTypeHeader()).body(proto.toByteArray())
        return makeRequest(request, "postProcess") {}
    }

    override fun postAuthorDistances(distances: List<AuthorDistance>): ApiResult<Unit> {
        val proto = Protos.AuthorDistanceGroup.newBuilder()
            .addAllAuthorDistances(distances.map { d ->
                Protos.AuthorDistance.newBuilder()
                    .setRepoRehash(d.repoRehash)
                    .setEmail(d.email.value())
                    .setScore(d.score)
                    .build()
            })
            .build()
        val request = post("/distances").header(getContentTypeHeader()).body(proto.toByteArray())
        return makeRequest(request, "postDistances") {}
    }

    private fun <T> makeRequest(
        request: Request,
        requestName: String,
        parser: (ByteArray) -> T
    ): ApiResult<T> {
        try {
            Logger.debug { "Request $requestName initialized" }
            val (_, res, result) = request.responseString()
            val (_, e) = result
            if (e == null) {
                Logger.debug { "Request $requestName success" }
                val data = parser(res.data)
                return ApiResult.Success(data)
            } else {
                return ApiResult.Error(res.statusCode, e.message ?: "Unknown error")
            }
        } catch (e: InvalidProtocolBufferException) {
            return ApiResult.Error(message = e.message ?: "Proto parse error")
        } catch (e: InvalidParameterException) {
            return ApiResult.Error(message = e.message ?: "Invalid parameter")
        }
    }
}
