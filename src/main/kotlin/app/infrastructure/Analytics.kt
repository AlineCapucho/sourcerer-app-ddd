package app.infrastructure

import app.BuildConfig
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.google.protobuf.InvalidProtocolBufferException
import java.security.InvalidParameterException

typealias Param = Pair<String, String>

/**
 * Google Analytics events tracking.
 */
object Analytics {
    private val IS_ENABLED = BuildConfig.IS_GA_ENABLED
    private val BASE_PATH = BuildConfig.GA_BASE_PATH
    private val BASE_URL = "/virtual/app/"
    private val PROTOCOL_VERSION = "1"
    private val TRACKING_ID = BuildConfig.GA_TRACKING_ID
    private val DATA_SOURCE = "app"

    private val HIT_PAGEVIEW = "pageview"
    private val HIT_EXCEPTION = "exception"

    private val fuelManager = FuelManager()

    var uuid: String = ""
    var username: String = ""

    init {
        fuelManager.basePath = BASE_PATH
    }

    private fun post(params: List<Param>): Request {
        return fuelManager.request(Method.POST, "/collect", params)
    }

    fun trackEvent(event: String, params: List<Param> = listOf()) {
        if (!IS_ENABLED || (username.isEmpty() && uuid.isEmpty())) {
            return
        }

        val idParams = mutableListOf<Param>()
        if (uuid.isNotEmpty()) {
            idParams.add("cid" to uuid)
        }
        if (username.isNotEmpty()) {
            idParams.add("uid" to username)
        }

        val defaultParams = listOf("v" to PROTOCOL_VERSION,
                                   "tid" to TRACKING_ID,
                                   "ds" to DATA_SOURCE,
                                   "t" to HIT_PAGEVIEW,
                                   "dp" to BASE_URL + event)

        try {
            val (_, _, result) = post(params +
                defaultParams.filter { !params.contains(it) } +
                idParams).responseString()
            val (_, e) = result
            if (e != null) { throw e }
        } catch (e: Throwable) {
            Logger.error(e, "Error while sending GA report", logOnly = true)
        }
    }

    fun trackError(e: Throwable? = null) {
        val url = if (e != null) getErrorUrl(e) else ""
        val separator = if (url.isNotEmpty()) "/" else ""
        trackEvent("error" + separator + url, listOf("t" to HIT_EXCEPTION))
    }

    private fun getErrorUrl(e: Throwable): String {
        when (e) {
            is FuelError -> return "request"
            is InvalidParameterException -> return "request/parsing"
            is InvalidProtocolBufferException -> return "request/parsing"
        }

        val name = e.javaClass.simpleName.replace("Exception", "")
                                         .replace("Error", "")
                                         .replace("Throwable", "")

        if (name.length <= 1) {
            return name
        }

        val nameCapitalized = name.toUpperCase()
        var url = name[0].toString()
        for (i in 1..name.length - 1) {
            if (name[i] == nameCapitalized[i]) {
                url += "-"
            }
            url += name[i]
        }

        return url.toLowerCase()
    }
}
