package ru.kbzhu.tracker.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.util.defaultMealForNow
import ru.kbzhu.tracker.ui.util.formatFull
import ru.kbzhu.tracker.ui.util.icon
import ru.kbzhu.tracker.ui.util.titleRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Пейджер дней: 10 лет назад и год вперёд от сегодняшнего дня. */
private const val TODAY_PAGE = 3650
private const val PAGE_COUNT = TODAY_PAGE + 366

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddFood: (MealType, LocalDate) -> Unit,
    onScan: (MealType, LocalDate) -> Unit,
    onEditEntry: (Long) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val today = remember { LocalDate.now() }
    fun dateFor(page: Int): LocalDate = today.plusDays((page - TODAY_PAGE).toLong())
    fun pageFor(date: LocalDate): Int =
        (TODAY_PAGE + java.time.temporal.ChronoUnit.DAYS.between(today, date)).toInt().coerceIn(0, PAGE_COUNT - 1)

    val pagerState = rememberPagerState(initialPage = TODAY_PAGE) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var currentDate by remember { mutableStateOf(dateFor(pagerState.currentPage)) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentDate = dateFor(it) }
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // Снекбар «Запись удалена — Отменить»
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DiaryEvent.EntryDeleted -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.diary_entry_deleted, event.entry.name),
                            actionLabel = context.getString(R.string.action_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.entry)
                        else viewModel.finalizeDelete(event.entry)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            DiaryTopBar(
                date = currentDate,
                today = today,
                onPrev = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                onToday = { scope.launch { pagerState.animateScrollToPage(TODAY_PAGE) } },
                onPickDate = { showDatePicker = true }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.diary_add_food)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1,
            key = { it }
        ) { page ->
            val date = dateFor(page)
            val summary by remember(date) { viewModel.summary(date) }
                .collectAsStateWithLifecycle(initialValue = null)
            val s = summary
            if (s == null) {
                LoadingBox()
            } else {
                DayContent(
                    summary = s,
                    onAddToMeal = { meal -> onAddFood(meal, date) },
                    onScanToMeal = { meal -> onScan(meal, date) },
                    onEditEntry = { onEditEntry(it.id) },
                    onDeleteEntry = viewModel::delete,
                    onWaterChange = { delta -> viewModel.changeWater(date, delta) }
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        scope.launch { pagerState.scrollToPage(pageFor(picked)) }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showAddSheet) {
        AddFoodSheet(
            date = currentDate,
            onDismiss = { showAddSheet = false },
            onMealSelected = { meal ->
                showAddSheet = false
                onAddFood(meal, currentDate)
            },
            onScan = { meal ->
                showAddSheet = false
                onScan(meal, currentDate)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryTopBar(
    date: LocalDate,
    today: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPickDate: () -> Unit
) {
    val relative = when (date) {
        today -> stringResource(R.string.date_today)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        today.plusDays(1) -> stringResource(R.string.date_tomorrow)
        else -> null
    }
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.diary_prev_day))
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onPickDate)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(relative ?: date.formatFull(), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = stringResource(R.string.diary_pick_date),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (relative != null) {
                    Text(
                        date.formatFull(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if (date != today) {
                IconButton(onClick = onToday) {
                    Icon(Icons.Filled.Today, stringResource(R.string.diary_go_today))
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.diary_next_day))
            }
        }
    )
}

/** Быстрый выбор приёма пищи по кнопке «+». Предлагаемый по времени — первым. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    date: LocalDate,
    onDismiss: () -> Unit,
    onMealSelected: (MealType) -> Unit,
    onScan: (MealType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val suggested = remember { defaultMealForNow() }
    val ordered = remember(suggested) { listOf(suggested) + MealType.entries.filter { it != suggested } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                stringResource(R.string.diary_add_to, date.formatFull()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(ordered, key = { it.name }) { meal ->
                    ListItem(
                        modifier = Modifier
                            .clickable { onMealSelected(meal) }
                            .padding(horizontal = 8.dp),
                        leadingContent = { Icon(meal.icon, contentDescription = null) },
                        headlineContent = { Text(stringResource(meal.titleRes), style = MaterialTheme.typography.titleMedium) },
                        supportingContent = if (meal == suggested) {
                            { Text(stringResource(R.string.diary_suggested_now)) }
                        } else null,
                        trailingContent = {
                            IconButton(onClick = { onScan(meal) }) {
                                Icon(Icons.Filled.QrCodeScanner, stringResource(R.string.diary_scan_barcode))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.diary_sheet_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
