package ru.kbzhu.tracker.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.domain.calculator.NutritionCalculator
import ru.kbzhu.tracker.domain.model.ActivityLevel
import ru.kbzhu.tracker.domain.model.Gender
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.MacroSplit
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.TargetsCalculation
import ru.kbzhu.tracker.domain.model.UserProfile
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.domain.usecase.CalculateTargetsUseCase
import ru.kbzhu.tracker.domain.usecase.SaveProfileUseCase
import ru.kbzhu.tracker.ui.navigation.OnboardingRoute
import ru.kbzhu.tracker.ui.util.parseDecimal
import ru.kbzhu.tracker.ui.util.sanitizeDecimal
import ru.kbzhu.tracker.ui.util.sanitizeInt
import ru.kbzhu.tracker.ui.util.toInputString
import javax.inject.Inject
import kotlin.math.roundToInt

enum class OnboardingStep { GENDER, BODY, GOAL, TARGET, ACTIVITY, MACROS, RESULT }

enum class Macro { PROTEIN, FAT, CARBS }

/** Готовые варианты распределения БЖУ */
enum class MacroPreset(val split: MacroSplit) {
    BALANCED(MacroSplit(30, 30, 40)),
    HIGH_PROTEIN(MacroSplit(40, 30, 30)),
    LOW_CARB(MacroSplit(35, 45, 20)),
    SPORT(MacroSplit(25, 20, 55))
}

data class OnboardingUiState(
    val editMode: Boolean = false,
    val step: OnboardingStep = OnboardingStep.GENDER,
    val gender: Gender? = null,
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val goal: Goal? = null,
    val targetWeight: String = "",
    val weeklyRate: Double = 0.5,
    val activity: ActivityLevel? = null,
    val macroSplit: MacroSplit = MacroSplit(),
    val calculation: TargetsCalculation? = null,
    val manualEnabled: Boolean = false,
    val manualCalories: String = "",
    val manualProtein: String = "",
    val manualFat: String = "",
    val manualCarbs: String = "",
    val isSaving: Boolean = false
) {
    val ageValue: Int? get() = age.toIntOrNull()
    val heightValue: Int? get() = height.toIntOrNull()
    val weightValue: Double? get() = weight.parseDecimal()
    val targetWeightValue: Double? get() = targetWeight.parseDecimal()

    val ageValid get() = ageValue in 14..100
    val heightValid get() = heightValue in 120..230
    val weightValid get() = weightValue?.let { it in 30.0..300.0 } == true

    /** Желаемый вес согласуется с целью (при похудении — меньше текущего и т. д.) */
    val targetWeightValid: Boolean
        get() {
            val target = targetWeightValue ?: return false
            val current = weightValue ?: return false
            if (target !in 30.0..300.0) return false
            return when (goal) {
                Goal.LOSE -> target < current
                Goal.GAIN -> target > current
                else -> true
            }
        }

    /** TARGET пропускаем, если цель — поддержание. */
    val visibleSteps: List<OnboardingStep>
        get() = OnboardingStep.entries.filter { it != OnboardingStep.TARGET || goal != Goal.MAINTAIN }

    val stepIndex: Int get() = visibleSteps.indexOf(step).coerceAtLeast(0)
    val progress: Float get() = (stepIndex + 1f) / visibleSteps.size
    val isFirstStep: Boolean get() = stepIndex == 0
    val isLastStep: Boolean get() = step == OnboardingStep.RESULT

    val manualTargets: NutritionTargets?
        get() {
            if (!manualEnabled) return null
            val kcal = manualCalories.toIntOrNull() ?: return null
            return NutritionTargets(
                kcal,
                manualProtein.toIntOrNull() ?: return null,
                manualFat.toIntOrNull() ?: return null,
                manualCarbs.toIntOrNull() ?: return null
            )
        }

    val manualValid: Boolean
        get() = !manualEnabled || manualTargets?.let { it.calories in 800..8000 } == true

    val canProceed: Boolean
        get() = when (step) {
            OnboardingStep.GENDER -> gender != null
            OnboardingStep.BODY -> ageValid && heightValid && weightValid
            OnboardingStep.GOAL -> goal != null
            OnboardingStep.TARGET -> targetWeightValid
            OnboardingStep.ACTIVITY -> activity != null
            OnboardingStep.MACROS -> macroSplit.isValid
            OnboardingStep.RESULT -> calculation != null && manualValid && !isSaving
        }

    fun toProfile(): UserProfile? {
        val profileGoal = goal ?: return null
        val currentWeight = weightValue ?: return null
        return UserProfile(
            gender = gender ?: return null,
            age = ageValue ?: return null,
            heightCm = heightValue ?: return null,
            weightKg = currentWeight,
            goal = profileGoal,
            targetWeightKg = if (profileGoal == Goal.MAINTAIN) currentWeight else targetWeightValue ?: currentWeight,
            weeklyRateKg = if (profileGoal == Goal.MAINTAIN) 0.0 else weeklyRate,
            activityLevel = activity ?: return null,
            macroSplit = macroSplit
        )
    }
}

sealed interface OnboardingEvent {
    data object Finished : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val calculateTargets: CalculateTargetsUseCase,
    private val saveProfile: SaveProfileUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<OnboardingRoute>()

    private val _state = MutableStateFlow(OnboardingUiState(editMode = route.editMode))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (route.editMode) {
            viewModelScope.launch {
                val settings = settingsRepository.settings.first()
                val p = settings.profile ?: return@launch
                _state.update {
                    it.copy(
                        gender = p.gender,
                        age = p.age.toString(),
                        height = p.heightCm.toString(),
                        weight = p.weightKg.toInputString(),
                        goal = p.goal,
                        targetWeight = p.targetWeightKg.toInputString(),
                        weeklyRate = p.weeklyRateKg.takeIf { r -> r > 0 } ?: defaultRate(p.goal),
                        activity = p.activityLevel,
                        macroSplit = p.macroSplit,
                        manualEnabled = settings.manualTargets,
                        manualCalories = settings.targets.calories.toString(),
                        manualProtein = settings.targets.protein.toString(),
                        manualFat = settings.targets.fat.toString(),
                        manualCarbs = settings.targets.carbs.toString()
                    )
                }
            }
        }
    }

    // ---------- Ввод ----------
    fun selectGender(gender: Gender) = _state.update { it.copy(gender = gender) }

    fun onAgeChange(v: String) { sanitizeInt(v, 3)?.let { s -> _state.update { it.copy(age = s) } } }
    fun onHeightChange(v: String) { sanitizeInt(v, 3)?.let { s -> _state.update { it.copy(height = s) } } }
    fun onWeightChange(v: String) { sanitizeDecimal(v, 1, 5)?.let { s -> _state.update { it.copy(weight = s) } } }
    fun onTargetWeightChange(v: String) { sanitizeDecimal(v, 1, 5)?.let { s -> _state.update { it.copy(targetWeight = s) } } }

    fun selectGoal(goal: Goal) = _state.update { s ->
        val weight = s.weightValue
        // Подставляем разумный желаемый вес, если пользователь его ещё не вводил или цель сменилась
        val suggestedTarget = when {
            weight == null -> s.targetWeight
            goal == Goal.LOSE && (s.goal != Goal.LOSE || s.targetWeight.isBlank()) -> (weight - 5).coerceAtLeast(35.0).toInputString()
            goal == Goal.GAIN && (s.goal != Goal.GAIN || s.targetWeight.isBlank()) -> (weight + 3).toInputString()
            else -> s.targetWeight
        }
        s.copy(
            goal = goal,
            targetWeight = suggestedTarget,
            weeklyRate = if (s.goal != goal) defaultRate(goal) else s.weeklyRate
        )
    }

    fun selectRate(rate: Double) = _state.update { it.copy(weeklyRate = rate) }

    fun selectActivity(level: ActivityLevel) = _state.update { it.copy(activity = level) }

    fun selectPreset(preset: MacroPreset) = _state.update { it.copy(macroSplit = preset.split) }

    /** Шаг ±5%. Сумму 100% пользователь доводит сам — показываем подсказку. */
    fun changeMacro(macro: Macro, delta: Int) = _state.update { s ->
        val m = s.macroSplit
        val updated = when (macro) {
            Macro.PROTEIN -> m.copy(proteinPercent = (m.proteinPercent + delta).coerceIn(5, 80))
            Macro.FAT -> m.copy(fatPercent = (m.fatPercent + delta).coerceIn(5, 80))
            Macro.CARBS -> m.copy(carbsPercent = (m.carbsPercent + delta).coerceIn(5, 80))
        }
        s.copy(macroSplit = updated)
    }

    // ---------- Ручная корректировка нормы ----------
    fun setManualEnabled(enabled: Boolean) = _state.update { s ->
        val base = s.calculation?.targets
        if (enabled && base != null && !s.manualEnabled) {
            s.copy(
                manualEnabled = true,
                manualCalories = base.calories.toString(),
                manualProtein = base.protein.toString(),
                manualFat = base.fat.toString(),
                manualCarbs = base.carbs.toString()
            )
        } else s.copy(manualEnabled = enabled)
    }

    /** Меняем калории — граммы БЖУ пересчитываются по выбранному распределению. */
    fun onManualCaloriesChange(v: String) {
        val clean = sanitizeInt(v, 4) ?: return
        _state.update { s ->
            val kcal = clean.toIntOrNull()
            if (kcal == null) s.copy(manualCalories = clean)
            else {
                val t = NutritionCalculator.macros(kcal, s.macroSplit)
                s.copy(
                    manualCalories = clean,
                    manualProtein = t.protein.toString(),
                    manualFat = t.fat.toString(),
                    manualCarbs = t.carbs.toString()
                )
            }
        }
    }

    /** Меняем граммы — калории = Б·4 + Ж·9 + У·4. */
    fun onManualMacroChange(macro: Macro, v: String) {
        val clean = sanitizeInt(v, 4) ?: return
        _state.update { s ->
            val updated = when (macro) {
                Macro.PROTEIN -> s.copy(manualProtein = clean)
                Macro.FAT -> s.copy(manualFat = clean)
                Macro.CARBS -> s.copy(manualCarbs = clean)
            }
            val p = updated.manualProtein.toIntOrNull()
            val f = updated.manualFat.toIntOrNull()
            val c = updated.manualCarbs.toIntOrNull()
            if (p != null && f != null && c != null) {
                updated.copy(manualCalories = (p * 4 + f * 9 + c * 4).toString())
            } else updated
        }
    }

    // ---------- Переходы ----------
    fun next() {
        val s = _state.value
        if (!s.canProceed) return
        if (s.isLastStep) {
            finish()
            return
        }
        val nextStep = s.visibleSteps.getOrNull(s.stepIndex + 1) ?: return
        _state.update {
            val withStep = it.copy(step = nextStep)
            if (nextStep == OnboardingStep.MACROS || nextStep == OnboardingStep.RESULT) recalculate(withStep) else withStep
        }
    }

    /** @return false, если это первый шаг (назад некуда) */
    fun back(): Boolean {
        val s = _state.value
        if (s.isFirstStep) return false
        val prev = s.visibleSteps.getOrNull(s.stepIndex - 1) ?: return false
        _state.update { it.copy(step = prev) }
        return true
    }

    private fun recalculate(s: OnboardingUiState): OnboardingUiState {
        val profile = s.toProfile() ?: return s
        val calc = calculateTargets(profile)
        return if (s.manualEnabled) s.copy(calculation = calc)
        else s.copy(
            calculation = calc,
            manualCalories = calc.targets.calories.toString(),
            manualProtein = calc.targets.protein.toString(),
            manualFat = calc.targets.fat.toString(),
            manualCarbs = calc.targets.carbs.toString()
        )
    }

    private fun finish() {
        val s = _state.value
        val profile = s.toProfile() ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveProfile(profile, s.manualTargets, completeOnboarding = !s.editMode)
            _state.update { it.copy(isSaving = false) }
            _events.send(OnboardingEvent.Finished)
        }
    }

    private fun defaultRate(goal: Goal): Double = when (goal) {
        Goal.LOSE -> 0.5
        Goal.GAIN -> 0.25
        Goal.MAINTAIN -> 0.0
    }

    /** Граммы для предпросмотра на шаге БЖУ. */
    fun previewGrams(macro: Macro, s: OnboardingUiState): Int? {
        val kcal = s.calculation?.targets?.calories ?: return null
        val pct = when (macro) {
            Macro.PROTEIN -> s.macroSplit.proteinPercent
            Macro.FAT -> s.macroSplit.fatPercent
            Macro.CARBS -> s.macroSplit.carbsPercent
        }
        val kcalPerGram = if (macro == Macro.FAT) 9.0 else 4.0
        return (kcal * pct / 100.0 / kcalPerGram).roundToInt()
    }
}
