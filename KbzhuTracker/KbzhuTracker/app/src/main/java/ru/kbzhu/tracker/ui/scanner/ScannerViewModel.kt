package ru.kbzhu.tracker.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.domain.model.BarcodeLookupResult
import ru.kbzhu.tracker.domain.model.NetworkError
import ru.kbzhu.tracker.domain.usecase.LookupBarcodeUseCase
import javax.inject.Inject

sealed interface ScanState {
    data object Scanning : ScanState
    data class Looking(val barcode: String) : ScanState
    data class NotFound(val barcode: String) : ScanState
    data class Error(val barcode: String, val reason: NetworkError) : ScanState
}

sealed interface ScannerEvent {
    data class Found(val foodId: Long) : ScannerEvent
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val lookupBarcode: LookupBarcodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _events = Channel<ScannerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lookupJob: Job? = null

    /** Последний «отклонённый» код: не реагируем на него сразу после «Сканировать ещё». */
    private var ignoredCode: String? = null
    private var ignoredUntil = 0L

    fun onBarcodeDetected(raw: String, format: Int) {
        if (_state.value !is ScanState.Scanning) return
        val code = BarcodeValidator.normalize(raw, format) ?: return
        if (code == ignoredCode && System.currentTimeMillis() < ignoredUntil) return
        lookup(code)
    }

    fun submitManual(code: String): Boolean {
        val normalized = BarcodeValidator.normalize(code, null) ?: return false
        lookup(normalized)
        return true
    }

    fun retry() {
        val current = _state.value
        if (current is ScanState.Error) lookup(current.barcode)
    }

    fun scanAgain() {
        when (val current = _state.value) {
            is ScanState.NotFound -> ignore(current.barcode)
            is ScanState.Error -> ignore(current.barcode)
            else -> Unit
        }
        lookupJob?.cancel()
        _state.value = ScanState.Scanning
    }

    private fun ignore(code: String) {
        ignoredCode = code
        ignoredUntil = System.currentTimeMillis() + 2_500
    }

    private fun lookup(code: String) {
        lookupJob?.cancel()
        _state.value = ScanState.Looking(code)
        lookupJob = viewModelScope.launch {
            when (val result = lookupBarcode(code)) {
                is BarcodeLookupResult.Found -> {
                    _events.send(ScannerEvent.Found(result.food.id))
                }
                is BarcodeLookupResult.NotFound -> _state.value = ScanState.NotFound(result.barcode)
                is BarcodeLookupResult.Error -> _state.value = ScanState.Error(result.barcode, result.reason)
            }
        }
    }
}
