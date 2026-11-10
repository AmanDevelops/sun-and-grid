package dev.amandevelops.solax.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * One day of consumption returned by the UPPCL Smart Consumers
 * `/users/dailyGetData` endpoint. The portal returns every day of the requested
 * month, including future days with a consumption of zero.
 */
@Serializable
data class UppclDayConsumption(
    @Serializable(with = LenientIntSerializer::class)
    val day: Int,
    @Serializable(with = LenientDoubleSerializer::class)
    val consumption: Double = 0.0,
)

/**
 * One month of UPPCL meter readings.
 */
data class UppclMonthData(
    val year: Int,
    val month: Int,
    val days: List<UppclDayConsumption>,
    val fetchedAt: Long,
) {
    val totalKwh: Double get() = days.sumOf { it.consumption }

    fun consumptionOn(day: Int): Double? =
        days.firstOrNull { it.day == day }?.consumption
}

/**
 * The common response envelope used by every UPPCL endpoint.
 */
@Serializable
internal data class UppclEnvelope(
    @Serializable(with = LenientStringSerializer::class)
    val authCode: String = "",
    @Serializable(with = LenientStringSerializer::class)
    val msg: String = "",
    val token: String? = null,
    @SerialName("data_params")
    val dataParams: kotlinx.serialization.json.JsonElement? = null,
)

/**
 * The verified session decoded from the JWT returned by `/users/login`.
 */
data class UppclSession(
    val token: String,
    val userId: String,
    val username: String? = null,
    val numberOfAccounts: Int? = null,
    val expiresAtMillis: Long? = null,
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAtMillis != null && expiresAtMillis <= nowMillis + 60_000L
}

sealed interface UppclConnectionTest {
    data class Success(
        val username: String?,
        val numberOfAccounts: Int?,
        val monthData: UppclMonthData,
    ) : UppclConnectionTest

    data class Failure(
        val message: String,
        val kind: UppclErrorKind = UppclErrorKind.API,
    ) : UppclConnectionTest
}

enum class UppclErrorKind {
    /** Wrong username/password or an expired/missing token. */
    AUTH,

    /** The server accepted the request but rejected it with a business error. */
    API,

    /** The request never reached UPPCL or UPPCL was unreachable. */
    NETWORK,

    /** UPPCL returned something this app could not parse. */
    PARSE,

    /** The user did not fill in a required field. */
    INPUT,
}

class UppclException(
    message: String,
    val kind: UppclErrorKind = UppclErrorKind.API,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * UPPCL sometimes returns numbers or messages as strings and messages as arrays.
 */
internal object LenientStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UppclLenientString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content
            is JsonArray -> element.joinToString(" ") { (it as? JsonPrimitive)?.content.orEmpty() }
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

internal object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UppclLenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return 0
        return element.content.trim().toIntOrNull() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

internal object LenientDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UppclLenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        val element = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return 0.0
        val text = element.content.trim()
        val normalized = if (text.contains(',') && !text.contains('.')) {
            text.replace(',', '.')
        } else {
            text.replace(",", "")
        }
        return normalized.toDoubleOrNull() ?: 0.0
    }

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}
