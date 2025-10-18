package dev.barreto.fleetctrl.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Chaves das preferências
    private object PreferencesKeys {
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val SELECTED_PALETTE = stringPreferencesKey("selected_palette")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val CURRENT_SCREEN = stringPreferencesKey("current_screen")
        val IS_PROFILE_MENU_EXPANDED = booleanPreferencesKey("is_profile_menu_expanded")
        val UPLOAD_LOCAL_TO_CLOUD = booleanPreferencesKey("upload_local_to_cloud")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
        val ALLOW_MOBILE_DATA = booleanPreferencesKey("allow_mobile_data")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val UPLOAD_QUALITY = stringPreferencesKey("upload_quality")
        val SELECTED_ORG_ID = stringPreferencesKey("selected_org_id")
    }

    // ===== Migração para nested collections =====
    private fun migratedKey(orgId: String) = booleanPreferencesKey("migrated_nested_" + orgId)

    suspend fun isNestedMigrationDone(orgId: String): Boolean {
        val key = migratedKey(orgId)
        return dataStore.data.first()[key] ?: false
    }

    suspend fun setNestedMigrationDone(orgId: String, done: Boolean) {
        val key = migratedKey(orgId)
        dataStore.edit { prefs ->
            prefs[key] = done
        }
    }

    // Theme preferences
    val selectedTheme: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_THEME] ?: "system"
    }

    val selectedPalette: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_PALETTE] ?: "system"
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_THEME] ?: false
    }

    // Navigation preferences
    val currentScreen: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENT_SCREEN] ?: "HOME"
    }

    val isProfileMenuExpanded: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_PROFILE_MENU_EXPANDED] ?: false
    }

    // Data/sync preferences
    val uploadLocalToCloud: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.UPLOAD_LOCAL_TO_CLOUD] ?: true
    }

    val autoSyncEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_SYNC_ENABLED] ?: false
    }

    val selectedOrganizationId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_ORG_ID]
    }

    val syncWifiOnly: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SYNC_WIFI_ONLY] ?: true
    }

    val allowMobileData: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ALLOW_MOBILE_DATA] ?: false
    }

    val offlineMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OFFLINE_MODE] ?: false
    }

    val uploadQuality: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.UPLOAD_QUALITY] ?: "Alta"
    }

    // Theme setters
    suspend fun setSelectedTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] = theme
        }
    }

    suspend fun setSelectedPalette(palette: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_PALETTE] = palette
        }
    }

    suspend fun setIsDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDark
        }
    }

    // Navigation setters
    suspend fun setCurrentScreen(screen: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_SCREEN] = screen
        }
    }

    suspend fun setIsProfileMenuExpanded(expanded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PROFILE_MENU_EXPANDED] = expanded
        }
    }

    suspend fun setUploadLocalToCloud(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UPLOAD_LOCAL_TO_CLOUD] = enabled
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setSyncWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_WIFI_ONLY] = enabled
        }
    }

    suspend fun setAllowMobileData(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOW_MOBILE_DATA] = enabled
        }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFLINE_MODE] = enabled
        }
    }

    suspend fun setUploadQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UPLOAD_QUALITY] = quality
        }
    }

    suspend fun setSelectedOrganizationId(orgId: String?) {
        dataStore.edit { preferences ->
            if (orgId == null) preferences.remove(PreferencesKeys.SELECTED_ORG_ID)
            else preferences[PreferencesKeys.SELECTED_ORG_ID] = orgId
        }
    }

    // ===== Incremental sync timestamps (por org + escopo) =====
    private fun lastSyncKey(scope: String, orgId: String) = stringPreferencesKey("last_sync_${scope}_$orgId")

    suspend fun getLastSyncAt(scope: String, orgId: String): java.time.LocalDateTime? {
        val key = lastSyncKey(scope, orgId)
        val iso = dataStore.data.first()[key] ?: return null
        return try { java.time.LocalDateTime.parse(iso) } catch (e: Exception) { null }
    }

    suspend fun setLastSyncAt(scope: String, orgId: String, time: java.time.LocalDateTime) {
        val key = lastSyncKey(scope, orgId)
        dataStore.edit { prefs ->
            prefs[key] = time.toString()
        }
    }

    // Clear all preferences
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Extensões para migração de dados
    val dataVersion: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("data_version")]
    }
    
    suspend fun getDataVersion(): Int? {
        return dataVersion.first()
    }

    suspend fun setDataVersion(version: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("data_version")] = version
        }
    }
}
