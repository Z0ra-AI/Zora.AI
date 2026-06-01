package com.yozora.aichat.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

class Rule34ImageRepository {
    suspend fun randomExplicitImageUrl(
        apiKey: String,
        userId: String,
        tags: String
    ): String? = withContext(Dispatchers.IO) {
        val cleanApiKey = apiKey.trim()
        val cleanUserId = userId.trim()
        if (cleanApiKey.isEmpty() || cleanUserId.isEmpty()) return@withContext null

        tagAttempts(tags).firstNotNullOfOrNull { attempt ->
            fetchImageUrl(
                apiKey = cleanApiKey,
                userId = cleanUserId,
                tags = attempt.query,
                requiredTags = attempt.requiredTags
            )
        }
    }

    private fun tagAttempts(tags: String): List<TagAttempt> {
        val orderedTags = tags
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .split(' ')
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("rating") }
            .distinct()
            .prioritizeRequestTags()

        if (orderedTags.isEmpty()) {
            return listOf(TagAttempt(query = "rating:explicit", requiredTags = emptyList()))
        }

        val requestSpecificTags = orderedTags.filterNot { it in genericSupportTags }
        val genderTag = orderedTags.firstOrNull { it in genderSupportTags }

        return buildList {
            add(orderedTags)
            for (size in orderedTags.size - 1 downTo 1) {
                add(orderedTags.take(size))
            }
            if (requestSpecificTags.isNotEmpty()) {
                add(requestSpecificTags)
                for (size in requestSpecificTags.size - 1 downTo 1) {
                    add(requestSpecificTags.take(size))
                }
                if (genderTag != null) {
                    requestSpecificTags.forEach { tag ->
                        add(listOf(tag, genderTag))
                    }
                }
            }
        }
            .distinct()
            .map { attemptTags ->
                TagAttempt(
                    query = (attemptTags + "rating:explicit").joinToString(" "),
                    requiredTags = attemptTags.filterNot { it in genericSupportTags }
                )
            }
    }

    private fun fetchImageUrl(
        apiKey: String,
        userId: String,
        tags: String,
        requiredTags: List<String>
    ): String? {
        return runCatching {
            val url = URL(
                "https://api.rule34.xxx/index.php" +
                    "?page=dapi" +
                    "&s=post" +
                    "&q=index" +
                    "&json=1" +
                    "&tags=${tags.urlEncode()}" +
                    "&limit=20" +
                    "&pid=${Random.nextInt(0, 6)}" +
                    "&api_key=${apiKey.urlEncode()}" +
                    "&user_id=${userId.urlEncode()}"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    return@runCatching null
                }
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val posts = JSONArray(responseText)
                val candidates = mutableListOf<Rule34Post>()
                for (index in 0 until posts.length()) {
                    val post = posts.optJSONObject(index) ?: continue
                    val fileUrl = post
                        ?.optString("file_url")
                        ?.takeIf { it.isNotBlank() }
                        ?.normalizeFileUrl()
                    if (fileUrl != null && fileUrl.isDisplayableImage()) {
                        candidates += Rule34Post(
                            fileUrl = fileUrl,
                            tags = post.optString("tags")
                                .split(Regex("\\s+"))
                                .filter { it.isNotBlank() }
                                .toSet()
                        )
                    }
                }
                candidates.bestMatch(requiredTags)?.fileUrl
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun List<Rule34Post>.bestMatch(requiredTags: List<String>): Rule34Post? {
        if (isEmpty()) return null
        if (requiredTags.isEmpty()) return random(Random)

        val scored = map { post: Rule34Post ->
            var score = 0
            requiredTags.forEach { tag ->
                score += when {
                    tag in post.tags -> 12
                    post.tags.any { it.contains(tag) || tag.contains(it) } -> 3
                    else -> 0
                }
            }
            ScoredRule34Post(post = post, score = score)
        }
        val bestScore = scored.maxOfOrNull { it.score } ?: return null
        if (bestScore <= 0) return null
        return scored
            .filter { it.score == bestScore }
            .map { it.post }
            .random(Random)
    }

    private fun List<String>.prioritizeRequestTags(): List<String> {
        val requestSpecific = filterNot { it in genericSupportTags }
        val support = filter { it in genericSupportTags }
        return requestSpecific + support
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    private fun String.normalizeFileUrl(): String {
        return if (startsWith("//")) "https:$this" else this
    }

    private fun String.isDisplayableImage(): Boolean {
        val lower = lowercase()
        return !lower.endsWith(".mp4") && !lower.endsWith(".webm")
    }

    private companion object {
        val genderSupportTags = setOf(
            "1girl",
            "1boy",
            "2girls",
            "2boys",
            "multiple_girls",
            "multiple_boys"
        )
        val genericSupportTags = genderSupportTags + setOf(
            "solo",
            "looking_at_viewer",
            "smile",
            "blush",
            "open_mouth",
            "closed_mouth",
            "standing",
            "sitting",
            "portrait",
            "upper_body",
            "cowboy_shot",
            "full_body",
            "anime"
        )
    }
}

private data class TagAttempt(
    val query: String,
    val requiredTags: List<String>
)

private data class Rule34Post(
    val fileUrl: String,
    val tags: Set<String>
)

private data class ScoredRule34Post(
    val post: Rule34Post,
    val score: Int
)
