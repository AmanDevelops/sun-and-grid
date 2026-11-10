package dev.amandevelops.solax.data

import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class UppclHttpResponse(
    val code: Int,
    val body: String,
)

/**
 * Small seam around HTTP so the UPPCL client can be unit tested without a server.
 */
interface UppclTransport {
    suspend fun post(url: String, body: String, token: String?): UppclHttpResponse
}

class OkHttpUppclTransport(
    private val client: OkHttpClient = defaultClient(),
) : UppclTransport {
    override suspend fun post(url: String, body: String, token: String?): UppclHttpResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .apply { if (token != null) header("token", token) }
                .post(body.toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                UppclHttpResponse(
                    code = response.code,
                    body = response.body?.string().orEmpty(),
                )
            }
        }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

/**
 * Client for the UPPCL Smart Consumers API.
 *
 * Verified flow (see the reverse-engineering notes):
 * - `POST /users/login` with `user_name` and a Base64-encoded `password`
 * - the returned JWT goes in the `token` header, verbatim
 * - `POST /users/dailyGetData` with `account_number`, `userId`, `year`, `month`
 */
class UppclClient(
    private val transport: UppclTransport = OkHttpUppclTransport(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun login(username: String, password: String): UppclSession {
        val user = username.trim()
        if (user.isEmpty() || password.isEmpty()) {
            throw UppclException(
                "Enter your UPPCL username and password.",
                UppclErrorKind.INPUT,
            )
        }

        val body = buildJsonObject {
            put("user_name", user)
            put("password", base64(password))
        }.toString()

        val envelope = request("/users/login", body, token = null, signingIn = true)
        val token = envelope.token?.trim().orEmpty()
        if (token.isEmpty()) {
            throw UppclException(
                "UPPCL signed in but did not return a token.",
                UppclErrorKind.PARSE,
            )
        }

        return UppclJwt.decode(token)
    }

    suspend fun dailyConsumption(
        session: UppclSession,
        accountNumber: String,
        year: Int,
        month: Int,
    ): UppclMonthData {
        val account = accountNumber.trim()
        if (account.isEmpty()) {
            throw UppclException(
                "Enter your UPPCL account number.",
                UppclErrorKind.INPUT,
            )
        }
        require(month in 1..12) { "month must be between 1 and 12" }

        val body = buildJsonObject {
            put("account_number", account)
            put("userId", session.userId)
            put("year", year)
            put("month", month)
        }.toString()

        val envelope = request("/users/dailyGetData", body, session.token, signingIn = false)
        val days = envelope.dataParams.toDays()

        return UppclMonthData(
            year = year,
            month = month,
            days = days.sortedBy { it.day },
            fetchedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun request(
        path: String,
        body: String,
        token: String?,
        signingIn: Boolean,
    ): UppclEnvelope {
        val response = try {
            transport.post("$BASE$path", body, token)
        } catch (error: IOException) {
            throw UppclException(
                "Couldn't reach UPPCL. Check your internet connection.",
                UppclErrorKind.NETWORK,
                error,
            )
        }

        val envelope = runCatching {
            json.decodeFromString<UppclEnvelope>(response.body)
        }.getOrNull()

        when {
            response.code == 401 -> throw UppclException(
                envelope?.msg?.takeIf { it.isNotBlank() }
                    ?: "Your UPPCL session expired. Sign in again.",
                UppclErrorKind.AUTH,
            )

            response.code !in 200..299 -> throw UppclException(
                envelope?.msg?.takeIf { it.isNotBlank() }
                    ?: "UPPCL returned HTTP ${response.code}.",
                UppclErrorKind.API,
            )

            envelope == null -> throw UppclException(
                "UPPCL sent a response the app could not read.",
                UppclErrorKind.PARSE,
            )
        }

        if (envelope.authCode != "200") {
            val message = envelope.msg.takeIf { it.isNotBlank() }
                ?: if (signingIn) {
                    "UPPCL sign-in failed."
                } else {
                    "UPPCL couldn't return that data."
                }
            throw UppclException(
                message,
                if (signingIn || message.looksLikeAuthFailure()) {
                    UppclErrorKind.AUTH
                } else {
                    UppclErrorKind.API
                },
            )
        }

        return envelope
    }

    private fun kotlinx.serialization.json.JsonElement?.toDays(): List<UppclDayConsumption> {
        val array = this as? JsonArray ?: throw UppclException(
            "UPPCL did not return daily consumption data.",
            UppclErrorKind.PARSE,
        )
        return runCatching {
            json.decodeFromJsonElement<List<UppclDayConsumption>>(array)
        }.getOrElse {
            throw UppclException(
                "UPPCL returned consumption data in an unexpected format.",
                UppclErrorKind.PARSE,
                it,
            )
        }
    }

    private fun base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    companion object {
        const val BASE = "https://uppcl.eeslsmartmeter.in/api"

        private fun String.looksLikeAuthFailure(): Boolean {
            val lower = lowercase()
            return AUTH_HINTS.any { it in lower }
        }

        private val AUTH_HINTS = listOf(
            "token",
            "session",
            "sign in",
            "sign-in",
            "login",
            "log in",
            "expired",
            "unauthor",
        )
    }
}

/**
 * The UPPCL token is a normal three-part JWT. The signature is not needed by
 * this app; we only read the claims the portal generates client-side.
 */
internal object UppclJwt {
    fun decode(token: String): UppclSession {
        val parts = token.split(".")
        if (parts.size < 2) {
            throw UppclException(
                "UPPCL returned an invalid sign-in token.",
                UppclErrorKind.PARSE,
            )
        }

        val payload = runCatching {
            val normalized = parts[1].replace('-', '+').replace('_', '/')
            val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
            String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
        }.getOrElse {
            throw UppclException(
                "UPPCL returned an invalid sign-in token.",
                UppclErrorKind.PARSE,
                it,
            )
        }

        val claims = runCatching {
            Json.parseToJsonElement(payload).jsonObject
        }.getOrElse {
            throw UppclException(
                "UPPCL returned an unreadable sign-in token.",
                UppclErrorKind.PARSE,
                it,
            )
        }

        fun claim(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
            (claims[name] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }

        val userId = claim("userId", "user_id", "id", "sub")
            ?: throw UppclException(
                "UPPCL signed in but the token has no user id.",
                UppclErrorKind.PARSE,
            )

        return UppclSession(
            token = token,
            userId = userId,
            username = claim("username", "user_name", "name"),
            numberOfAccounts = claim("numberOfAccounts")?.toIntOrNull(),
            expiresAtMillis = claim("exp")?.toLongOrNull()?.times(1000L),
        )
    }
}
