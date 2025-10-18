package dev.barreto.fleetctrl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import kotlin.math.log10

data class VehicleImageCandidate(
    val imageUrl: String,
    val previewUrl: String = imageUrl,
    val width: Int? = null,
    val height: Int? = null,
    val providerId: String,
    val providerName: String,
    val title: String? = null,
    val pageUrl: String? = null,
    val attribution: String? = null,
    val score: Double = 0.0
)

data class VehicleImageFetchHints(
    val brand: String,
    val model: String,
    val year: Int?,
    val query: String
)

interface VehicleImageProvider {
    val id: String
    val displayName: String
    suspend fun search(query: String, hints: VehicleImageFetchHints, limit: Int): List<VehicleImageCandidate>
}

class VehicleImageFetcher(
    private val providers: List<VehicleImageProvider> = defaultProviders()
) {
    suspend fun fetchCandidates(
        brand: String,
        model: String,
        year: Int?,
        limit: Int = 6
    ): List<VehicleImageCandidate> {
        val normalizedBrand = brand.trim()
        val normalizedModel = model.trim()
        if (normalizedBrand.isBlank() && normalizedModel.isBlank()) return emptyList()
        val queries = buildQueries(normalizedBrand, normalizedModel, year)
        val resultMap = LinkedHashMap<String, VehicleImageCandidate>()

        for (query in queries) {
            val hints = VehicleImageFetchHints(
                brand = normalizedBrand,
                model = normalizedModel,
                year = year,
                query = query
            )
            for (provider in providers) {
                val candidates = provider.search(query, hints, limit)
                for (candidate in candidates) {
                    if (candidate.imageUrl.isBlank()) continue
                    resultMap.putIfAbsent(candidate.imageUrl, candidate)
                    if (resultMap.size >= limit) break
                }
                if (resultMap.size >= limit) break
            }
            if (resultMap.size >= limit) break
        }

        return resultMap.values.sortedByDescending { it.score }
    }

    private fun buildQueries(brand: String, model: String, year: Int?): List<String> {
        val queries = LinkedHashSet<String>()
        val brandModel = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").trim()
        val sanitizedModel = model.replace(Regex("\\b\\d+\\w?\\b"), "").trim()
        val lowerBrand = brand.lowercase()

        if (brandModel.isNotBlank()) {
            queries += brandModel
            queries += "$brandModel car"
            queries += "$brandModel exterior"
            queries += "$brandModel front view"
        }

        year?.let {
            if (brandModel.isNotBlank()) {
                queries += "$brandModel $it"
                queries += "$brandModel $it car"
                queries += "$brandModel $it front"
            }
        }

        if (sanitizedModel.isNotBlank() && sanitizedModel.lowercase() != lowerBrand) {
            queries += "$brand $sanitizedModel"
            queries += "$brand $sanitizedModel car"
        }

        if (queries.isEmpty()) {
            queries += listOfNotNull(
                listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
                brand.takeIf { it.isNotBlank() },
                model.takeIf { it.isNotBlank() }
            )
        }

        return queries.filter { it.isNotBlank() }
    }

    companion object {
        private fun defaultProviders(): List<VehicleImageProvider> = listOf(
            WikipediaImageProvider("pt"),
            WikipediaImageProvider("en"),
            WikimediaCommonsProvider()
        )
    }
}

private class WikipediaImageProvider(
    private val language: String
) : VehicleImageProvider {
    override val id: String = "wikipedia_$language"
    override val displayName: String = "Wikipedia (${language.uppercase()})"

    override suspend fun search(query: String, hints: VehicleImageFetchHints, limit: Int): List<VehicleImageCandidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url =
            "https://$language.wikipedia.org/w/api.php?action=query&format=json&generator=search&gsrlimit=5&gsrnamespace=0" +
                    "&gsrsearch=$encoded&prop=pageimages|info&piprop=original|thumbnail&pithumbsize=800&pilicense=any&inprop=url"

        val json = HttpHelper.get(url) ?: return emptyList()
        val root = JSONObject(json)
        val queryObj = root.optJSONObject("query") ?: return emptyList()
        val pages = queryObj.optJSONObject("pages") ?: return emptyList()
        val keys = pages.names() ?: return emptyList()
        val candidates = mutableListOf<VehicleImageCandidate>()

        for (i in 0 until keys.length()) {
            if (candidates.size >= limit) break
            val page = pages.optJSONObject(keys.getString(i)) ?: continue
            val title = page.optString("title", null)
            val pageUrl = page.optString("fullurl", null)
            val original = page.optJSONObject("original")
            val thumb = page.optJSONObject("thumbnail")
            val sourceUrl = original?.optString("source") ?: thumb?.optString("source") ?: continue
            val width = original?.optInt("width")?.takeIf { it > 0 } ?: thumb?.optInt("width")?.takeIf { it > 0 }
            val height = original?.optInt("height")?.takeIf { it > 0 } ?: thumb?.optInt("height")?.takeIf { it > 0 }

            val score = scoreCandidate(title, hints, index = i, width = width, height = height)
            candidates += VehicleImageCandidate(
                imageUrl = sourceUrl,
                previewUrl = thumb?.optString("source") ?: sourceUrl,
                width = width,
                height = height,
                providerId = id,
                providerName = displayName,
                title = title,
                pageUrl = pageUrl,
                score = score
            )
        }

        return candidates
    }
}

private class WikimediaCommonsProvider : VehicleImageProvider {
    override val id: String = "wikimedia_commons"
    override val displayName: String = "Wikimedia Commons"

    override suspend fun search(query: String, hints: VehicleImageFetchHints, limit: Int): List<VehicleImageCandidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url =
            "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=8" +
                    "&gsrsearch=$encoded&prop=imageinfo&iiprop=url|mime|size|extmetadata&iiurlwidth=800&iiurlheight=800"

        val json = HttpHelper.get(url) ?: return emptyList()
        val root = JSONObject(json)
        val queryObj = root.optJSONObject("query") ?: return emptyList()
        val pages = queryObj.optJSONObject("pages") ?: return emptyList()
        val keys = pages.names() ?: return emptyList()
        val candidates = mutableListOf<VehicleImageCandidate>()

        for (i in 0 until keys.length()) {
            if (candidates.size >= limit) break
            val page = pages.optJSONObject(keys.getString(i)) ?: continue
            val title = page.optString("title", null)
            val infos = page.optJSONArray("imageinfo") ?: continue
            if (infos.length() == 0) continue
            val info = infos.optJSONObject(0) ?: continue
            val urlFull = info.optString("url", null) ?: continue
            val mime = info.optString("mime", "")
            if (!mime.startsWith("image/")) continue
            val thumb = info.optString("thumburl", urlFull)
            val width = info.optInt("width").takeIf { it > 0 }
            val height = info.optInt("height").takeIf { it > 0 }
            val extMeta = info.optJSONObject("extmetadata")
            val artist = extMeta?.optJSONObject("Artist")?.optString("value")
            val licenceShort = extMeta?.optJSONObject("LicenseShortName")?.optString("value")
            val attribution = listOfNotNull(artist, licenceShort).joinToString(" • ").takeIf { it.isNotBlank() }

            val score = scoreCandidate(title, hints, index = i, width = width, height = height, bonus = 5.0)
            candidates += VehicleImageCandidate(
                imageUrl = urlFull,
                previewUrl = thumb,
                width = width,
                height = height,
                providerId = id,
                providerName = displayName,
                title = title,
                pageUrl = buildCommonsPageUrl(title),
                attribution = attribution,
                score = score
            )
        }

        return candidates
    }

    private fun buildCommonsPageUrl(title: String?): String? {
        if (title.isNullOrBlank()) return null
        val encoded = URLEncoder.encode(title, Charsets.UTF_8.name())
        return "https://commons.wikimedia.org/wiki/$encoded"
    }
}

private fun scoreCandidate(
    title: String?,
    hints: VehicleImageFetchHints,
    index: Int,
    width: Int?,
    height: Int?,
    bonus: Double = 0.0
): Double {
    var score = 100.0 - (index * 5)
    val t = title?.lowercase().orEmpty()
    if (hints.brand.isNotBlank() && t.contains(hints.brand.lowercase())) score += 20
    if (hints.model.isNotBlank() && t.contains(hints.model.lowercase())) score += 20
    hints.year?.let { if (t.contains(it.toString())) score += 10 }
    if (t.contains("concept") || t.contains("prototype")) score -= 15
    if (t.contains("interior")) score -= 10
    if (t.contains("rear")) score -= 5
    if (width != null && height != null) {
        score += log10((width * height).toDouble()).coerceAtLeast(0.0)
    }
    score += bonus
    return score
}

private object HttpHelper {
    suspend fun get(urlStr: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "FleetCtrl/1.0 (Android)")
            }
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
