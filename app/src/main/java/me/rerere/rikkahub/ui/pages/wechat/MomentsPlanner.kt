package me.rerere.rikkahub.ui.pages.wechat

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class LocalMedia(val uri:String,val video:Boolean)
@Serializable
data class LocalMoment(val id:String,val time:Long,val authorId:String?=null,val media:List<LocalMedia>,val liked:Boolean=false)
@Serializable
data class MomentsSnapshot(val posts:List<LocalMoment> = emptyList(),val lastSlot:Long?=null)

object MomentsPlanner {
    const val INTERVAL=12*60*60*1000L
    private const val BEIJING_OFFSET=8*60*60*1000L
    fun slot(now:Long):Long=Math.floorDiv(now+BEIJING_OFFSET,INTERVAL)*INTERVAL-BEIJING_OFFSET
    fun times(last:Long?,now:Long):List<Long> {
        val current=slot(now)
        if(last==null)return (19 downTo 0).map{current-it*INTERVAL}
        if(current<=last)return emptyList()
        return generateSequence(last+INTERVAL){it+INTERVAL}.takeWhile{it<=current}.toList()
    }
    fun choose(images:List<LocalMedia>,videos:List<LocalMedia>,random:Random=Random.Default):List<LocalMedia> {
        val photoPool=images.filterNot{it.video}.distinctBy{it.uri}.shuffled(random)
        val videoPool=videos.filter{it.video}.distinctBy{it.uri}.shuffled(random)
        if(photoPool.isEmpty()&&videoPool.isEmpty())return emptyList()
        val modes=buildList{if(photoPool.isNotEmpty())add(0);if(videoPool.isNotEmpty())add(1);if(photoPool.isNotEmpty()&&videoPool.isNotEmpty())add(2)}
        return when(modes.random(random)) {
            0->{val count=listOf(1,3,9).filter{it<=photoPool.size}.random(random);photoPool.take(count)}
            1->videoPool.take(random.nextInt(1,minOf(3,videoPool.size)+1))
            else->{val clipCount=random.nextInt(1,minOf(2,videoPool.size)+1);(videoPool.take(clipCount)+photoPool.take(random.nextInt(1,minOf(9-clipCount,photoPool.size)+1))).shuffled(random)}
        }
    }
}
