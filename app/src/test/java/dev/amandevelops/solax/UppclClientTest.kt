package dev.amandevelops.solax

import dev.amandevelops.solax.data.AppSettings
import dev.amandevelops.solax.data.UppclClient
import dev.amandevelops.solax.data.UppclErrorKind
import dev.amandevelops.solax.data.UppclException
import dev.amandevelops.solax.data.UppclHttpResponse
import dev.amandevelops.solax.data.UppclRepository
import dev.amandevelops.solax.data.UppclTransport
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UppclClientTest {

    private class FakeTransport(
        vararg responses: UppclHttpResponse,
    ) : UppclTransport {
        data class Call(val url: String, val body: String, val token: String?)

        private val queue = ArrayDeque(responses.toList())
        val calls = mutableListOf<Call>()

        override suspend fun post(url: String, body: String, token: String?): UppclHttpResponse {
            calls += Call(url, body, token)
            return queue.removeFirst()
        }
    }

    private class FailingTransport(private val error: IOException) : UppclTransport {
        override suspend fun post(url: String, body: String, token: String?): UppclHttpResponse =
            throw error
    }

    private fun base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        fun part(value: String) = encoder.encodeToString(value.toByteArray(Charsets.UTF_8))
        return "${part("""{"alg":"HS256","typ":"JWT"}""")}.${part(payload)}.signature"
    }

    private fun loginResponse(
        payload: String = """{"userId":"16055","username":"alice","numberOfAccounts":2}""",
    ) = UppclHttpResponse(
        code = 200,
        body = """{"authCode":"200","status":true,"token":"${jwt(payload)}","msg":"success"}""",
    )

    private fun dailyResponse(body: String = DAILY_BODY) = UppclHttpResponse(200, body)

    private fun expectError(kind: UppclErrorKind, block: suspend () -> Unit): UppclException {
        val error = runCatching { runBlocking { block() } }.exceptionOrNull()
        assertTrue("Expected UppclException, got $error", error is UppclException)
        assertEquals(kind, (error as UppclException).kind)
        return error
    }

    @Test
    fun `login sends the Base64 password and decodes the JWT claims`() = runBlocking {
        val transport = FakeTransport(
            loginResponse(
                """{"userId":"16055","username":"alice","numberOfAccounts":2,"exp":4102444800}""",
            ),
        )
        val session = UppclClient(transport).login(" alice ", "s3cret")

        assertEquals("16055", session.userId)
        assertEquals("alice", session.username)
        assertEquals(2, session.numberOfAccounts)
        assertEquals(4102444800000L, session.expiresAtMillis)

        val call = transport.calls.single()
        assertEquals("https://uppcl.eeslsmartmeter.in/api/users/login", call.url)
        assertNull(call.token)
        val body = Json.parseToJsonElement(call.body).jsonObject
        assertEquals("alice", body.getValue("user_name").jsonPrimitive.content)
        assertEquals(base64("s3cret"), body.getValue("password").jsonPrimitive.content)
    }

    @Test
    fun `daily usage sends the raw JWT and reads every day of the month`() = runBlocking {
        val token = jwt("""{"userId":"16055"}""")
        val transport = FakeTransport(
            UppclHttpResponse(
                200,
                """{"authCode":"200","status":true,"token":"$token","msg":"success"}""",
            ),
            dailyResponse(),
        )
        val client = UppclClient(transport)
        val session = client.login("alice", "s3cret")

        val data = client.dailyConsumption(session, "1234567890", 2026, 11)

        assertEquals(2026, data.year)
        assertEquals(11, data.month)
        assertEquals((1..30).toList(), data.days.map { it.day })
        assertEquals(4.13 + 4.25 + 4.12 + 5.14 + 4.83 + 3.63 + 5.71 + 6.77, data.totalKwh, 0.0001)
        assertEquals(4.25, data.consumptionOn(2)!!, 0.0001)
        assertEquals(0.0, data.consumptionOn(9)!!, 0.0)
        assertNull(data.consumptionOn(31))

        val call = transport.calls.last()
        assertEquals("https://uppcl.eeslsmartmeter.in/api/users/dailyGetData", call.url)
        assertEquals(token, call.token)

        val body = Json.parseToJsonElement(call.body).jsonObject
        assertEquals("1234567890", body.getValue("account_number").jsonPrimitive.content)
        assertEquals("16055", body.getValue("userId").jsonPrimitive.content)
        assertEquals("2026", body.getValue("year").jsonPrimitive.content)
        assertEquals("11", body.getValue("month").jsonPrimitive.content)
    }

    @Test
    fun `daily usage accepts numbers sent as strings`() = runBlocking {
        val transport = FakeTransport(
            loginResponse(),
            dailyResponse(
                """{"authCode":"200","status":true,"msg":"ok","data_params":[
                    {"day":2,"consumption":"4,25","extra":"ignored"},
                    {"day":"1","consumption":"4.13"}
                ]}""",
            ),
        )
        val client = UppclClient(transport)
        val session = client.login("alice", "s3cret")

        val data = client.dailyConsumption(session, "1234567890", 2026, 11)

        assertEquals(listOf(1, 2), data.days.map { it.day })
        assertEquals(4.13, data.consumptionOn(1)!!, 0.0001)
        assertEquals(4.25, data.consumptionOn(2)!!, 0.0001)
    }

    @Test
    fun `rejected login reports an auth error`() {
        val transport = FakeTransport(
            UppclHttpResponse(
                200,
                """{"authCode":"100","status":0,"data":[],"msg":"Invalid user name or password"}""",
            ),
        )
        val error = expectError(UppclErrorKind.AUTH) {
            UppclClient(transport).login("alice", "wrong")
        }
        assertEquals("Invalid user name or password", error.message)
    }

    @Test
    fun `missing token on a daily request reports an auth error`() {
        val transport = FakeTransport(
            loginResponse(),
            UppclHttpResponse(
                401,
                """{"authCode":"100","status":0,"data":[],"msg":"Please send a token"}""",
            ),
        )
        expectError(UppclErrorKind.AUTH) {
            val client = UppclClient(transport)
            val session = client.login("alice", "s3cret")
            client.dailyConsumption(session, "1234567890", 2026, 11)
        }
    }

    @Test
    fun `an expired business response with a token message reports an auth error`() {
        val transport = FakeTransport(
            loginResponse(),
            UppclHttpResponse(
                200,
                """{"authCode":"100","status":0,"msg":["Token expired, please log in again"]}""",
            ),
        )
        val error = expectError(UppclErrorKind.AUTH) {
            val client = UppclClient(transport)
            val session = client.login("alice", "s3cret")
            client.dailyConsumption(session, "1234567890", 2026, 11)
        }
        assertEquals("Token expired, please log in again", error.message)
    }

    @Test
    fun `server failures and unreadable bodies are reported clearly`() {
        expectError(UppclErrorKind.API) {
            UppclClient(FakeTransport(UppclHttpResponse(500, "oops"))).login("alice", "s3cret")
        }
        expectError(UppclErrorKind.PARSE) {
            UppclClient(FakeTransport(UppclHttpResponse(200, "not json"))).login("alice", "s3cret")
        }
        expectError(UppclErrorKind.PARSE) {
            UppclClient(FakeTransport(loginResponse("""{"username":"alice"}"""))).login("alice", "s3cret")
        }
        expectError(UppclErrorKind.NETWORK) {
            UppclClient(FailingTransport(IOException("offline"))).login("alice", "s3cret")
        }
    }

    @Test
    fun `repository signs in once and reuses the cached month`() = runBlocking {
        val transport = FakeTransport(
            loginResponse(),
            dailyResponse(),
            dailyResponse(),
        )
        val repository = UppclRepository(UppclClient(transport))
        val settings = AppSettings(
            uppclUsername = "alice",
            uppclPassword = "s3cret",
            uppclAccountNumber = "1234567890",
        )

        repository.month(settings, 2026, 11)
        repository.month(settings, 2026, 11)
        assertEquals(2, transport.calls.size)
        assertEquals("login", transport.calls[0].url.substringAfterLast("/"))

        repository.month(settings, 2026, 11, force = true)
        assertEquals(3, transport.calls.size)
        assertEquals("dailyGetData", transport.calls[2].url.substringAfterLast("/"))
        assertTrue(transport.calls[2].token != null)
    }

    @Test
    fun `repository signs in again after a rejected token`() = runBlocking {
        val transport = FakeTransport(
            loginResponse(),
            UppclHttpResponse(401, """{"authCode":"100","msg":"Please send a token"}"""),
            loginResponse(),
            dailyResponse(),
        )
        val repository = UppclRepository(UppclClient(transport))
        val settings = AppSettings(
            uppclUsername = "alice",
            uppclPassword = "s3cret",
            uppclAccountNumber = "1234567890",
        )

        val data = repository.month(settings, 2026, 11)

        assertEquals(30, data.days.size)
        assertEquals(4, transport.calls.size)
        assertEquals("login", transport.calls[2].url.substringAfterLast("/"))
    }

    private companion object {
        val DAILY_BODY = """
            {"authCode":"200","status":true,"msg":"Data listed successfully","data_params":[
                {"day":3,"consumption":4.12},
                {"day":1,"consumption":4.13},
                {"day":2,"consumption":4.25},
                {"day":4,"consumption":5.14},
                {"day":5,"consumption":4.83},
                {"day":6,"consumption":3.63},
                {"day":7,"consumption":5.71},
                {"day":8,"consumption":6.77},
                {"day":9,"consumption":0},
                {"day":10,"consumption":0},
                {"day":11,"consumption":0},
                {"day":12,"consumption":0},
                {"day":13,"consumption":0},
                {"day":14,"consumption":0},
                {"day":15,"consumption":0},
                {"day":16,"consumption":0},
                {"day":17,"consumption":0},
                {"day":18,"consumption":0},
                {"day":19,"consumption":0},
                {"day":20,"consumption":0},
                {"day":21,"consumption":0},
                {"day":22,"consumption":0},
                {"day":23,"consumption":0},
                {"day":24,"consumption":0},
                {"day":25,"consumption":0},
                {"day":26,"consumption":0},
                {"day":27,"consumption":0},
                {"day":28,"consumption":0},
                {"day":29,"consumption":0},
                {"day":30,"consumption":0}
            ]}
        """.trimIndent()
    }
}
