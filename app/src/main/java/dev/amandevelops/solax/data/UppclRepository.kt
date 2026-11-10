package dev.amandevelops.solax.data

import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caches UPPCL sessions and monthly consumption.
 *
 * UPPCL updates smart-meter readings every 15–30 minutes, so holding a month in
 * memory for a few minutes avoids hitting the portal on every screen change.
 * Credentials never leave the app except in the login request.
 */
class UppclRepository(
    private val client: UppclClient = UppclClient(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class CachedMonth(
        val data: UppclMonthData,
        val storedAt: Long,
    )

    private val mutex = Mutex()
    private val months = mutableMapOf<String, CachedMonth>()
    private var session: UppclSession? = null
    private var sessionKey: String? = null

    /**
     * Returns one month of daily consumption, using the in-memory cache unless
     * [force] is true. If the cached JWT has expired, it signs in again once.
     */
    suspend fun month(
        settings: AppSettings,
        year: Int,
        month: Int,
        force: Boolean = false,
    ): UppclMonthData = mutex.withLock {
        val key = cacheKey(settings, year, month)
        val cached = months[key]
        if (!force && cached != null && now() - cached.storedAt < CACHE_TTL_MS) {
            return@withLock cached.data
        }

        val data = fetch(settings, year, month)
        months[key] = CachedMonth(data, now())
        return@withLock data
    }

    /**
     * Performs a real sign-in and reads the current month. Used by the Settings
     * screen's Test button and by Save, so bad credentials are never persisted.
     */
    suspend fun verify(settings: AppSettings): UppclConnectionTest = mutex.withLock {
        try {
            val fresh = client.login(settings.uppclUsername, settings.uppclPassword)
            val current = YearMonth.now()
            val data = client.dailyConsumption(
                fresh,
                settings.uppclAccountNumber,
                current.year,
                current.monthValue,
            )

            session = fresh
            sessionKey = credentialsKey(settings)
            months[cacheKey(settings, current.year, current.monthValue)] =
                CachedMonth(data, now())

            UppclConnectionTest.Success(
                username = fresh.username ?: settings.uppclUsername.trim(),
                numberOfAccounts = fresh.numberOfAccounts,
                monthData = data,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            UppclConnectionTest.Failure(
                message = error.message ?: "Couldn't connect to UPPCL.",
                kind = (error as? UppclException)?.kind ?: UppclErrorKind.NETWORK,
            )
        }
    }

    suspend fun clear() {
        mutex.withLock {
            months.clear()
            session = null
            sessionKey = null
        }
    }

    private suspend fun fetch(
        settings: AppSettings,
        year: Int,
        month: Int,
    ): UppclMonthData {
        var active = ensuredSession(settings)

        return try {
            client.dailyConsumption(
                active,
                settings.uppclAccountNumber,
                year,
                month,
            )
        } catch (error: UppclException) {
            if (error.kind != UppclErrorKind.AUTH) throw error

            // The JWT expired or was rejected. Sign in once more and retry.
            session = null
            sessionKey = null
            active = ensuredSession(settings)
            client.dailyConsumption(
                active,
                settings.uppclAccountNumber,
                year,
                month,
            )
        }
    }

    private suspend fun ensuredSession(settings: AppSettings): UppclSession {
        val key = credentialsKey(settings)
        val cached = session
        if (cached != null && sessionKey == key && !cached.isExpired(now())) {
            return cached
        }

        val fresh = client.login(settings.uppclUsername, settings.uppclPassword)
        session = fresh
        sessionKey = key
        return fresh
    }

    private fun credentialsKey(settings: AppSettings): String =
        "${settings.uppclUsername.trim()}|${settings.uppclPassword.hashCode()}"

    private fun cacheKey(settings: AppSettings, year: Int, month: Int): String =
        "${settings.uppclUsername.trim()}|${settings.uppclAccountNumber.trim()}|$year-$month"

    private companion object {
        const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}
