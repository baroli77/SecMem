package com.secondmemory.app.data

import com.secondmemory.app.domain.UrlMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.charset.Charset

object MetadataFetcher {
    private const val MAX_BYTES = 120_000
    private val BLOCKED_HOSTS = setOf(
        "localhost",
        "localhost.localdomain",
        "ip6-localhost",
        "metadata.google.internal",
    )

    suspend fun fetch(url: String): UrlMetadata? = withContext(Dispatchers.IO) {
        try {
            val target = sanitize(url) ?: return@withContext null
            val conn = (target.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36",
                )
            }
            conn.connect()
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: return@withContext null
                conn.disconnect()
                val redirected = runCatching { URI(target.toURI().toString()).resolve(location).toURL() }.getOrNull()
                    ?: return@withContext null
                if (sanitize(redirected.toString()) == null) return@withContext null
                return@withContext fetchOnce(redirected)
            }
            val result = readHtml(conn)
            conn.disconnect()
            result?.let { parseOpenGraph(it, url) }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchOnce(target: URL): UrlMetadata? {
        val conn = (target.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 4000
            readTimeout = 4000
        }
        conn.connect()
        val html = readHtml(conn)
        conn.disconnect()
        return html?.let { parseOpenGraph(it, target.toString()) }
    }

    private fun readHtml(conn: HttpURLConnection): String? {
        val type = conn.contentType.orEmpty().lowercase()
        if (type.isNotBlank() &&
            !type.contains("html") &&
            !type.contains("xml") &&
            !type.startsWith("text/")
        ) {
            return null
        }
        val length = conn.contentLengthLong
        if (length > MAX_BYTES * 4L) return null
        val bytes = conn.inputStream.use { input ->
            val out = ByteArrayOutputStream(MAX_BYTES.coerceAtMost(32_768))
            val buf = ByteArray(8_192)
            var total = 0
            while (total < MAX_BYTES) {
                val n = input.read(buf, 0, minOf(buf.size, MAX_BYTES - total))
                if (n <= 0) break
                out.write(buf, 0, n)
                total += n
            }
            out.toByteArray()
        }
        return String(bytes, Charset.forName("UTF-8"))
    }

    fun sanitize(raw: String): URL? {
        return try {
            val url = URL(raw)
            if (url.protocol != "https") return null
            val host = IDN.toASCII(url.host ?: return null).lowercase()
            if (host in BLOCKED_HOSTS || host.endsWith(".localhost") || host.endsWith(".internal")) return null
            if (isPrivateHost(host)) return null
            url
        } catch (_: Exception) {
            null
        }
    }

    private fun isPrivateHost(host: String): Boolean {
        if (host == "0.0.0.0" || host == "::1" || host == "127.0.0.1") return true
        return try {
            val address = InetAddress.getByName(host)
            address.isAnyLocalAddress ||
                address.isLoopbackAddress ||
                address.isSiteLocalAddress ||
                address.isLinkLocalAddress ||
                address.isMulticastAddress
        } catch (_: Exception) {
            true
        }
    }

    fun parseOpenGraph(html: String, fallbackUrl: String): UrlMetadata {
        fun prop(name: String): String? {
            val patterns = listOf(
                Regex("""<meta[^>]+property=["']og:$name["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:$name["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+name=["']$name["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            )
            return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }?.decode()
        }

        val title = prop("title")
            ?: Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.decode()
        return UrlMetadata(
            title = title,
            description = prop("description"),
            image = prop("image"),
            siteName = prop("site_name"),
            canonicalUrl = Regex(
                """<link[^>]+rel=["']canonical["'][^>]+href=["']([^"']+)["']""",
                RegexOption.IGNORE_CASE,
            ).find(html)?.groupValues?.get(1) ?: fallbackUrl,
        )
    }

    private fun String.decode(): String {
        val amp = "&" + "amp;"
        val lt = "&" + "lt;"
        val gt = "&" + "gt;"
        val quot = "&" + "quot;"
        val apos = "&" + "#39;"
        return replace(amp, "&")
            .replace(lt, "<")
            .replace(gt, ">")
            .replace(quot, "\"")
            .replace(apos, "'")
            .trim()
    }
}
