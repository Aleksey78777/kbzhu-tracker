package ru.kbzhu.tracker.ui.foods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.ui.components.EmptyState
import ru.kbzhu.tracker.ui.components.FoodImage
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.components.PhotoSection
import ru.kbzhu.tracker.ui.components.PrimaryButton
import ru.kbzhu.tracker.ui.components.rememberPhotoPicker
import ru.kbzhu.tracker.ui.theme.AppTheme
import ru.kbzhu.tracker.ui.theme.BigNumberStyle
import ru.kbzhu.tracker.ui.util.format1
import ru.kbzhu.tracker.ui.util.formatDayMonth
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.formatMacro
import ru.kbzhu.tracker.ui.util.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    onEditFood: (Food) -> Unit,
    onDone: () -> Unit,
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                FoodDetailEvent.Done -> onDone()
                is FoodDetailEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.res))
            }
        }
    }

    val picker = rememberPhotoPicker(
        createCameraUri = viewModel::createCameraUri,
        onCameraCancelled = viewModel::discardCameraUri,
        onPicked = viewModel::onPhotoPicked,
        onPermissionDenied = viewModel::onCameraPermissionDenied
    )

    val food = state.food
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEditingEntry) R.string.detail_edit_entry else R.string.detail_add_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (food != null && food.isSaved) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            if (food.isFavorite) Icon(Icons.Filled.Star, stringResource(R.string.food_unfavorite), tint = MaterialTheme.colorScheme.tertiary)
                            else Icon(Icons.Outlined.StarBorder, stringResource(R.string.food_favorite))
                        }
                        if (food.isUserEditable) {
                            IconButton(onClick = { onEditFood(food) }) { Icon(Icons.Filled.Edit, stringResource(R.string.detail_edit_food)) }
                        }
                    }
                    if (state.isEditingEntry) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (food != null) {
                Surface(tonalElevation = 3.dp) {
                    PrimaryButton(
                        text = if (state.isEditingEntry) stringResource(R.string.action_save)
                        else stringResource(R.string.detail_add_to, stringResource(state.meal.titleRes)),
                        onClick = viewModel::save,
                        enabled = state.canSave,
                        loading = state.isSaving,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            state.notFound || food == null -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.detail_not_found),
                message = stringResource(R.string.detail_not_found_message),
                modifier = Modifier.padding(padding),
                actionText = stringResource(R.string.action_back),
                onAction = onBack
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FoodHeader(food)

                if (state.isIncomplete) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.detail_incomplete),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            if (food.isSaved && food.isUserEditable) {
                                TextButton(onClick = { onEditFood(food) }) { Text(stringResource(R.string.action_edit)) }
                            }
                        }
                    }
                }

                AmountSection(
                    state = state,
                    onMode = viewModel::setMode,
                    onGrams = viewModel::onGramsChange,
                    onServings = viewModel::onServingsChange,
                    onQuick = viewModel::setQuickAmount
                )

                NutritionResult(state.nutrition)

                MealSelector(selected = state.meal, onSelect = viewModel::selectMeal)
                if (!state.isEditingEntry) {
                    Text(
                        stringResource(R.string.detail_date, state.date.formatDayMonth()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PhotoSection(
                    title = stringResource(R.string.detail_entry_photo),
                    photoPath = state.photoPath,
                    isProcessing = state.isPhotoProcessing,
                    onTakePhoto = picker.takePhoto,
                    onPickGallery = picker.pickFromGallery,
                    onRemove = viewModel::removePhoto
                )

                if (state.ingredients.isNotEmpty()) {
                    IngredientsInfo(state)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_entry_title)) },
            text = { Text(stringResource(R.string.detail_delete_entry_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteEntry() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun FoodHeader(food: Food) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FoodImage(food.displayImage, size = 80.dp, shape = MaterialTheme.shapes.large)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.titleLarge)
            food.brand?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.detail_per100) + ": " + per100Line(food.per100),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSection(
    state: FoodDetailUiState,
    onMode: (AmountMode) -> Unit,
    onGrams: (String) -> Unit,
    onServings: (String) -> Unit,
    onQuick: (Double) -> Unit
) {
    val food = state.food ?: return
    val servingSize = food.servingSize
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.detail_amount), style = MaterialTheme.typography.titleMedium)

        if (servingSize != null) {
            val servingLabel = food.servingName ?: stringResource(R.string.detail_serving)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.mode == AmountMode.SERVINGS,
                    onClick = { onMode(AmountMode.SERVINGS) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text(stringResource(R.string.detail_mode_servings, servingLabel, servingSize.formatInt())) }
                )
                SegmentedButton(
                    selected = state.mode == AmountMode.GRAMS,
                    onClick = { onMode(AmountMode.GRAMS) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text(stringResource(R.string.detail_mode_grams)) }
                )
            }
        }

        val isServings = state.mode == AmountMode.SERVINGS
        OutlinedTextField(
            value = if (isServings) state.servingsText else state.gramsText,
            onValueChange = if (isServings) onServings else onGrams,
            label = { Text(stringResource(if (isServings) R.string.detail_servings_count else R.string.detail_grams)) },
            suffix = {
                Text(
                    if (isServings) stringResource(R.string.detail_servings_suffix)
                    else stringResource(R.string.unit_g)
                )
            },
            supportingText = {
                when {
                    !state.isAmountValid -> Text(stringResource(R.string.detail_amount_error))
                    isServings -> Text(stringResource(R.string.detail_equals_grams, (state.grams ?: 0.0).formatInt()))
                    else -> Unit
                }
            },
            isError = !state.isAmountValid,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quick = if (isServings) FoodDetailViewModel.QUICK_SERVINGS else FoodDetailViewModel.QUICK_GRAMS
            quick.forEach { value ->
                val current = if (isServings) state.servingsText else state.gramsText
                FilterChip(
                    selected = current == value.format1(),
                    onClick = { onQuick(value) },
                    label = { Text(if (isServings) "×${value.format1()}" else value.formatInt()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NutritionResult(n: Nutrition) {
    val colors = AppTheme.nutrients
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1.2f)) {
                Text(n.calories.formatInt(), style = BigNumberStyle, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.unit_kcal), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            MacroValue(stringResource(R.string.protein), n.protein, colors.protein, Modifier.weight(1f))
            MacroValue(stringResource(R.string.fat), n.fat, colors.fat, Modifier.weight(1f))
            MacroValue(stringResource(R.string.carbs), n.carbs, colors.carbs, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroValue(label: String, grams: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(grams.formatMacro(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun MealSelector(selected: MealType, onSelect: (MealType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.detail_meal), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { meal ->
                FilterChip(
                    selected = selected == meal,
                    onClick = { onSelect(meal) },
                    label = { Text(stringResource(meal.titleRes), maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IngredientsInfo(state: FoodDetailUiState) {
    val food = state.food ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.detail_ingredients), style = MaterialTheme.typography.titleMedium)
            food.recipeYield?.let { yield ->
                Text(
                    stringResource(R.string.detail_recipe_yield, yield.formatInt(), food.recipePortions ?: 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            state.ingredients.forEach { ing ->
                Row(Modifier.fillMaxWidth()) {
                    Text(ing.food.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.value_g, ing.grams.formatInt()), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
