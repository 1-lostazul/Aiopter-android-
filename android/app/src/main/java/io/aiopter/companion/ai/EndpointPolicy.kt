package io.aiopter.companion.ai

import java.net.URI

object EndpointPolicy {
    private const val PRODUCTION_HOST = "api.aiopter.ai"
    private val debugHttpHosts = setOf("10.0.2.2", "127.0.0.1", "localhost")

    fun requireAllowed(baseUrl: String, isDebug: Boolean): String {
        val uri = runCatching { URI(baseUrl.trim()) }
            .getOrElse { throw IllegalStateException("The API endpoint is invalid.") }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "The API endpoint must not include credentials, a query, or a fragment."
        }
        require(uri.path.isNullOrEmpty() || uri.path == "/") {
            "The API endpoint must be an origin without a path."
        }
        val host = uri.host?.lowercase() ?: throw IllegalStateException("The API endpoint host is missing.")
        if (!isDebug) {
            require(uri.scheme == "https" && host == PRODUCTION_HOST && effectivePort(uri) == 443) {
                "Release builds require https://$PRODUCTION_HOST."
            }
        } else {
            val isLocalHttp = uri.scheme == "http" && host in debugHttpHosts
            require(uri.scheme == "https" || isLocalHttp) {
                "Debug cleartext is limited to emulator or loopback hosts."
            }
        }
        return uri.toString().trimEnd('/')
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port != -1 -> uri.port
        uri.scheme == "https" -> 443
        else -> 80
    }
}
