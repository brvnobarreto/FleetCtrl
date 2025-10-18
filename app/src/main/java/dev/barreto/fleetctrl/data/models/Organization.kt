package dev.barreto.fleetctrl.data.models

import java.util.Date

data class Organization(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val code: String = "",
    val ownerId: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)