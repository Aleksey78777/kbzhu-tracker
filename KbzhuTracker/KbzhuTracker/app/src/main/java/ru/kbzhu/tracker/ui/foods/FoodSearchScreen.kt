package ru.kbzhu.tracker.ui.foods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.ui.components.EmptyState
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.util.formatDayMonth
import ru.kbzhu.tracker.ui.util.messageRes
import ru.kbzhu.tracker.ui.util.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    onFoodSelected: (Long) -> Unit,
    onCreateFood: () -> Unit,
    onCreateRecipe: () -> Unit,
    onEditFood: (Food) -> Unit,
    onScan: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: FoodSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val myFoods by viewModel.myFoods.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showCreateMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FoodSearchEvent.OpenFood -> onFoodSelected(event.foodId)
            }
        }
    }

    val addContext = viewModel.addContext
    val isSearching = query.isNotBlank() || category != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (addContext != null) {
                        Column {
                            Text(stringResource(R.string.foods_add_title))
                            Text(
                                "${stringResource(addContext.meal.titleRes)} · ${addContext.date.formatDayMonth()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(stringResource(R.string.nav_foods))
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.QrCodeScanner, stringResource(R.string.diary_scan_barcode))
                    }
                    Box {
                        IconButton(onClick = { showCreateMenu = true }) {
                            Icon(Icons.Filled.Add, stringResource(R.string.foods_create))
                        }
                        DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.foods_create_product)) },
                                leadingIcon = { Icon(Icons.Filled.Inventory2, null) },
                                onClick = { showCreateMenu = false; onCreateFood() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.foods_create_recipe)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) },
                                onClick = { showCreateMenu = false; onCreateRecipe() }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ---------- Поле поиска ----------
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.foods_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Filled.Clear, stringResource(R.string.foods_clear))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            // ---------- Фильтр по категориям ----------
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FoodCategory.entries, key = { it.name }) { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { viewModel.selectCategory(c) },
                        label = { Text(stringResource(c.titleRes)) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            if (!isSearching) {
                // ---------- Вкладки, когда поиск пуст ----------
                PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 16.dp) {
                    FoodTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { viewModel.selectTab(t) },
                            text = { Text(stringResource(t.titleRes)) }
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                if (isSearching) {
                    SearchResultsList(
                        results = results,
                        query = query,
                        online = online,
                        onFoodClick = { onFoodSelected(it.id) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onEdit = onEditFood,
                        onSearchOnline = viewModel::searchOnline,
                        onOnlineFoodClick = viewModel::selectOnlineFood,
                        onCreateFood = onCreateFood
                    )
                } else {
                    val list = when (tab) {
                        FoodTab.RECENT -> recent
                        FoodTab.FAVORITES -> favorites
                        FoodTab.MY -> myFoods
                        FoodTab.ALL -> results
                    }
                    when {
                        list == null -> LoadingBox()
                        list.isEmpty() -> TabEmptyState(tab, onCreateFood, onCreateRecipe)
                        else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(list, key = { it.id }) { food ->
                                FoodRow(
                                    food = food,
                                    onClick = { onFoodSelected(food.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(food) },
                                    onEdit = if (tab == FoodTab.MY && food.isUserEditable) ({ onEditFood(food) }) else null
                                )
                                HorizontalDivider(Modifier.padding(start = 78.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private val FoodTab.titleRes: Int
    get() = when (this) {
        FoodTab.RECENT -> R.string.foods_tab_recent
        FoodTab.FAVORITES -> R.string.foods_tab_favorites
        FoodTab.MY -> R.string.foods_tab_my
        FoodTab.ALL -> R.string.foods_tab_all
    }

@Composable
private fun TabEmptyState(tab: FoodTab, onCreateFood: () -> Unit, onCreateRecipe: () -> Unit) {
    when (tab) {
        FoodTab.RECENT -> EmptyState(
            icon = Icons.Filled.History,
            title = stringResource(R.string.foods_empty_recent_title),
            message = stringResource(R.string.foods_empty_recent_message)
        )
        FoodTab.FAVORITES -> EmptyState(
            icon = Icons.Filled.Star,
            title = stringResource(R.string.foods_empty_fav_title),
            message = stringResource(R.string.foods_empty_fav_message)
        )
        FoodTab.MY -> Column {
            EmptyState(
                icon = Icons.Filled.Inventory2,
                title = stringResource(R.string.foods_empty_my_title),
                message = stringResource(R.string.foods_empty_my_message),
                actionText = stringResource(R.string.foods_create_product),
                onAction = onCreateFood
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onCreateRecipe) { Text(stringResource(R.string.foods_create_recipe)) }
            }
        }
        FoodTab.ALL -> LoadingBox()
    }
}

@Composable
private fun SearchResultsList(
    results: List<Food>?,
    query: String,
    online: OnlineSearchState,
    onFoodClick: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
    onEdit: (Food) -> Unit,
    onSearchOnline: () -> Unit,
    onOnlineFoodClick: (Food) -> Unit,
    onCreateFood: () -> Unit
) {
    if (results == null) {
        LoadingBox()
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        if (results.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(R.string.foods_nothing_found),
                    message = stringResource(R.string.foods_nothing_found_message),
                    actionText = stringResource(R.string.foods_create_product),
                    onAction = onCreateFood
                )
            }
        }
        items(results, key = { "local_${it.id}" }) { food ->
            FoodRow(
                food = food,
                onClick = { onFoodClick(food) },
                onToggleFavorite = { onToggleFavorite(food) },
                onEdit = if (food.isUserEditable) ({ onEdit(food) }) else null
            )
            HorizontalDivider(Modifier.padding(start = 78.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
        if (query.trim().length >= 2) {
            onlineSection(online, onSearchOnline, onOnlineFoodClick)
        }
    }
}

/** Блок «Поиск в Open Food Facts» под локальными результатами. */
private fun LazyListScope.onlineSection(
    online: OnlineSearchState,
    onSearchOnline: () -> Unit,
    onOnlineFoodClick: (Food) -> Unit
) {
    item(key = "online_header") {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Public, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.foods_online_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (online) {
                            is OnlineSearchState.Error -> stringResource(online.reason.messageRes)
                            is OnlineSearchState.Results ->
                                if (online.foods.isEmpty()) stringResource(R.string.foods_online_empty)
                                else stringResource(R.string.foods_online_found, online.foods.size)
                            else -> stringResource(R.string.foods_online_hint)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                when (online) {
                    OnlineSearchState.Loading -> CircularProgressIndicator(Modifier.size(28.dp))
                    is OnlineSearchState.Results -> Unit
                    else -> TextButton(onClick = onSearchOnline) {
                        Text(stringResource(if (online is OnlineSearchState.Error) R.string.action_retry else R.string.foods_online_search))
                    }
                }
            }
        }
    }
    if (online is OnlineSearchState.Results) {
        items(online.foods, key = { "online_${it.barcode ?: it.name}" }) { food ->
            FoodRow(food = food, onClick = { onOnlineFoodClick(food) })
            HorizontalDivider(Modifier.padding(start = 78.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}
