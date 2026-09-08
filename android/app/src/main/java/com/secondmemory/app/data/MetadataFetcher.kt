package com.secondmemory.app.data

import com.secondmemory.app.domain.UrlMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object MetadataFetcher {
    suspend fun fetch(url: String): UrlMetadata? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36",
                )
            }
            conn.connect()
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            val html = String(bytes.take(120_000).toByteArray(), Charset.forName("UTF-8"))
            parseOpenGraph(html, url)
        } catch (_: Exception) {
            null
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
        val description = prop("description")
        val image = prop("image")
        val siteName = prop("site_name")
        val canonical = Regex(
            """<link[^>]+rel=["']canonical["'][^>]+href=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1) ?: fallbackUrl

        return UrlMetadata(
            title = title,
            description = description,
            image = image,
            siteName = siteName,
            canonicalUrl = canonical,
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

