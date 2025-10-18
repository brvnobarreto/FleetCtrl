package dev.barreto.fleetctrl.utils

import dev.barreto.fleetctrl.data.database.entities.Vehicle
import java.text.Normalizer

/**
 * Simple, robust and scalable text search for vehicles.
 * - Case-insensitive
 * - Diacritics-insensitive
 * - Token-based (all query tokens must match)
 */
object SearchIndex {

    fun vehicleMatches(vehicle: Vehicle, query: String): Boolean {
        if (query.isBlank()) return true
        val haystack = buildVehicleIndex(vehicle)
        val tokens = normalize(query).split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        return tokens.all { token -> haystack.contains(token) }
    }

    fun buildVehicleIndex(vehicle: Vehicle): String {
        val parts = listOfNotNull(
            vehicle.plate,
            vehicle.driver,
            vehicle.model,
            vehicle.brand,
            vehicle.vehicleNumber,
            vehicle.color,
            vehicle.year?.toString()
        )
        return normalize(parts.joinToString(" "))
    }

    private fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = DIACRITICS_REGEX.replace(decomposed, "")
        return withoutDiacritics
            .lowercase()
            .replace(NON_ALNUM_REGEX, " ")
            .replace(WHITESPACE_MULTIPLE_REGEX, " ")
            .trim()
    }

    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val NON_ALNUM_REGEX = "[^a-z0-9]+".toRegex()
    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private val WHITESPACE_MULTIPLE_REGEX = "\\s{2,}".toRegex()
}
