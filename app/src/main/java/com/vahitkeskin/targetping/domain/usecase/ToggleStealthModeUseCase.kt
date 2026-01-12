package com.vahitkeskin.targetping.domain.usecase

import com.vahitkeskin.targetping.domain.repository.AppSettingsRepository
import javax.inject.Inject

class ToggleStealthModeUseCase @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) {
    suspend operator fun invoke(isEnabled: Boolean) {
        appSettingsRepository.setStealthMode(isEnabled)
        // Ekstra mantık: Stealth mod açılırsa son konumu temizle vs.
    }
}