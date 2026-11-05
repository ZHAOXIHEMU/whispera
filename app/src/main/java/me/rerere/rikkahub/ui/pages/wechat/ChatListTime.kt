package me.rerere.rikkahub.ui.pages.wechat

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Calendar boundaries use the phone's timezone, including the Monday week boundary. */
internal fun chatListTime(time: Instant, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    val dateTime = time.atZone(zone)
    val date = dateTime.toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when {
        date == today -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        date == today.minusDays(1) -> "昨天"
        date.year != today.year -> dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月"))
        date in today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))..today ->
            "星期" + listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]
        else -> dateTime.format(DateTimeFormatter.ofPattern("M月d日"))
    }
}
