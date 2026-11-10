package ru.kbzhu.tracker.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kbzhu.tracker.domain.model.ActivityLevel
import ru.kbzhu.tracker.domain.model.Gender
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.MacroSplit
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.ThemeMode
import ru.kbzhu.tracker.domain.model.UserProfile
import ru.kbzhu.tracker.domain.model.UserSettings
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Профиль, норма и настройки в Preferences DataStore. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val GENDER = stringPreferencesKey("gender")
        val AGE = intPreferencesKey("age")
        val HEIGHT = intPreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val GOAL = stringPreferencesKey("goal")
        val TARGET_WEIGHT = doublePreferencesKey("target_weight_kg")
        val WEEKLY_RATE = doublePreferencesKey("weekly_rate_kg")
        val ACTIVITY = stringPreferencesKey("activity")
        val MACRO_P = intPreferencesKey("macro_protein_pct")
        val MACRO_F = intPreferencesKey("macro_fat_pct")
        val MACRO_C = intPreferencesKey("macro_carbs_pct")
        val T_KCAL = intPreferencesKey("target_kcal")
        val T_P = intPreferencesKey("target_protein")
        val T_F = intPreferencesKey("target_fat")
        val T_C = intPreferencesKey("target_carbs")
        val MANUAL = booleanPreferencesKey("manual_targets")
        val THEME = stringPreferencesKey("theme_mode")
        val WATER_GOAL = intPreferencesKey("water_goal_ml")
        val WATER_PORTION = intPreferencesKey("water_portion_ml")
    }

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun saveProfile(profile: UserProfile, targets: NutritionTargets, manualTargets: Boolean) {
        dataStore.edit { p ->
            p.writeProfile(profile)
            p[Keys.T_KCAL] = targets.calories
            p[Keys.T_P] = targets.protein
            p[Keys.T_F] = targets.fat
            p[Keys.T_C] = targets.carbs
            p[Keys.MANUAL] = manualTargets
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING] = completed }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setWaterSettings(goalMl: Int, portionMl: Int) {
        dataStore.edit {
            it[Keys.WATER_GOAL] = goalMl.coerceIn(500, 6000)
            it[Keys.WATER_PORTION] = portionMl.coerceIn(50, 1000)
        }
    }

    private fun MutablePreferences.writeProfile(profile: UserProfile) {
        this[Keys.GENDER] = profile.gender.name
        this[Keys.AGE] = profile.age
        this[Keys.HEIGHT] = profile.heightCm
        this[Keys.WEIGHT] = profile.weightKg
        this[Keys.GOAL] = profile.goal.name
        this[Keys.TARGET_WEIGHT] = profile.targetWeightKg
        this[Keys.WEEKLY_RATE] = profile.weeklyRateKg
        this[Keys.ACTIVITY] = profile.activityLevel.name
        this[Keys.MACRO_P] = profile.macroSplit.proteinPercent
        this[Keys.MACRO_F] = profile.macroSplit.fatPercent
        this[Keys.MACRO_C] = profile.macroSplit.carbsPercent
    }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        val profile = run {
            val gender = enumOrNull<Gender>(this[Keys.GENDER]) ?: return@run null
            UserProfile(
                gender = gender,
                age = this[Keys.AGE] ?: return@run null,
                heightCm = this[Keys.HEIGHT] ?: return@run null,
                weightKg = this[Keys.WEIGHT] ?: return@run null,
                goal = enumOrNull<Goal>(this[Keys.GOAL]) ?: Goal.MAINTAIN,
                targetWeightKg = this[Keys.TARGET_WEIGHT] ?: this[Keys.WEIGHT] ?: 0.0,
                weeklyRateKg = this[Keys.WEEKLY_RATE] ?: 0.0,
                activityLevel = enumOrNull<ActivityLevel>(this[Keys.ACTIVITY]) ?: ActivityLevel.LIGHT,
                macroSplit = MacroSplit(
                    proteinPercent = this[Keys.MACRO_P] ?: 30,
                    fatPercent = this[Keys.MACRO_F] ?: 30,
                    carbsPercent = this[Keys.MACRO_C] ?: 40
                )
            )
        }
        val kcal = this[Keys.T_KCAL]
        val targets = if (kcal != null) {
            NutritionTargets(kcal, this[Keys.T_P] ?: 0, this[Keys.T_F] ?: 0, this[Keys.T_C] ?: 0)
        } else defaults.targets

        return UserSettings(
            onboardingCompleted = this[Keys.ONBOARDING] ?: false,
            profile = profile,
            targets = targets,
            manualTargets = this[Keys.MANUAL] ?: false,
            themeMode = enumOrNull<ThemeMode>(this[Keys.THEME]) ?: ThemeMode.SYSTEM,
            waterGoalMl = this[Keys.WATER_GOAL] ?: defaults.waterGoalMl,
            waterPortionMl = this[Keys.WATER_PORTION] ?: defaults.waterPortionMl
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String?): T? =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } }
}
