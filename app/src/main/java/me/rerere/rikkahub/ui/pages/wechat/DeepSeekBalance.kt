package me.rerere.rikkahub.ui.pages.wechat

import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.Settings
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal
import java.net.URI
import java.util.concurrent.TimeUnit

data class BalanceCurrency(val currency:String,val total:String,val granted:String,val toppedUp:String) {
    val symbol:String get()=when(currency){"CNY"->"¥";"USD"->"$";else->currency+" "}
}
data class DeepSeekBalance(val available:Boolean,val currencies:List<BalanceCurrency>) {
    val primary get()=currencies.firstOrNull{it.currency=="CNY"}?:currencies.firstOrNull()
    companion object {
        fun parse(text:String):DeepSeekBalance {
            val data=JSONObject(text);val array=data.getJSONArray("balance_infos")
            val rows=(0 until array.length()).map{index->
                val row=array.getJSONObject(index)
                fun amount(key:String)=row.getString(key).also{BigDecimal(it)}
                BalanceCurrency(row.getString("currency"),amount("total_balance"),amount("granted_balance"),amount("topped_up_balance"))
            }
            require(rows.isNotEmpty()){"接口未返回余额"}
            return DeepSeekBalance(data.getBoolean("is_available"),rows)
        }
    }
}

fun deepSeekProvider(settings:Settings):ProviderSetting.OpenAI?=settings.providers.filterIsInstance<ProviderSetting.OpenAI>().firstOrNull{provider->
    provider.enabled&&runCatching{val uri=URI(provider.baseUrl);uri.scheme=="https"&&uri.host.equals("api.deepseek.com",true)&&(uri.port==-1||uri.port==443)&&uri.userInfo==null}.getOrDefault(false)
}

object DeepSeekBalanceApi {
    const val ENDPOINT="https://api.deepseek.com/user/balance"
    const val USAGE="https://platform.deepseek.com/usage"
    private val client=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    fun get(key:String):DeepSeekBalance {
        require(key.isNotBlank()){"请先填写 DeepSeek Key"}
        val request=Request.Builder().url(ENDPOINT).header("Authorization","Bearer "+key.trim()).header("Accept","application/json").build()
        return client.newCall(request).execute().use{response->
            if(!response.isSuccessful)error(when(response.code){401,403->"Key 无效或没有查询权限";429->"查询过于频繁，请稍后重试";else->"余额查询失败（HTTP ${response.code}）"})
            DeepSeekBalance.parse(response.body.string())
        }
    }
    fun allowedPage(url:String):Boolean=runCatching {
        val uri=URI(url)
        uri.scheme=="https"&&uri.userInfo==null&&(uri.port==-1||uri.port==443)&&when(uri.host?.lowercase()){
            "platform.deepseek.com"->uri.path in listOf("", "/")||uri.path=="/usage"||uri.path.startsWith("/usage/")||listOf("/login","/sign_in","/signin","/auth","/oauth","/callback","/user/login","/user/sign_in","/user/sign_up").any{uri.path==it||uri.path.startsWith("$it/")}
            "login.deepseek.com","accounts.deepseek.com","auth.deepseek.com"->true
            else->false
        }
    }.getOrDefault(false)
}
