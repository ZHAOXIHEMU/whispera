package me.rerere.rikkahub.ui.pages.wechat

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatListTimeTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-10-29T08:00:00Z") // Thursday

    @Test fun calendarLabelsUseMessageTimeAndMondayWeekBoundary() {
        assertEquals("15:26", chatListTime(Instant.parse("2026-10-29T07:26:00Z"), now, zone))
        assertEquals("昨天", chatListTime(Instant.parse("2026-10-28T04:00:00Z"), now, zone))
        assertEquals("星期二", chatListTime(Instant.parse("2026-10-27T04:00:00Z"), now, zone))
        assertEquals("星期一", chatListTime(Instant.parse("2026-10-26T04:00:00Z"), now, zone))
        assertEquals("10月25日", chatListTime(Instant.parse("2026-10-25T04:00:00Z"), now, zone))
        assertEquals("2025年10月", chatListTime(Instant.parse("2025-10-29T04:00:00Z"), now, zone))
    }

    @Test fun yesterdayWinsAtYearChangeAndLocalMidnight() {
        val newYear = Instant.parse("2026-12-31T16:02:00Z")
        assertEquals("00:01", chatListTime(Instant.parse("2026-12-31T16:01:00Z"), newYear, zone))
        assertEquals("昨天", chatListTime(Instant.parse("2026-12-31T15:59:00Z"), newYear, zone))
        assertEquals("2026年12月", chatListTime(Instant.parse("2026-12-30T10:00:00Z"), newYear, zone))
    }
}
