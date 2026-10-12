package com.bharavahi.core.domain.usecase.settings

import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** UI-5: System, Light or Dark, persisted on the device. */
class ObserveThemeModeUseCase @Inject constructor(private val repository: ThemePreferenceRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val repository: ThemePreferenceRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
