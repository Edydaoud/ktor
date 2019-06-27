/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlin.test.*

class PostTest : ClientLoader() {
    @Test
    fun testPostString() {
        postHelper(makeString(777))
    }

    @Test
    fun testHugePost() {
        postHelper(makeString(32 * 1024 * 1024))
    }

    @Test
    fun testWithPause() = clientTests {
        test { client ->
            val content = makeString(32 * 1024 * 1024)

            val response = client.post<String>("$TEST_SERVER/content/echo") {
                body = object : OutgoingContent.WriteChannelContent() {
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        channel.writeStringUtf8(content)
                        delay(1000)
                        channel.writeStringUtf8(content)
                        channel.close()
                    }
                }

            }

            assertEquals(content + content, response)
        }
    }

    private fun postHelper(text: String) = clientTests {
        test { client ->
            val response = client.post<String>("$TEST_SERVER/content/echo") {
                body = text
            }
            assertEquals(text, response)
        }
    }
}
