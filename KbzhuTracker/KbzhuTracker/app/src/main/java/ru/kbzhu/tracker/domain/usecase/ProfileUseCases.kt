package ru.kbzhu.tracker.domain.usecase

import kotlinx.coroutines.flow.first
import ru.kbzhu.tracker.core.util.today
import ru.kbzhu.tracker.domain.calculator.NutritionCalculator
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.TargetsCalculation
import ru.kbzhu.tracker.domain.model.UserProfile
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.domain.repository.WeightRepository
import java.time.LocalDate
import javax.inject.Inject

class CalculateTargetsUseCase @Inject constructor() {
    operator fun invoke(profile: UserProfile): TargetsCalculation = NutritionCalculator.calculate(profile)
}

/**
 * Сохраняет профиль (онбординг или настройки).
 * [manualTargets] != null — пользователь скорректировал норму вручную.
 */
class SaveProfileUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val weightRepository: WeightRepository
) {
    suspend operator fun invoke(
        profile: UserProfile,
        manualTargets: NutritionTargets?,
        completeOnboarding: Boolean
    ) {
        val targets = manualTargets ?: NutritionCalculator.calculate(profile).targets
        settingsRepository.saveProfile(profile, targets, manualTargets = manualTargets != null)

        // Первая отметка веса появляется сразу — график не будет пустым
        val latest = weightRepository.observeLatest().first()
        if (latest == null || latest.weightKg != profile.weightKg) {
            weightRepository.log(today(), profile.weightKg)
        }

        if (completeOnboarding) {
            val current = settingsRepository.settings.first()
            // Норма воды по умолчанию — от веса, если пользователь её не менял
            if (current.waterGoalMl == UserSettingsDefaults.WATER_GOAL) {
                settingsRepository.setWaterSettings(
                    NutritionCalculator.recommendedWaterMl(profile.weightKg),
                    current.waterPortionMl
                )
            }
            settingsRepository.setOnboardingCompleted(true)
        }
    }
}

/**
 * Отметка веса. Если отметка самая свежая — обновляем вес в профиле
 * и пересчитываем норму (если она не задана вручную).
 */
class LogWeightUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val weightRepository: WeightRepository
) {
    suspend operator fun invoke(date: LocalDate, weightKg: Double) {
        require(weightKg in 25.0..350.0) { "Недопустимый вес" }
        val latestBefore = weightRepository.observeLatest().first()
        weightRepository.log(date, weightKg)

        val isNewest = latestBefore == null || !date.isBefore(latestBefore.date)
        if (!isNewest) return

        val settings = settingsRepository.settings.first()
        val profile = settings.profile ?: return
        val updated = profile.copy(weightKg = weightKg)
        val targets = if (settings.manualTargets) settings.targets
        else NutritionCalculator.calculate(updated).targets
        settingsRepository.saveProfile(updated, targets, settings.manualTargets)
    }
}

object UserSettingsDefaults {
    const val WATER_GOAL = 2000
}
