package dev.barreto.fleetctrl.data.models

import java.util.Date

data class UserOrganization(
    val id: String = "",
    val userId: String = "",
    val organizationId: String = "",
    val role: String = "viewer", // owner, editor, viewer
    val joinedAt: Date = Date(),
    val userEmail: String = "" // Email do usuário
)