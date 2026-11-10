package dev.amandevelops.solax.data

sealed interface ConnectionTest {
    data class Success(val reading: Reading) : ConnectionTest
    data class Failure(val message: String) : ConnectionTest
}
