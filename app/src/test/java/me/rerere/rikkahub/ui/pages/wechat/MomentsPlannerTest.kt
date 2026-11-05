package me.rerere.rikkahub.ui.pages.wechat

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class MomentsPlannerTest {
    @Test fun beijingSlotsAndRestartCatchup(){
        val midnight=Instant.parse("2026-10-27T16:00:00Z").toEpochMilli()
        assertEquals(midnight,MomentsPlanner.slot(midnight+1))
        assertEquals(midnight-12*3600_000L,MomentsPlanner.slot(midnight-1))
        val initial=MomentsPlanner.times(null,midnight+1000)
        assertEquals(20,initial.size);assertEquals(midnight,initial.last())
        assertTrue(MomentsPlanner.times(midnight,midnight+3600_000).isEmpty())
        assertEquals(listOf(midnight+MomentsPlanner.INTERVAL,midnight+2*MomentsPlanner.INTERVAL),MomentsPlanner.times(midnight,midnight+2*MomentsPlanner.INTERVAL+1))
        assertTrue(MomentsPlanner.times(midnight,midnight-1).isEmpty())
    }
    @Test fun thousandsOfLayoutsRespectMediaLimits(){
        val photos=(1..20).map{LocalMedia("photo-$it",false)};val videos=(1..10).map{LocalMedia("video-$it",true)}
        repeat(3000){seed->
            val media=MomentsPlanner.choose(photos,videos,Random(seed))
            assertTrue(media.size in 1..9);assertEquals(media.size,media.distinctBy{it.uri}.size)
            val clips=media.count{it.video}
            assertTrue(clips<=if(clips==media.size)3 else 2)
        }
        assertTrue(MomentsPlanner.choose(emptyList(),emptyList()).isEmpty())
        assertEquals(1,MomentsPlanner.choose(photos.take(1),emptyList()).size)
        repeat(100){assertTrue(MomentsPlanner.choose(emptyList(),videos).size in 1..3)}
    }
}
