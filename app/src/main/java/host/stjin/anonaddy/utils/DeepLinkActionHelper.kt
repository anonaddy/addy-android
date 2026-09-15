package host.stjin.anonaddy.utils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object DeepLinkActionHelper {

    data class BlockAction(
        val type: String, // "email" or "domain"
        val value: String
    )

    fun parseAction(uriString: String): Pair<String?, BlockAction?> {
        val queryPart = uriString.substringAfter("?", "")
        val queryParams = parseQueryParams(queryPart)

        val action = queryParams["action"]
        val email = queryParams["email"]
        val domainParam = queryParams["domain"]

        val aliasId = when {
            uriString.contains("/actions") -> {
                uriString.substringAfter("aliases/").substringBefore("/actions").substringBefore("?")
            }
            uriString.contains("/deactivate") -> {
                uriString.substringAfter("deactivate/").substringBefore("?")
            }
            uriString.contains("/aliases/") -> {
                uriString.substringAfter("aliases/").substringBefore("/").substringBefore("?")
            }
            else -> null
        }?.takeIf { it.isNotEmpty() }

        val blockAction = when (action) {
            "block_email" -> {
                if (!email.isNullOrEmpty()) {
                    BlockAction(type = "email", value = email)
                } else null
            }
            "block_domain" -> {
                val domain = domainParam ?: email?.substringAfterLast("@")?.trim()?.takeIf { it.isNotEmpty() }
                if (!domain.isNullOrEmpty()) {
                    BlockAction(type = "domain", value = domain)
                } else null
            }
            else -> null
        }

        return Pair(aliasId, blockAction)
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) return emptyMap()
        val params = mutableMapOf<String, String>()
        for (pair in queryString.split("&")) {
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
                val value = if (parts.size > 1) {
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                } else {
                    ""
                }
                params[key] = value
            }
        }
        return params
    }
}
