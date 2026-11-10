package ru.kbzhu.tracker.ui.foods

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.core.util.toLocalDate
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.domain.model.NetworkError
import ru.kbzhu.tracker.domain.model.NetworkException
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.usecase.CacheRemoteFoodUseCase
import ru.kbzhu.tracker.domain.usecase.SearchFoodsUseCase
import ru.kbzhu.tracker.domain.usecase.SearchOnlineUseCase
import java.time.LocalDate
import javax.inject.Inject

enum class FoodTab { RECENT, FAVORITES, MY, ALL }

sealed interface OnlineSearchState {
    data object Idle : OnlineSearchState
    data object Loading : OnlineSearchState
    data class Results(val foods: List<Food>) : OnlineSearchState
    data class Error(val reason: NetworkError) : OnlineSearchState
}

sealed interface FoodSearchEvent {
    data class OpenFood(val foodId: Long) : FoodSearchEvent
}

/** Контекст «добавить в дневник», если экран открыт из дневника. */
data class AddContext(val meal: MealType, val date: LocalDate)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val searchFoods: SearchFoodsUseCase,
    private val searchOnline: SearchOnlineUseCase,
    private val cacheRemoteFood: CacheRemoteFoodUseCase
) : ViewModel() {

    /** Аргументы AddFoodRoute (meal, date) лежат в SavedStateHandle под именами полей. */
    val addContext: AddContext? = run {
        val meal = savedStateHandle.get<String>("meal")?.let { m -> MealType.entries.firstOrNull { it.name == m } }
        val date = savedStateHandle.get<Long>("date")?.toLocalDate()
        if (meal != null && date != null) AddContext(meal, date) else null
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow<FoodCategory?>(null)
    val category: StateFlow<FoodCategory?> = _category.asStateFlow()

    private val _tab = MutableStateFlow(FoodTab.RECENT)
    val tab: StateFlow<FoodTab> = _tab.asStateFlow()

    private val _online = MutableStateFlow<OnlineSearchState>(OnlineSearchState.Idle)
    val online: StateFlow<OnlineSearchState> = _online.asStateFlow()

    private val _events = Channel<FoodSearchEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var onlineJob: Job? = null

    /** null — ещё грузится (показываем индикатор), пустой список — ничего не найдено. */
    val searchResults: StateFlow<List<Food>?> =
        combine(_query.debounce(150), _category, _tab) { q, c, t -> Triple(q.trim(), c, t) }
            .flatMapLatest { (q, c, t) ->
                if (q.isEmpty() && c == null && t != FoodTab.ALL) flowOf(emptyList())
                else searchFoods(q, c, limit = if (q.isEmpty()) 300 else 100)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recent: StateFlow<List<Food>?> = foodRepository.observeRecent(30)
        .map<List<Food>, List<Food>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val favorites: StateFlow<List<Food>?> = foodRepository.observeFavorites()
        .map<List<Food>, List<Food>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myFoods: StateFlow<List<Food>?> = foodRepository.observeMyFoods()
        .map<List<Food>, List<Food>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) {
        _query.value = value.take(80)
        // Новый запрос — старые онлайн-результаты уже не актуальны
        onlineJob?.cancel()
        _online.value = OnlineSearchState.Idle
    }

    fun clearQuery() = onQueryChange("")

    fun selectCategory(category: FoodCategory?) {
        _category.value = if (_category.value == category) null else category
    }

    fun selectTab(tab: FoodTab) {
        _tab.value = tab
    }

    fun toggleFavorite(food: Food) {
        if (!food.isSaved) return
        viewModelScope.launch { foodRepository.setFavorite(food.id, !food.isFavorite) }
    }

    fun searchOnline() {
        val q = _query.value.trim()
        if (q.length < 2) return
        onlineJob?.cancel()
        onlineJob = viewModelScope.launch {
            _online.value = OnlineSearchState.Loading
            _online.value = searchOnline(q).fold(
                onSuccess = { OnlineSearchState.Results(it) },
                onFailure = { e ->
                    OnlineSearchState.Error((e as? NetworkException)?.reason ?: NetworkError.SERVER)
                }
            )
        }
    }

    /** Выбран продукт из интернета: сохраняем в базу (кэш) и открываем карточку. */
    fun selectOnlineFood(food: Food) {
        viewModelScope.launch {
            val saved = cacheRemoteFood(food)
            _events.send(FoodSearchEvent.OpenFood(saved.id))
        }
    }
}
