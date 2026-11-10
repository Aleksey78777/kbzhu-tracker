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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.components.NumberField
import ru.kbzhu.tracker.ui.components.PhotoSection
import ru.kbzhu.tracker.ui.components.PrimaryButton
import ru.kbzhu.tracker.ui.components.rememberPhotoPicker
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.messageRes
import ru.kbzhu.tracker.ui.util.parseDecimal
import ru.kbzhu.tracker.ui.util.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditScreen(
    onBack: () -> Unit,
    onSaved: (foodId: Long, isNew: Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: FoodEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FoodEditEvent.Saved -> onSaved(event.id, event.isNew)
                FoodEditEvent.Deleted -> onDeleted()
                is FoodEditEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.res))
            }
        }
    }

    val picker = rememberPhotoPicker(
        createCameraUri = viewModel::createCameraUri,
        onCameraCancelled = viewModel::discardCameraUri,
        onPicked = viewModel::onPhotoPicked,
        onPermissionDenied = viewModel::onCameraPermissionDenied
    )

    val canDelete = state.original?.isUserEditable == true && !state.isNew

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.edit_new_title else R.string.edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (canDelete) {
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
            if (state.original?.isUserEditable == false) {
                Text(
                    stringResource(R.string.edit_builtin_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text(stringResource(R.string.edit_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) ({ Text(stringResource(R.string.error_empty_name)) }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::onBrand,
                label = { Text(stringResource(R.string.edit_brand)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            CategoryDropdown(state.category, viewModel::onCategory)

            Text(stringResource(R.string.edit_per100_title), style = MaterialTheme.typography.titleMedium)
            NumberField(
                value = state.calories,
                onValueChange = viewModel::onCalories,
                label = stringResource(R.string.calories),
                suffix = stringResource(R.string.unit_kcal),
                errorText = if (state.caloriesError) stringResource(R.string.edit_calories_error) else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(state.protein, viewModel::onProtein, stringResource(R.string.protein), Modifier.weight(1f), suffix = stringResource(R.string.unit_g))
                NumberField(state.fat, viewModel::onFat, stringResource(R.string.fat), Modifier.weight(1f), suffix = stringResource(R.string.unit_g))
                NumberField(state.carbs, viewModel::onCarbs, stringResource(R.string.carbs), Modifier.weight(1f), suffix = stringResource(R.string.unit_g))
            }
            // Подсказка: посчитать ккал по БЖУ, если отличается от введённого
            state.caloriesFromMacros?.let { fromMacros ->
                if (state.calories.parseDecimal()?.toInt() != fromMacros) {
                    AssistChip(
                        onClick = viewModel::applyCaloriesFromMacros,
                        label = { Text(stringResource(R.string.edit_kcal_from_macros, fromMacros.formatInt())) },
                        leadingIcon = { Icon(Icons.Filled.AutoFixHigh, null) }
                    )
                }
            }

            Text(stringResource(R.string.edit_serving_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.servingName,
                    onValueChange = viewModel::onServingName,
                    label = { Text(stringResource(R.string.edit_serving_name)) },
                    placeholder = { Text(stringResource(R.string.edit_serving_name_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1.3f)
                )
                NumberField(
                    state.servingSize, viewModel::onServingSize, stringResource(R.string.edit_serving_size),
                    Modifier.weight(1f), suffix = stringResource(R.string.unit_g)
                )
            }

            OutlinedTextField(
                value = state.barcode,
                onValueChange = viewModel::onBarcode,
                label = { Text(stringResource(R.string.edit_barcode)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            PhotoSection(
                photoPath = state.photoPath,
                isProcessing = state.isPhotoProcessing,
                onTakePhoto = picker.takePhoto,
                onPickGallery = picker.pickFromGallery,
                onRemove = viewModel::removePhoto
            )

            state.error?.let {
                Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.edit_delete_title)) },
            text = { Text(stringResource(R.string.edit_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(selected: FoodCategory, onSelect: (FoodCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(selected.titleRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.edit_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FoodCategory.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(stringResource(c.titleRes)) },
                    onClick = { onSelect(c); expanded = false }
                )
            }
        }
    }
}
