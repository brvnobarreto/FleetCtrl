package dev.barreto.fleetctrl.navigation

import androidx.annotation.StringRes
import dev.barreto.fleetctrl.R

enum class Screen(
    val route: String,
    @StringRes val titleRes: Int
) {
    HOME("home", R.string.title_home),
    DIARY("diary", R.string.title_diary),
    FUEL("fuel", R.string.title_fuel),
    MAINTENANCE("maintenance", R.string.title_maintenance),
    FLEET("fleet", R.string.title_fleet),
    PROFILE("profile", R.string.title_profile),
    SETTINGS("settings", R.string.title_settings),
    THEME("theme", R.string.title_theme),
    CONNECTIVITY("connectivity", R.string.connectivity_title),
    DATA_MANAGEMENT("data_management", R.string.settings_data_title),
    NOTIFICATIONS("notifications", R.string.notifications_title)
}
