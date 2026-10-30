package com.ecoruta.dispositivo.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import com.ecoruta.dispositivo.R
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Elapsed time rounded down to the largest unit that fits: 59 s, then 1 min, then 1 h. */
data class Ago(val amount: Long, val unit: Unit) {
    enum class Unit { SECONDS, MINUTES, HOURS }
}

fun agoOf(then: Instant, now: Instant): Ago {
    val seconds = Duration.between(then, now).seconds.coerceAtLeast(0)
    return when {
        seconds < SECONDS_PER_MINUTE -> Ago(seconds, Ago.Unit.SECONDS)
        seconds < SECONDS_PER_HOUR -> Ago(seconds / SECONDS_PER_MINUTE, Ago.Unit.MINUTES)
        else -> Ago(seconds / SECONDS_PER_HOUR, Ago.Unit.HOURS)
    }
}

/**
 * "hace 5 s", "hace 3 min", "hace 2 h": read at a glance, unlike a clock time the driver would
 * have to compare with the current one.
 */
@Composable
fun agoText(then: Instant, now: Instant): String {
    val ago = agoOf(then, now)
    val res = when (ago.unit) {
        Ago.Unit.SECONDS -> R.string.ago_seconds
        Ago.Unit.MINUTES -> R.string.ago_minutes
        Ago.Unit.HOURS -> R.string.ago_hours
    }
    return stringResource(res, ago.amount)
}

/** The current instant, refreshed every second, for texts like [agoText]. */
@Composable
fun rememberNow(): Instant {
    val now by produceState(Instant.now()) {
        while (true) {
            delay(1.seconds)
            value = Instant.now()
        }
    }
    return now
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
