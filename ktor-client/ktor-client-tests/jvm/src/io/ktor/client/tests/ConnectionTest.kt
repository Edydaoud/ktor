/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.response.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlin.test.*

@Suppress("KDocMissingDocumentation")
abstract class ConnectionTest(val factory: HttpClientEngineFactory<*>) : TestWithKtor() {
    private val testContent = buildString {
        append("x".repeat(100))
    }

    override val server: ApplicationEngine = embeddedServer(Jetty, port = serverPort) {
        routing {
            head("/emptyHead") {
                call.respond(object : OutgoingContent.NoContent() {
                    override val contentLength: Long = 150
                })
            }
            get("/ok") {
                call.respondText(testContent)
            }
        }
    }

    @Test
    fun testContentLengthWithEmptyBody() = clientTest(factory) {
        test { client ->
            repeat(10) {
                val response = client.call {
                    url {
                        method = HttpMethod.Head
                        port = serverPort
                        encodedPath = "/emptyHead"
                    }
                }.response

                response.use {
                    assert(it.status.isSuccess())
                    assert(it.readBytes().isEmpty())
                }
            }
        }
    }

    @Test
    fun testCloseResponseWithConnectionPipeline() = clientTest(factory) {
        suspend fun HttpClient.testCall(): HttpClientCall = call {
            url {
                port = serverPort
                encodedPath = "/ok"
            }
        }

        test { client ->
            client.testCall().close()
            assertEquals(testContent, client.testCall().receive())
        }
    }
}
