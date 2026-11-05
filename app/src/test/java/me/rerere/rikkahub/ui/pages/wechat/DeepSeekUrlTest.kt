package me.rerere.rikkahub.ui.pages.wechat

import org.junit.Assert.*
import org.junit.Test

class DeepSeekUrlTest {
    @Test fun usagePageCannotBecomeAnArbitraryBrowser(){
        assertTrue(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com/usage"))
        assertTrue(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com/sign_in?redirect=%2Fusage"))
        assertTrue(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com/"))
        assertTrue(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com/user/login?redirect=%2Fusage"))
        assertFalse(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com/loginevil"))
        assertFalse(DeepSeekBalanceApi.allowedPage("https://example.com/usage"))
        assertFalse(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com.attacker.test/usage"))
        assertFalse(DeepSeekBalanceApi.allowedPage("http://platform.deepseek.com/usage"))
        assertFalse(DeepSeekBalanceApi.allowedPage("https://platform.deepseek.com/api_keys"))
        assertFalse(DeepSeekBalanceApi.allowedPage("file:///storage/emulated/0/test.html"))
        assertFalse(DeepSeekBalanceApi.allowedPage("https://user:secret@platform.deepseek.com/usage"))
    }
}
