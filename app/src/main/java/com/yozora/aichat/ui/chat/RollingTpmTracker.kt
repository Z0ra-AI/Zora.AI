package com.yozora.aichat.ui.chat

import kotlin.math.ceil
import kotlin.math.max

internal enum class TpmComfortZone {
    Green,
    Yellow,
    Red
}

/**
 * Owns two concerns that used to be split across the app and undercounted:
 *
 *  1. A rolling Tokens-Per-Minute window that is [settle]d against REAL API usage
 *     (not just pre-send estimates), so the meter matches what the provider charged.
 *  2. A per-request context budget used to decide when older messages should be
 *     summarized out of the verbatim history.
 *
 * "Remember more" dial: target ~70k prompt tokens, hard cap ~110k. Memory that has
 * already been summarized is kept in a bounded rolling buffer so it never explodes.
 */
internal class TokenBudgetManager(
    private val tpmLimit: Int = TPM_LIMIT,
    private val windowMs: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class UsageEvent(val timestampMs: Long, val tokens: Int)

    private val events = ArrayDeque<UsageEvent>()

    /** Estimated tokens reserved before a send is fired, keyed by request id. */
    private val reservations = LinkedHashMap<String, Int>()

    @Synchronized
    fun currentTokens(): Int {
        prune(clock())
        return events.sumOf { it.tokens }
    }

    /**
     * Reserve [estimate] tokens for an in-flight request identified by [requestId].
     * Returns the projected token count (current + all reservations + this estimate)
     * so the caller can decide whether to apply backpressure before sending.
     */
    @Synchronized
    fun reserve(requestId: String, estimate: Int): Int {
        prune(clock())
        if (estimate > 0) {
            reservations[requestId] = estimate
        }
        return currentTokens()
    }

    /**
     * Reconcile a reserved request with the real token total returned by the API.
     * The estimate is removed from in-flight reservations and the actual usage is
     * recorded into the rolling window. Safe to call even if [reserve] was skipped.
     */
    @Synchronized
    fun settle(requestId: String, actualTokens: Int) {
        val now = clock()
        prune(now)
        reservations.remove(requestId)
        if (actualTokens > 0) {
            events.addLast(UsageEvent(now, actualTokens))
        }
    }

    /** Release a reservation without recording usage (e.g. request failed before any bytes counted). */
    @Synchronized
    fun release(requestId: String) {
        reservations.remove(requestId)
    }

    @Synchronized
    fun projectedTokens(): Int {
        prune(clock())
        return events.sumOf { it.tokens } + reservations.values.sum()
    }

    fun zoneFor(tokens: Int): TpmComfortZone {
        val ratio = tokens.toDouble() / tpmLimit.toDouble()
        return when {
            ratio >= RED_THRESHOLD -> TpmComfortZone.Red
            ratio >= YELLOW_THRESHOLD -> TpmComfortZone.Yellow
            else -> TpmComfortZone.Green
        }
    }

    fun projectedZone(): TpmComfortZone = zoneFor(projectedTokens())

    private fun prune(now: Long) {
        while (events.isNotEmpty() && now - events.first().timestampMs >= windowMs) {
            events.removeFirst()
        }
        if (events.size > MAX_EVENTS) {
            repeat(events.size - MAX_EVENTS) { events.removeFirst() }
        }
    }

    companion object {
        const val YELLOW_THRESHOLD = 0.70
        const val RED_THRESHOLD = 0.88

        // "Remember more" dial: keep a large verbatim window before summarizing.
        const val TPM_LIMIT = 250_000
        const val TPM_YELLOW_TOKENS = 175_000
        const val TARGET_PROMPT_TOKENS = 70_000
        const val HARD_PROMPT_TOKEN_CAP = 110_000

        // How much of the archived/summarized memory is injected per request.
        const val ARCHIVE_INJECTION_CAP_CHARS = 8_000
        const val ARCHIVE_RECOMPRESS_THRESHOLD_CHARS = 12_000

        // When trimming verbatim history, keep at least this many recent turns.
        const val MIN_RETAINED_MESSAGES = 6
        const val AGGRESSIVE_RETAIN_FRACTION = 0.30
        const val NORMAL_RETAIN_FRACTION = 0.55

        private const val MAX_EVENTS = 4_000
    }
}

/**
 * Token estimator that does not silently undercount CJK text the way chars/4 does.
 * Latin/digits stay close to ~4 chars/token; CJK runs ~1.5 chars/token, so it is
 * weighted separately to keep the budget honest for Japanese roleplay.
 */
internal object TokenEstimator {
    private val cjkRegex = Regex("[\\u3000-\\u30FF\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uF900-\\uFAFF]")

    fun estimateTextTokens(text: String): Int {
        if (text.isEmpty()) return 0
        val cjk = cjkRegex.findAll(text).sumOf { it.value.length }
        val rest = text.length - cjk
        val tokens = (cjk.toDouble() / CJK_CHARS_PER_TOKEN) +
            (rest.toDouble() / LATIN_CHARS_PER_TOKEN)
        return ceil(tokens).coerceAtLeast(1.0).toInt()
    }

    /**
     * Whole-payload estimate: system prompt + every history message + user input,
     * plus a small per-message overhead for the JSON/role framing the providers add.
     */
    fun estimateRequestTokens(
        systemPrompt: String,
        historyContents: Collection<String>,
        userInput: String
    ): Int {
        val overhead = (historyContents.size + 2) * PER_MESSAGE_OVERHEAD
        val total = estimateTextTokens(systemPrompt) +
            estimateTextTokens(userInput) +
            historyContents.sumOf { estimateTextTokens(it) } +
            overhead
        return total.coerceAtLeast(1)
    }

    private const val LATIN_CHARS_PER_TOKEN = 4.0
    private const val CJK_CHARS_PER_TOKEN = 1.5
    private const val PER_MESSAGE_OVERHEAD = 4
}

/**
 * Decides how many of the newest messages must stay verbatim and how many older
 * ones are candidates for summarization, given the projected prompt size.
 */
internal object ContextTrimPolicy {
    data class TrimPlan(
        val shouldTrim: Boolean,
        val retainCount: Int,
        val archiveCandidates: Int
    ) {
        val totalKeptInMemory: Int
            get() = retainCount
    }

    fun plan(
        mutableCount: Int,
        projectedPromptTokens: Int,
        aggressive: Boolean
    ): TrimPlan {
        if (mutableCount <= TokenBudgetManager.MIN_RETAINED_MESSAGES) {
            return TrimPlan(shouldTrim = false, retainCount = mutableCount, archiveCandidates = 0)
        }
        val overTarget = projectedPromptTokens > TokenBudgetManager.TARGET_PROMPT_TOKENS
        val overHardCap = projectedPromptTokens > TokenBudgetManager.HARD_PROMPT_TOKEN_CAP
        if (!overTarget && !aggressive) {
            return TrimPlan(shouldTrim = false, retainCount = mutableCount, archiveCandidates = 0)
        }
        val fraction = when {
            overHardCap -> TokenBudgetManager.AGGRESSIVE_RETAIN_FRACTION
            aggressive -> TokenBudgetManager.AGGRESSIVE_RETAIN_FRACTION
            else -> TokenBudgetManager.NORMAL_RETAIN_FRACTION
        }
        val retain = max(
            TokenBudgetManager.MIN_RETAINED_MESSAGES,
            ceil(mutableCount * fraction).toInt()
        ).coerceAtMost(mutableCount)
        val archive = mutableCount - retain
        return TrimPlan(
            shouldTrim = archive > 0,
            retainCount = retain,
            archiveCandidates = archive
        )
    }

    /** The largest prefix of [archivedContext] that should be injected per request. */
    fun injectedArchiveSlice(archivedContext: String): String {
        val trimmed = archivedContext.trim()
        if (trimmed.length <= TokenBudgetManager.ARCHIVE_INJECTION_CAP_CHARS) {
            return trimmed
        }
        // Keep the most recent (last) portion of the rolling summary — it is the
        // freshest state and what continuity depends on.
        return trimmed.takeLast(TokenBudgetManager.ARCHIVE_INJECTION_CAP_CHARS)
    }

    /** Whether the rolling memory is large enough to warrant recompression. */
    fun shouldRecompressArchive(archivedContext: String): Boolean {
        return archivedContext.trim().length > TokenBudgetManager.ARCHIVE_RECOMPRESS_THRESHOLD_CHARS
    }
}
