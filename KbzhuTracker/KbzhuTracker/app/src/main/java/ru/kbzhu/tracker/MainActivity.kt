package ru.kbzhu.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.kbzhu.tracker.domain.model.ThemeMode
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.ui.navigation.KbzhuNavApp
import ru.kbzhu.tracker.ui.theme.KbzhuTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            when (val s = state) {
                MainUiState.Loading -> KbzhuTheme {
                    // Доли секунды, пока читается DataStore: просто фон окна
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
                is MainUiState.Ready -> KbzhuTheme(themeMode = s.themeMode) {
                    KbzhuNavApp(startWithOnboarding = !s.onboardingCompletedAtStart)
                }
            }
        }
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(
        /** Фиксируется один раз при запуске — чтобы не пересоздавать граф навигации */
        val onboardingCompletedAtStart: Boolean,
        val themeMode: ThemeMode
    ) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = flow {
        val initial = settingsRepository.settings.first()
        emitAll(
            settingsRepository.settings.map {
                MainUiState.Ready(initial.onboardingCompleted, it.themeMode)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
