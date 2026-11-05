package me.rerere.rikkahub.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Keep upstream promotional/API services out of the fork's shared HTTP client. */
object UpstreamNetworkPolicy : Interceptor {
    fun blocked(host: String): Boolean {
        val normalized=host.lowercase().trimEnd('.')
        return listOf("rikka-ai.com", "rikkahub.luoca.cn", "ai.ota1245.top").any {
            normalized==it || normalized.endsWith(".$it")
        }
    }
    override fun intercept(chain: Interceptor.Chain): Response {
        val host=chain.request().url.host
        if(blocked(host)) throw IOException("whispera 已停用上游服务：$host")
        return chain.proceed(chain.request())
    }
}
