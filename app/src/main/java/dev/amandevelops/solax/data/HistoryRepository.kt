package dev.amandevelops.solax.data

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class HistoryFile(
    val samples: List<Reading> = emptyList(),
)

/**
 * A small JSON-backed sample log. It intentionally avoids Room: the app only
 * needs a few thousand light samples, and this keeps the APK lean.
 */
class HistoryRepository(context: Context) {
    private val file = File(context.filesDir, "history.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mutex = Mutex()
    private var cache: MutableList<Reading>? = null

    suspend fun load(): List<Reading> = withContext(Dispatchers.IO) {
        mutex.withLock { readCache().toList() }
    }

    suspend fun append(reading: Reading): List<Reading> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val samples = readCache()
            val last = samples.lastOrNull()
            val newDay = last == null || localDate(last.timestamp) != localDate(reading.timestamp)

            if (last == null || newDay || reading.timestamp - last.timestamp >= MIN_GAP_MS) {
                samples.add(reading)
                prune(samples, reading.timestamp)
                write(samples)
            }
            samples.toList()
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache = mutableListOf()
            if (file.exists()) file.delete()
        }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        mutex.withLock { readCache().size }
    }

    private fun readCache(): MutableList<Reading> {
        cache?.let { return it }

        val loaded = runCatching {
            if (!file.exists()) return@runCatching mutableListOf<Reading>()
            val content = file.readText()
            if (content.isBlank()) return@runCatching mutableListOf<Reading>()
            json.decodeFromString<HistoryFile>(content).samples
                .sortedBy { it.timestamp }
                .toMutableList()
        }.getOrElse {
            mutableListOf()
        }

        prune(loaded, System.currentTimeMillis())
        cache = loaded
        return loaded
    }

    private fun prune(samples: MutableList<Reading>, now: Long) {
        val cutoff = now - RETENTION_MS
        samples.removeAll { it.timestamp < cutoff }
        if (samples.size > MAX_SAMPLES) {
            samples.subList(0, samples.size - MAX_SAMPLES).clear()
        }
    }

    private fun write(samples: List<Reading>) {
        runCatching {
            val temp = File(file.parentFile, "history.json.tmp")
            temp.writeText(json.encodeToString(HistoryFile(samples)))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    private fun localDate(timestamp: Long): java.time.LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        private const val MIN_GAP_MS = 2 * 60 * 1000L
        private const val RETENTION_MS = 75L * 24 * 60 * 60 * 1000
        private const val MAX_SAMPLES = 6000
    }
}
