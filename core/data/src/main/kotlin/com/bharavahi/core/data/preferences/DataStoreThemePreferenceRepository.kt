package com.bharavahi.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.repository.ThemePreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

internal class DataStoreThemePreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ThemePreferenceRepository {

    private val key = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[key] } ?: ThemeMode.DEFAULT
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[key] = mode.name }
    }
}
