package ru.kbzhu.tracker.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kbzhu.tracker.domain.model.DaySummary
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.usecase.ChangeWaterUseCase
import ru.kbzhu.tracker.domain.usecase.DeleteDiaryEntryUseCase
import ru.kbzhu.tracker.domain.usecase.ObserveDaySummaryUseCase
import ru.kbzhu.tracker.domain.usecase.RestoreDiaryEntryUseCase
import java.time.LocalDate
import javax.inject.Inject

sealed interface DiaryEvent {
    data class EntryDeleted(val entry: DiaryEntry) : DiaryEvent
}

/**
 * Дата хранится в состоянии пейджера (переживает поворот экрана),
 * поэтому ViewModel без собственного состояния: каждая страница пейджера
 * подписывается на свой день через [summary].
 */
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val observeDaySummary: ObserveDaySummaryUseCase,
    private val deleteDiaryEntry: DeleteDiaryEntryUseCase,
    private val restoreDiaryEntry: RestoreDiaryEntryUseCase,
    private val changeWaterUseCase: ChangeWaterUseCase
) : ViewModel() {

    private val _events = Channel<DiaryEvent>(Channel.BUFFERED)
    val events: Flow<DiaryEvent> = _events.receiveAsFlow()

    fun summary(date: LocalDate): Flow<DaySummary> = observeDaySummary(date)

    /** Удаляем запись, но фото оставляем до закрытия снекбара — вдруг нажмут «Отменить». */
    fun delete(entry: DiaryEntry) {
        viewModelScope.launch {
            deleteDiaryEntry(entry, deletePhoto = false)
            _events.send(DiaryEvent.EntryDeleted(entry))
        }
    }

    fun undoDelete(entry: DiaryEntry) {
        viewModelScope.launch { restoreDiaryEntry(entry) }
    }

    fun finalizeDelete(entry: DiaryEntry) {
        viewModelScope.launch {
            withContext(NonCancellable) { deleteDiaryEntry.cleanupPhoto(entry) }
        }
    }

    fun changeWater(date: LocalDate, deltaMl: Int) {
        viewModelScope.launch { changeWaterUseCase(date, deltaMl) }
    }
}
