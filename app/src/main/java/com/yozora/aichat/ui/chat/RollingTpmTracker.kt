package com.yozora.aichat.ui.chat

internal enum class TpmComfortZone {
    Green,
    Yellow,
    Red
}

internal class RollingTpmTracker(
    private val limit: Int,
    private val windowMs: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class UsageEvent(
        val timestampMs: Long,
        val tokens: Int
    )

    private val events = ArrayDeque<UsageEvent>()

    @Synchronized
    fun currentTokens(): Int {
        prune(clock())
        return events.sumOf { it.tokens }
    }

    @Synchronized
    fun record(tokens: Int): Int {
        val now = clock()
        prune(now)
        if (tokens > 0) {
            events.addLast(UsageEvent(now, tokens))
        }
        return events.sumOf { it.tokens }
    }

    fun zoneFor(tokens: Int): TpmComfortZone {
        val ratio = tokens.toDouble() / limit.toDouble()
        return when {
            ratio >= RED_THRESHOLD -> TpmComfortZone.Red
            ratio >= YELLOW_THRESHOLD -> TpmComfortZone.Yellow
            else -> TpmComfortZone.Green
        }
    }

    private fun prune(now: Long) {
        while (events.isNotEmpty() && now - events.first().timestampMs >= windowMs) {
            events.removeFirst()
        }
    }

    companion object {
        const val YELLOW_THRESHOLD = 0.70
        const val RED_THRESHOLD = 0.88
    }
}
