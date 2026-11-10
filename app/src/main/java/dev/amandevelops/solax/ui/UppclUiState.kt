package dev.amandevelops.solax.ui

import dev.amandevelops.solax.data.UppclConnectionTest
import dev.amandevelops.solax.data.UppclMonthData

enum class UppclStatus {
    NOT_CONFIGURED,
    LOADING,
    READY,
    ERROR,
}

data class UppclUiState(
    val configured: Boolean = false,
    val status: UppclStatus = UppclStatus.NOT_CONFIGURED,
    val monthData: UppclMonthData? = null,
    val lastUpdated: Long? = null,
    val error: String? = null,
    val authBlocked: Boolean = false,
    val isRefreshing: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: UppclConnectionTest? = null,
)
