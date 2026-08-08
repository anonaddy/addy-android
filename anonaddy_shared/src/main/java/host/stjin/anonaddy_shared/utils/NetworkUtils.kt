package host.stjin.anonaddy_shared.utils

import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

object NetworkUtils {
    fun isLocalAddress(url: String): Boolean {
        val uri = try {
            url.toUri()
        } catch (e: Exception) {
            return false
        }
        var host = uri.host ?: return false

        // Strip brackets for IPv6
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length - 1)
        }

        host = host.lowercase()

        // Check for common local hostnames
        if (host == "localhost" || host.endsWith(".local")) {
            return true
        }

        // Simple hostnames (no dots) are usually local (e.g. http://pi-hole)
        if (!host.contains(".")) {
            return true
        }

        // Check for local IP ranges (simplified)
        // IPv4: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.1/8
        val ipv4Pattern = Regex("""^(10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.|127\.)""")
        if (ipv4Pattern.containsMatchIn(host)) {
            return true
        }

        // IPv6: fc00::/7 (Unique Local Address), ::1 (Loopback)
        if (host.startsWith("fc00:", ignoreCase = true) || host.startsWith("fd00:", ignoreCase = true) || host == "::1") {
            return true
        }

        return false
    }

    /**
     * More robust check that includes DNS resolution to handle domains pointing to local IPs
     */
    suspend fun isLocalAddressRobust(url: String): Boolean {
        // Fast path: string check
        if (isLocalAddress(url)) return true

        // Slow path: DNS resolution
        return withContext(Dispatchers.IO) {
            try {
                val uri = url.toUri()
                val host = uri.host ?: return@withContext false
                val address = InetAddress.getByName(host)
                address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress
            } catch (e: Exception) {
                false
            }
        }
    }
}
