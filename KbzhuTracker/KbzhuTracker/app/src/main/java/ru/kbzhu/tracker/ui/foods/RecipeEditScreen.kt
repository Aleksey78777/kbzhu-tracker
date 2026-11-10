package ru.kbzhu.tracker.ui.foods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.components.NumberField
import ru.kbzhu.tracker.ui.components.PhotoSection
import ru.kbzhu.tracker.ui.components.PrimaryButton
import ru.kbzhu.tracker.ui.components.SecondaryButton
import ru.kbzhu.tracker.ui.components.rememberPhotoPicker
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.formatMacro
import ru.kbzhu.tracker.ui.util.messageRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RecipeEditEvent.Saved -> onSaved()
                RecipeEditEvent.Deleted -> onDeleted()
                is RecipeEditEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.res))
            }
        }
    }

    val picker = rememberPhotoPicker(
        createCameraUri = viewModel::createCameraUri,
        onCameraCancelled = viewModel::discardCameraUri,
        onPicked = viewModel::onPhotoPicked,
        onPermissionDenied = viewModel::onCameraPermissionDenied
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.recipe_new_title else R.string.recipe_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    loading = state.isSaving,
                    enabled = !state.isPhotoProcessing,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text(stringResource(R.string.recipe_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Ингредиенты ----------
            Text(stringResource(R.string.recipe_ingredients), style = MaterialTheme.typography.titleMedium)
            if (state.ingredients.isEmpty()) {
                Text(
                    stringResource(R.string.recipe_no_ingredients),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    state.ingredients.forEachIndexed { index, ing ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        IngredientRow(
                            ingredient = ing,
                            onGrams = { viewModel.onIngredientGrams(index, it) },
                            onRemove = { viewModel.removeIngredient(index) }
                        )
                    }
                }
            }
            SecondaryButton(
                text = stringResource(R.string.recipe_add_ingredient),
                onClick = { showPicker = true },
                icon = Icons.Filled.Add
            )

            // ---------- Выход и порции ----------
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = state.cookedWeight,
                    onValueChange = viewModel::onCookedWeight,
                    label = stringResource(R.string.recipe_cooked_weight),
                    modifier = Modifier.weight(1.4f),
                    suffix = stringResource(R.string.unit_g),
                    supportingText = stringResource(R.string.recipe_raw_weight, state.rawWeight.formatInt())
                )
                NumberField(
                    value = state.portions,
                    onValueChange = viewModel::onPortions,
                    label = stringResource(R.string.recipe_portions),
                    modifier = Modifier.weight(1f),
                    decimal = false
                )
            }

            RecipeSummary(state)

            PhotoSection(
                photoPath = state.photoPath,
                isProcessing = state.isPhotoProcessing,
                onTakePhoto = picker.takePhoto,
                onPickGallery = picker.pickFromGallery,
                onRemove = viewModel::removePhoto
            )

            state.error?.let {
                Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPicker) {
        IngredientPickerSheet(
            viewModel = viewModel,
            onDismiss = { showPicker = false },
            onPicked = { food ->
                viewModel.addIngredient(food)
                showPicker = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.recipe_delete_title)) },
            text = { Text(stringResource(R.string.recipe_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientInput, onGrams: (String) -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(ingredient.food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.value_kcal, ingredient.nutrition.calories.formatInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = ingredient.gramsText,
            onValueChange = onGrams,
            singleLine = true,
            suffix = { Text(stringResource(R.string.unit_g)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(110.dp)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, stringResource(R.string.recipe_remove_ingredient))
        }
    }
}

@Composable
private fun RecipeSummary(state: RecipeEditUiState) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.recipe_summary), style = MaterialTheme.typography.titleMedium)
            SummaryLine(
                stringResource(R.string.recipe_total, state.totalWeight.formatInt()),
                state.total.calories, state.total.protein, state.total.fat, state.total.carbs
            )
            SummaryLine(
                stringResource(R.string.detail_per100),
                state.per100.calories, state.per100.protein, state.per100.fat, state.per100.carbs
            )
            SummaryLine(
                stringResource(R.string.recipe_per_portion, (state.totalWeight / state.portionsCount).formatInt()),
                state.perPortion.calories, state.perPortion.protein, state.perPortion.fat, state.perPortion.carbs
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, kcal: Double, p: Double, f: Double, c: Double) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            "${kcal.formatInt()} ${stringResource(R.string.unit_kcal)} · " +
                stringResource(R.string.macro_line, p.formatMacro(), f.formatMacro(), c.formatMacro()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientPickerSheet(
    viewModel: RecipeEditViewModel,
    onDismiss: () -> Unit,
    onPicked: (Food) -> Unit
) {
    val query by viewModel.pickerQuery.collectAsStateWithLifecycle()
    val results by viewModel.pickerResults.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxHeight(0.9f)
                .imePadding()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onPickerQuery,
                placeholder = { Text(stringResource(R.string.foods_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            if (query.isBlank()) {
                Text(
                    stringResource(R.string.foods_tab_recent),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(results, key = { it.id }) { food ->
                    FoodRow(food = food, onClick = { onPicked(food) })
                }
                if (results.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (query.isBlank()) R.string.recipe_picker_hint else R.string.foods_nothing_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}
