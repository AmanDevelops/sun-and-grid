package dev.amandevelops.solax.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.amandevelops.solax.data.Analytics
import dev.amandevelops.solax.data.AppSettings
import dev.amandevelops.solax.data.ConnectionTest
import dev.amandevelops.solax.data.DemoData
import dev.amandevelops.solax.data.HistoryRepository
import dev.amandevelops.solax.data.Reading
import dev.amandevelops.solax.data.SettingsRepository
import dev.amandevelops.solax.data.SolaxClient
import dev.amandevelops.solax.data.SyncWorker
import dev.amandevelops.solax.data.ThemeMode
import dev.amandevelops.solax.data.UppclConnectionTest
import dev.amandevelops.solax.data.UppclErrorKind
import dev.amandevelops.solax.data.UppclException
import dev.amandevelops.solax.data.UppclRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class MonitorStatus {
    LOADING,
    DEMO,
    ONLINE,
    OFFLINE,
    NEEDS_SETUP,
}

data class MonitorUiState(
    val settings: AppSettings = AppSettings(),
    val reading: Reading? = null,
    val history: List<Reading> = emptyList(),
    val status: MonitorStatus = MonitorStatus.LOADING,
    val lastUpdated: Long? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isTesting: Boolean = false,
    val connectionTest: ConnectionTest? = null,
    val uppcl: UppclUiState = UppclUiState(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val historyRepository = HistoryRepository(application)
    private val client = SolaxClient()
    private val uppclRepository = UppclRepository()

    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    private var demoDay: LocalDate = LocalDate.now()
    private var demoHistory: List<Reading> = DemoData.history()
    private var realHistory: List<Reading> = emptyList()
    private var historyLoaded = false
    private var pollingJob: Job? = null
    private var manualJob: Job? = null
    private var isForeground = true
    private val fetchMutex = Mutex()
    private val uppclMutex = Mutex()

    init {
        val now = System.currentTimeMillis()
        _state.value = _state.value.copy(
            reading = DemoData.live(),
            history = demoHistory,
            status = MonitorStatus.DEMO,
            lastUpdated = now,
            uppcl = UppclUiState(),
        )
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings
                .distinctUntilChanged()
                .collect { settings -> applySettings(settings) }
        }
    }

    private suspend fun applySettings(settings: AppSettings) {
        val previous = _state.value.settings
        val connectionChanged = previous.host != settings.host ||
            previous.dongleSerial != settings.dongleSerial ||
            previous.demoMode != settings.demoMode

        val uppclChanged = previous.uppclUsername != settings.uppclUsername ||
            previous.uppclPassword != settings.uppclPassword ||
            previous.uppclAccountNumber != settings.uppclAccountNumber
        val uppclConfigured = settings.isUppclConfigured
        if (!uppclConfigured) {
            viewModelScope.launch { uppclRepository.clear() }
        }

        if (!settings.demoMode && !historyLoaded) {
            realHistory = historyRepository.load()
            historyLoaded = true
        }

        if (connectionChanged) {
            manualJob?.cancel()
            manualJob = null
        }

        val history = if (settings.demoMode) demoHistory else realHistory

        _state.update { current ->
            when {
                settings.demoMode -> current.copy(
                    settings = settings,
                    history = history,
                    reading = DemoData.live(),
                    status = MonitorStatus.DEMO,
                    lastUpdated = System.currentTimeMillis(),
                    error = null,
                    isRefreshing = false,
                )

                !settings.isConfigured -> current.copy(
                    settings = settings,
                    history = history,
                    reading = null,
                    status = MonitorStatus.NEEDS_SETUP,
                    error = null,
                    isRefreshing = false,
                )

                else -> current.copy(
                    settings = settings,
                    history = history,
                    reading = if (previous.demoMode) null else current.reading,
                    status = if (connectionChanged || current.reading == null) {
                        MonitorStatus.LOADING
                    } else {
                        current.status
                    },
                    error = if (previous.demoMode) null else current.error,
                    isRefreshing = if (connectionChanged) false else current.isRefreshing,
                )
            }.withUppclSettings(
                configured = uppclConfigured,
                changed = uppclChanged,
            )
        }

        SyncWorker.sync(getApplication(), settings)
        restartPolling()
        val needsInitialUppcl =
            uppclConfigured &&
                (uppclChanged || _state.value.uppcl.status == UppclStatus.NOT_CONFIGURED)
        if (needsInitialUppcl) {
            viewModelScope.launch { loadUppcl(settings, force = false) }
        }
    }

    private fun MonitorUiState.withUppclSettings(
        configured: Boolean,
        changed: Boolean,
    ): MonitorUiState {
        val next = when {
            !configured -> UppclUiState()

            changed -> uppcl.copy(
                configured = true,
                status = if (uppcl.monthData != null) {
                    UppclStatus.READY
                } else {
                    UppclStatus.LOADING
                },
                error = null,
                authBlocked = false,
                isRefreshing = false,
            )

            else -> uppcl.copy(configured = true)
        }
        return copy(uppcl = next)
    }

    private suspend fun loadUppcl(
        settings: AppSettings,
        force: Boolean,
    ) {
        if (!settings.isUppclConfigured) return

        uppclMutex.withLock {
            _state.update { current ->
                current.copy(
                    uppcl = current.uppcl.copy(
                        configured = true,
                        status = if (current.uppcl.monthData == null) {
                            UppclStatus.LOADING
                        } else {
                            current.uppcl.status
                        },
                        isRefreshing = true,
                        error = null,
                    ),
                )
            }

            val month = YearMonth.now()
            try {
                val data = uppclRepository.month(
                    settings = settings,
                    year = month.year,
                    month = month.monthValue,
                    force = force,
                )
                _state.update { current ->
                    current.copy(
                        uppcl = current.uppcl.copy(
                            configured = true,
                            status = UppclStatus.READY,
                            monthData = data,
                            lastUpdated = data.fetchedAt,
                            error = null,
                            authBlocked = false,
                            isRefreshing = false,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { current ->
                    current.copy(
                        uppcl = current.uppcl.copy(
                            status = UppclStatus.ERROR,
                            error = error.message ?: "Couldn't reach UPPCL.",
                            authBlocked = (error as? UppclException)?.kind ==
                                UppclErrorKind.AUTH,
                            isRefreshing = false,
                        ),
                    )
                }
            }
        }
    }

    private fun restartPolling() {
        pollingJob?.cancel()
        pollingJob = null
        if (!isForeground) return

        val settings = _state.value.settings
        if (settings.demoMode) {
            pollingJob = viewModelScope.launch {
                while (isActive) {
                    delay(4_000)
                    val today = LocalDate.now()
                    if (today != demoDay) {
                        demoDay = today
                        demoHistory = DemoData.history()
                    }
                    _state.update { current ->
                        if (!current.settings.demoMode) {
                            current
                        } else {
                            current.copy(
                                reading = DemoData.live(),
                                history = demoHistory,
                                lastUpdated = System.currentTimeMillis(),
                                status = MonitorStatus.DEMO,
                            )
                        }
                    }
                }
            }
            return
        }

        if (!settings.isConfigured) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchOnce(settings)
                delay(settings.refreshSeconds.coerceIn(10, 300) * 1000L)
            }
        }
    }

    private suspend fun fetchOnce(settings: AppSettings) {
        fetchMutex.withLock {
            try {
                val reading = client.fetch(settings.host, settings.dongleSerial)
                realHistory = historyRepository.append(reading)
                _state.update { current ->
                    current.copy(
                        reading = reading,
                        history = realHistory,
                        status = MonitorStatus.ONLINE,
                        lastUpdated = reading.timestamp,
                        error = null,
                    )
                }
            } catch (error: Exception) {
                _state.update { current ->
                    current.copy(
                        status = MonitorStatus.OFFLINE,
                        error = error.message ?: "Couldn't reach the inverter.",
                    )
                }
            }
        }
    }

    fun refresh() {
        val settings = _state.value.settings
        if (settings.isUppclConfigured) {
            viewModelScope.launch { loadUppcl(settings, force = true) }
        }
        if (settings.demoMode) {
            val today = LocalDate.now()
            if (today != demoDay) {
                demoDay = today
                demoHistory = DemoData.history()
            }
            _state.update {
                it.copy(
                    reading = DemoData.live(),
                    history = demoHistory,
                    lastUpdated = System.currentTimeMillis(),
                    status = MonitorStatus.DEMO,
                    isRefreshing = true,
                )
            }
            viewModelScope.launch {
                delay(500)
                _state.update { it.copy(isRefreshing = false) }
            }
            return
        }

        if (!settings.isConfigured) {
            _state.update { it.copy(status = MonitorStatus.NEEDS_SETUP) }
            return
        }
        if (manualJob?.isActive == true) return

        manualJob = viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val started = System.currentTimeMillis()
            fetchOnce(settings)
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < 450) delay(450 - elapsed)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onForeground() {
        isForeground = true
        viewModelScope.launch {
            if (!_state.value.settings.demoMode) {
                val stored = historyRepository.load()
                realHistory = stored
                historyLoaded = true
                _state.update { it.copy(history = stored) }
            }
            restartPolling()
            val settings = _state.value.settings
            if (settings.isUppclConfigured) {
                loadUppcl(settings, force = false)
            }
        }
    }

    fun onBackground() {
        isForeground = false
        pollingJob?.cancel()
        pollingJob = null
    }

    fun saveConnection(host: String, dongleSerial: String) {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    host = cleanHost,
                    dongleSerial = dongleSerial.trim(),
                    demoMode = false,
                )
            }
            _state.update { it.copy(connectionTest = null) }
        }
    }

    fun testConnection(host: String, dongleSerial: String) {
        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, connectionTest = null) }
            val result = try {
                ConnectionTest.Success(client.fetch(host, dongleSerial))
            } catch (error: Exception) {
                ConnectionTest.Failure(error.message ?: "Couldn't reach the inverter.")
            }
            _state.update { it.copy(isTesting = false, connectionTest = result) }
        }
    }

    fun clearConnectionTest() {
        _state.update { it.copy(connectionTest = null) }
    }

    fun saveUppclConnection(username: String, password: String, accountNumber: String) {
        viewModelScope.launch {
            val probe = _state.value.settings.copy(
                uppclUsername = username.trim(),
                uppclPassword = password,
                uppclAccountNumber = accountNumber.trim(),
            )
            _state.update {
                it.copy(uppcl = it.uppcl.copy(isTesting = true, testResult = null))
            }

            when (val result = uppclRepository.verify(probe)) {
                is UppclConnectionTest.Success -> {
                    settingsRepository.update {
                        it.copy(
                            uppclUsername = probe.uppclUsername,
                            uppclPassword = probe.uppclPassword,
                            uppclAccountNumber = probe.uppclAccountNumber,
                        )
                    }
                    _state.update { current ->
                        current.copy(
                            uppcl = current.uppcl.copy(
                                configured = true,
                                status = UppclStatus.READY,
                                monthData = result.monthData,
                                lastUpdated = result.monthData.fetchedAt,
                                error = null,
                                authBlocked = false,
                                isRefreshing = false,
                                isTesting = false,
                                testResult = result,
                            ),
                        )
                    }
                }

                is UppclConnectionTest.Failure -> {
                    _state.update { current ->
                        val alreadyConfigured = current.uppcl.configured
                        current.copy(
                            uppcl = current.uppcl.copy(
                                isTesting = false,
                                testResult = result,
                                status = if (alreadyConfigured) {
                                    current.uppcl.status
                                } else {
                                    UppclStatus.NOT_CONFIGURED
                                },
                                authBlocked = if (alreadyConfigured) {
                                    current.uppcl.authBlocked
                                } else {
                                    result.kind == UppclErrorKind.AUTH
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    fun testUppclConnection(username: String, password: String, accountNumber: String) {
        viewModelScope.launch {
            val probe = _state.value.settings.copy(
                uppclUsername = username.trim(),
                uppclPassword = password,
                uppclAccountNumber = accountNumber.trim(),
            )
            _state.update {
                it.copy(uppcl = it.uppcl.copy(isTesting = true, testResult = null))
            }
            val result = uppclRepository.verify(probe)
            _state.update {
                it.copy(uppcl = it.uppcl.copy(isTesting = false, testResult = result))
            }
        }
    }

    fun clearUppclConnectionTest() {
        _state.update { it.copy(uppcl = it.uppcl.copy(testResult = null)) }
    }

    fun disconnectUppcl() {
        _state.update { it.copy(uppcl = UppclUiState()) }
        viewModelScope.launch {
            uppclRepository.clear()
            settingsRepository.update {
                it.copy(
                    uppclUsername = "",
                    uppclPassword = "",
                    uppclAccountNumber = "",
                )
            }
        }
    }

    fun refreshUppcl() {
        val settings = _state.value.settings
        if (!settings.isUppclConfigured) return
        viewModelScope.launch { loadUppcl(settings, force = true) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(demoMode = enabled) }
            _state.update { it.copy(connectionTest = null) }
        }
    }

    fun setWeeklyGoal(kwh: Double) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(weeklyGoalKwh = kwh.coerceIn(10.0, 1000.0)) }
        }
    }

    fun setRefreshSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(refreshSeconds = seconds.coerceIn(10, 300)) }
        }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(theme = theme) }
        }
    }

    fun setInvertGridSign(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(invertGridSign = enabled) }
        }
    }

    fun setBackgroundSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(backgroundSync = enabled) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clear()
            realHistory = emptyList()
            historyLoaded = true
            _state.update { current ->
                if (current.settings.demoMode) current else current.copy(history = emptyList())
            }
        }
    }
}
