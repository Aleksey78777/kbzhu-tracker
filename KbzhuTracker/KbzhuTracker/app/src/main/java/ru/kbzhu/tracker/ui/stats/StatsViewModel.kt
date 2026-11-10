package ru.kbzhu.tracker.ui.stats

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.core.util.today
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.StatsData
import ru.kbzhu.tracker.domain.model.StatsPeriod
import ru.kbzhu.tracker.domain.model.WeightRecord
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.domain.repository.WeightRepository
import ru.kbzhu.tracker.domain.usecase.LogWeightUseCase
import ru.kbzhu.tracker.domain.usecase.ObserveStatisticsUseCase
import ru.kbzhu.tracker.ui.util.parseDecimal
import java.time.LocalDate
import javax.inject.Inject

sealed interface StatsEvent {
    data class Message(@StringRes val res: Int) : StatsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    observeStatistics: ObserveStatisticsUseCase,
    settingsRepository: SettingsRepository,
    private val weightRepository: WeightRepository,
    private val logWeight: LogWeightUseCase
) : ViewModel() {

    private val _period = MutableStateFlow(StatsPeriod.WEEK)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    val stats: StateFlow<StatsData?> = _period
        .flatMapLatest { observeStatistics(it) }
        .map<StatsData, StatsData?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val targets: StateFlow<NutritionTargets> = settingsRepository.settings
        .map { it.targets }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionTargets.DEFAULT)

    /** Желаемый вес — линия цели на графике (если цель не «поддержание»). */
    val goalWeight: StateFlow<Double?> = settingsRepository.settings
        .map { s -> s.profile?.takeIf { it.goal != Goal.MAINTAIN }?.targetWeightKg }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestWeight: StateFlow<WeightRecord?> = weightRepository.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Вся история веса — для списка отметок. */
    val weightHistory: StateFlow<List<WeightRecord>> = weightRepository.observeAll()
        .map { it.reversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<StatsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun selectPeriod(period: StatsPeriod) {
        _period.value = period
    }

    /** @return true — сохранено, false — некорректное значение */
    fun saveWeight(text: String, date: LocalDate = today()): Boolean {
        val kg = text.parseDecimal()
        if (kg == null || kg !in 25.0..350.0) return false
        viewModelScope.launch {
            logWeight(date, kg)
            _events.send(StatsEvent.Message(R.string.stats_weight_saved))
        }
        return true
    }

    fun deleteWeight(record: WeightRecord) {
        viewModelScope.launch { weightRepository.delete(record.date) }
    }
}
