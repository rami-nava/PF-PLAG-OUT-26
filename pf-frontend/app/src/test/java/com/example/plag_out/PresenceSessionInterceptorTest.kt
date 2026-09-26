package com.example.plag_out

import com.example.plag_out.Service.presenceSessionInterceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PresenceSessionInterceptorTest {
    @Test fun `account switch before dispatch never sends queued payload`() {
        var dispatched = false
        val client = OkHttpClient.Builder()
            .addInterceptor(presenceSessionInterceptor("A") { "B" to "token-b" })
            .addInterceptor { chain ->
                dispatched = true
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("".toResponseBody()).build()
            }.build()
        try {
            client.newCall(Request.Builder().url("https://example.invalid/biofix").build()).execute().close()
            fail("Session mismatch must block dispatch")
        } catch (_: IOException) { }
        assertFalse(dispatched)
    }

    @Test fun `dispatch uses one immutable session snapshot`() {
        var reads = 0
        var authorization: String? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(presenceSessionInterceptor("A") {
                reads++
                if (reads == 1) "A" to "token-a" else "B" to "token-b"
            })
            .addInterceptor { chain ->
                authorization = chain.request().header("Authorization")
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("".toResponseBody()).build()
            }.build()
        client.newCall(Request.Builder().url("https://example.invalid/biofix").build()).execute().close()
        assertEquals(1, reads)
        assertEquals("Bearer token-a", authorization)
    }
}
