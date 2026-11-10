package dev.amandevelops.solax.data

import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Minimal client for the SolaX Pocket Wi-Fi local API.
 *
 * The dongle expects a form POST with `optType=ReadRealTimeData`, the dongle
 * serial as `pwd`, and the X-Forwarded-For bypass described by the Python wrapper
 * (CVE-2023-35835). It is plain HTTP on the local network.
 */
class SolaxClient(
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun fetch(host: String, dongleSerial: String): Reading = withContext(Dispatchers.IO) {
        val url = buildUrl(host)
        val password = dongleSerial.trim()
        if (password.isEmpty()) {
            throw SolaxException("Enter your dongle serial number first.")
        }

        val body = FormBody.Builder()
            .add("optType", "ReadRealTimeData")
            .add("pwd", password)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("X-Forwarded-For", "5.8.8.8")
            .header("Accept-Encoding", "identity")
            .header("User-Agent", "SolaX-Monitor/1.0")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw SolaxException(
                        "The inverter answered with HTTP ${response.code}. Check the IP and dongle password.",
                    )
                }
                val payload = response.body?.string().orEmpty()
                if (payload.isBlank()) {
                    throw SolaxException("The inverter returned an empty response.")
                }
                SolaxParser.parse(payload)
            }
        } catch (error: SolaxException) {
            throw error
        } catch (error: InterruptedIOException) {
            throw SolaxException(
                "The inverter timed out. Make sure your phone is on the same Wi-Fi as the dongle.",
                error,
            )
        } catch (error: SocketTimeoutException) {
            throw SolaxException("The inverter timed out. Move closer to your Wi-Fi router and retry.", error)
        } catch (error: UnknownHostException) {
            throw SolaxException("Couldn't find that address. Check the inverter IP.", error)
        } catch (error: ConnectException) {
            throw SolaxException("The connection was refused. Check the inverter IP and that it is powered on.", error)
        } catch (error: Exception) {
            throw SolaxException(
                "Couldn't reach the inverter. Check that you're on its Wi-Fi network.",
                error,
            )
        }
    }

    private fun buildUrl(host: String): String {
        val trimmed = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) {
            throw SolaxException("Enter a valid inverter IP address.")
        }
        return "http://$trimmed/"
    }

    private companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }
}
