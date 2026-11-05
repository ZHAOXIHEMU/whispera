package me.rerere.rikkahub.data.sync

import me.rerere.rikkahub.data.remote.UpstreamNetworkPolicy
import org.junit.Assert.*
import org.junit.Test

class UpstreamNetworkPolicyTest {
    @Test fun upstreamHostsAreBlockedWhileUserProviderHostsRemainAvailable() {
        listOf("api.rikka-ai.com", "sponsors.rikka-ai.com", "rikkahub.luoca.cn", "ai.ota1245.top", "API.RIKKA-AI.COM.").forEach {
            assertTrue(it, UpstreamNetworkPolicy.blocked(it))
        }
        listOf("api.deepseek.com", "platform.deepseek.com", "api.openai.com", "localhost", "192.168.1.10", "example.com").forEach {
            assertFalse(it, UpstreamNetworkPolicy.blocked(it))
        }
    }
}
