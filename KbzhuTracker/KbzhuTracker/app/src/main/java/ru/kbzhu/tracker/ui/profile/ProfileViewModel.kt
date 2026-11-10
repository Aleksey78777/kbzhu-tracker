package ru.kbzhu.tracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.domain.calculator.NutritionCalculator
import ru.kbzhu.tracker.domain.model.ThemeMode
import ru.kbzhu.tracker.domain.model.UserSettings
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** @return false — значение вне диапазона 500–6000 мл */
    fun setWaterGoal(goalMl: Int?): Boolean {
        val current = settings.value ?: return false
        if (goalMl == null || goalMl !in 500..6000) return false
        viewModelScope.launch { settingsRepository.setWaterSettings(goalMl, current.waterPortionMl) }
        return true
    }

    fun setWaterPortion(portionMl: Int) {
        val current = settings.value ?: return
        viewModelScope.launch { settingsRepository.setWaterSettings(current.waterGoalMl, portionMl) }
    }

    fun recommendedWater(): Int? =
        settings.value?.profile?.let { NutritionCalculator.recommendedWaterMl(it.weightKg) }

    companion object {
        val WATER_PORTIONS = listOf(150, 200, 250, 300, 500)
    }
}
