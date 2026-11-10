package ru.kbzhu.tracker.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.ui.components.PrimaryButton
import ru.kbzhu.tracker.ui.components.SecondaryButton
import ru.kbzhu.tracker.ui.util.messageRes

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onFound: (Long) -> Unit,
    onAddManually: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        // Отказ без возможности повторного запроса → только через настройки
        permanentlyDenied = !granted && context.findActivity()
            ?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
    }

    // Пользователь мог выдать разрешение в настройках и вернуться
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = context.hasCameraPermission()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScannerEvent.Found -> onFound(event.foodId)
            }
        }
    }

    // Лёгкая вибрация, когда код распознан
    LaunchedEffect(state) {
        if (state is ScanState.Looking) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val analyzer = remember { BarcodeAnalyzer(viewModel::onBarcodeDetected) }
    DisposableEffect(analyzer) { onDispose { analyzer.close() } }
    LaunchedEffect(state) { analyzer.enabled = state is ScanState.Scanning }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !hasPermission -> PermissionContent(
                permanentlyDenied = permanentlyDenied,
                onRequest = {
                    if (permanentlyDenied) context.openAppSettings()
                    else permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onManual = { showManualDialog = true }
            )
            cameraError -> PermissionContent(
                permanentlyDenied = false,
                onRequest = { cameraError = false },
                onManual = { showManualDialog = true },
                titleRes = R.string.scanner_camera_error,
                messageRes = R.string.scanner_camera_error_message,
                actionRes = R.string.action_retry
            )
            else -> {
                CameraPreview(
                    analyzer = analyzer,
                    torchEnabled = torchOn,
                    onTorchAvailable = { torchAvailable = it },
                    onCameraError = { cameraError = true },
                    modifier = Modifier.fillMaxSize()
                )
                ScannerOverlay(active = state is ScanState.Scanning)
                StatusPanel(
                    state = state,
                    onRetry = viewModel::retry,
                    onScanAgain = viewModel::scanAgain,
                    onAddManually = onAddManually,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Верхняя панель поверх камеры
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
            }
            Text(
                stringResource(R.string.scanner_title),
                style = MaterialTheme.typography.titleMedium,
                // Поверх камеры — белый, на экране разрешения — обычный цвет текста
                color = if (hasPermission && !cameraError) Color.White else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            OverlayIconButton(onClick = { showManualDialog = true }) {
                Icon(Icons.Filled.Keyboard, stringResource(R.string.scanner_enter_manually))
            }
            if (hasPermission && torchAvailable) {
                Spacer(Modifier.width(8.dp))
                OverlayIconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        stringResource(if (torchOn) R.string.scanner_torch_off else R.string.scanner_torch_on)
                    )
                }
            }
        }
    }

    if (showManualDialog) {
        ManualBarcodeDialog(
            onDismiss = { showManualDialog = false },
            onSubmit = { code ->
                val ok = viewModel.submitManual(code)
                if (ok) showManualDialog = false
                ok
            }
        )
    }
}

@Composable
private fun OverlayIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White
        ),
        content = content
    )
}

/** Затемнение с «окном» под штрих-код, уголки и бегущая линия. */
@Composable
private fun ScannerOverlay(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val linePos by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "linePos"
    )
    val accent = MaterialTheme.colorScheme.primaryContainer
    Canvas(Modifier.fillMaxSize()) {
        val frameW = size.width * 0.8f
        val frameH = frameW * 0.6f
        val left = (size.width - frameW) / 2
        val top = size.height * 0.38f - frameH / 2
        val radius = 24.dp.toPx()
        val frame = RoundRect(left, top, left + frameW, top + frameH, CornerRadius(radius))

        // Затемнение вокруг окна (EvenOdd вырезает окно из прямоугольника экрана)
        val scrim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            addRoundRect(frame)
        }
        drawPath(scrim, Color.Black.copy(alpha = 0.55f))

        // Рамка
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(frameW, frameH),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = 3.dp.toPx())
        )

        if (active) {
            val y = top + frameH * linePos
            drawLine(
                color = accent,
                start = Offset(left + 16.dp.toPx(), y),
                end = Offset(left + frameW - 16.dp.toPx(), y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun StatusPanel(
    state: ScanState,
    onRetry: () -> Unit,
    onScanAgain: () -> Unit,
    onAddManually: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "scanStatus"
        ) { s ->
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (s) {
                    ScanState.Scanning -> {
                        Text(stringResource(R.string.scanner_hint_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.scanner_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ScanState.Looking -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.scanner_looking), style = MaterialTheme.typography.titleMedium)
                            Text(s.barcode, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is ScanState.NotFound -> {
                        StatusHeader(Icons.Filled.SearchOff, stringResource(R.string.scanner_not_found), s.barcode)
                        Text(
                            stringResource(R.string.scanner_not_found_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(stringResource(R.string.scanner_add_manually), onClick = { onAddManually(s.barcode) })
                        SecondaryButton(stringResource(R.string.scanner_scan_again), onClick = onScanAgain)
                    }
                    is ScanState.Error -> {
                        StatusHeader(Icons.Filled.WifiOff, stringResource(s.reason.messageRes), s.barcode)
                        PrimaryButton(stringResource(R.string.action_retry), onClick = onRetry)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onAddManually(s.barcode) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.scanner_add_manually), textAlign = TextAlign.Center)
                            }
                            TextButton(onClick = onScanAgain, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.scanner_scan_again), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, barcode: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(barcode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionContent(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onManual: () -> Unit,
    titleRes: Int = R.string.scanner_permission_title,
    messageRes: Int = if (permanentlyDenied) R.string.scanner_permission_denied else R.string.scanner_permission_message,
    actionRes: Int = if (permanentlyDenied) R.string.scanner_open_settings else R.string.scanner_allow
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.NoPhotography, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(actionRes), onClick = onRequest)
            Spacer(Modifier.height(8.dp))
            SecondaryButton(stringResource(R.string.scanner_enter_manually), onClick = onManual, icon = Icons.Filled.Keyboard)
        }
    }
}

@Composable
private fun ManualBarcodeDialog(onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scanner_enter_manually)) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { v -> if (v.length <= 13 && v.all { it.isDigit() }) { code = v; error = false } },
                label = { Text(stringResource(R.string.edit_barcode)) },
                singleLine = true,
                isError = error,
                supportingText = { Text(stringResource(if (error) R.string.scanner_invalid_code else R.string.scanner_code_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { error = !onSubmit(code) }, enabled = code.length >= 8) {
                Text(stringResource(R.string.scanner_find))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
