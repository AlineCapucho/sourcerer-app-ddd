package app.infrastructure

import app.BuildConfig
import io.sentry.Sentry
import io.sentry.context.Context
import io.sentry.event.Breadcrumb
import io.sentry.event.UserBuilder
import io.sentry.event.BreadcrumbBuilder
import java.util.*

/**
 * Singleton class that logs events of different levels.
 */
object Logger {
    object Events {
        val START = "start"
        val AUTH = "auth"
        val CONFIG_SETUP = "config/setup"
        val CONFIG_CHANGED = "config/changed"
        val HASHING_REPO_SUCCESS = "hashing/repo/success"
        val HASHING_SUCCESS = "hashing/success"
        val EXIT = "exit"
    }

    @kotlin.PublishedApi
    internal val LEVEL: Int

    @kotlin.PublishedApi
    internal const val ERROR = 0
    @kotlin.PublishedApi
    internal const val WARN = 1
    @kotlin.PublishedApi
    internal const val INFO = 2
    @kotlin.PublishedApi
    internal const val DEBUG = 3
    @kotlin.PublishedApi
    internal const val TRACE = 4

    const val SILENT = BuildConfig.SILENT_USER_OUTPUT
    const val SENTRY_ENABLED = BuildConfig.SENTRY_ENABLED

    private const val PRINT_STACK_TRACE = BuildConfig.PRINT_STACK_TRACE

    private val sentryContext: Context?

    var username: String? = null
        set(value) {
            if (SENTRY_ENABLED) {
                sentryContext?.user = UserBuilder().setUsername(value).build()
            }
            Analytics.username = value ?: ""
        }

    var uuid: String? = null
        set(value) {
            Analytics.uuid = value ?: ""
        }

    init {
        if (SENTRY_ENABLED) {
            Sentry.init(BuildConfig.SENTRY_DSN)
            sentryContext = Sentry.getContext()
            addTags()
        } else {
            sentryContext = null
        }
        LEVEL = configLevelValue()
    }

    private fun configLevelValue(): Int {
        val a = mapOf("trace" to TRACE, "debug" to DEBUG, "info" to INFO,
            "warn" to WARN, "error" to ERROR)
        return a.getValue(BuildConfig.LOG_LEVEL)
    }

    private fun Double.format(digits: Int, digitsFloating: Int) =
        java.lang.String.format("%${digits}.${digitsFloating}f", this)

    private fun generateIndent(num: Int): String {
        return 0.rangeTo(num).fold("") { ind, _ -> ind + " " }
    }

    fun print(message: Any, indentLine: Boolean = false) {
        if (!SILENT) {
            print(message.toString(), indentLine)
        }
    }

    fun print(message: String, indentLine: Boolean = false) {
        if (!SILENT) {
            if (indentLine) {
                println()
            }
            println(message)
        }
    }

    fun printCommit(commitMessage: String, commitHash: String, percents: Double) {
        if (!SILENT) {
            val percentsStr = percents.format(6, 2)
            val hash = commitHash.substring(0, 7)
            val messageTrim = if (commitMessage.length > 59) {
                commitMessage.substring(0, 56).plus("...")
            } else commitMessage
            println(" [$percentsStr%] * $hash $messageTrim")
        }
    }

    private val commitDetailIndent = generateIndent(10) + "|" + generateIndent(8)
    fun printCommitDetail(message: String) {
        if (!SILENT) {
            val messageTrim = if (message.length > 59) {
                message.substring(0, 56).plus("...")
            } else message
            println(commitDetailIndent + messageTrim)
        }
    }

    fun error(e: Throwable, message: String = "", logOnly: Boolean = false) {
        val finalMessage = if (message.isNotBlank()) { message + ": " }
        else { "" } + e.message
        if (LEVEL >= ERROR) {
            println("[e] $finalMessage")
            if (PRINT_STACK_TRACE) {
                e.printStackTrace()
            }
        }
        if (!logOnly) {
            Analytics.trackError(e)
            capture(e)
        }
        addBreadcrumb(finalMessage, Breadcrumb.Level.ERROR)
    }

    inline fun warn(message: () -> String) {
        val msg = message()
        if (LEVEL >= WARN) {
            println("[w] $msg.")
        }
        addBreadcrumb(msg, Breadcrumb.Level.WARNING)
    }

    inline fun info(event: String = "", message: () -> String) {
        val msg = message()
        if (LEVEL >= INFO) {
            println("[i] $msg.")
        }
        if (event.isNotBlank()) {
            Analytics.trackEvent(event)
        }
        addBreadcrumb(msg, Breadcrumb.Level.INFO)
    }

    inline fun debug(message: () -> String) {
        if (LEVEL >= DEBUG) {
            println("[d] ${message()}.")
        }
    }

    inline fun trace(message: () -> String) {
        if (LEVEL >= TRACE) {
            println("[t] ${message()}.")
        }
    }

    val isTrace: Boolean
        inline get() = LEVEL >= TRACE

    @kotlin.PublishedApi
    internal fun addBreadcrumb(message: String, level: Breadcrumb.Level) {
        if (SENTRY_ENABLED) {
            sentryContext?.recordBreadcrumb(BreadcrumbBuilder()
                .setMessage(message)
                .setLevel(level)
                .setTimestamp(Date())
                .build())
        }
    }

    private fun addTags() {
        if (SENTRY_ENABLED) {
            val default = "unavailable"
            val osName = System.getProperty("os.name", default)
            val osVersion = System.getProperty("os.version", default)
            val javaVendor = System.getProperty("java.vendor", default)
            val javaVersion = System.getProperty("java.version", default)

            sentryContext?.addTag("environment", BuildConfig.ENV)
            sentryContext?.addTag("log-level", BuildConfig.LOG_LEVEL)
            sentryContext?.addTag("version", BuildConfig.VERSION)
            sentryContext?.addTag("version-code", BuildConfig.VERSION_CODE.toString())
            sentryContext?.addTag("os-name", osName)
            sentryContext?.addTag("os-version", osVersion)
            sentryContext?.addTag("java-vendor", javaVendor)
            sentryContext?.addTag("java-version", javaVersion)
        }
    }

    private fun capture(e: Throwable) {
        if (SENTRY_ENABLED) {
            Sentry.capture(e)
        }
    }
}
