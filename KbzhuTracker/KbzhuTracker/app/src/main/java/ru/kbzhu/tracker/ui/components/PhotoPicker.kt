package ru.kbzhu.tracker.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ru.kbzhu.tracker.R

/** Открывает галерею или камеру. Создаётся через [rememberPhotoPicker]. */
class PhotoPickerController(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit
)

/**
 * Галерея — системный Photo Picker (разрешений не требует).
 * Камера — системное приложение камеры через FileProvider. Т.к. в манифесте объявлено
 * разрешение CAMERA (оно нужно сканеру), Android требует его и для съёмки — запрашиваем.
 *
 * @param createCameraUri создаёт URI временного файла (PhotoStorage.createCameraUri)
 * @param onPicked URI выбранного/снятого изображения — передайте в PhotoStorage.importPhoto
 */
@Composable
fun rememberPhotoPicker(
    createCameraUri: () -> String,
    onCameraCancelled: (String) -> Unit,
    onPicked: (String) -> Unit,
    onPermissionDenied: () -> Unit
): PhotoPickerController {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnCancelled by rememberUpdatedState(onCameraCancelled)
    val currentOnDenied by rememberUpdatedState(onPermissionDenied)
    val currentCreateUri by rememberUpdatedState(createCameraUri)

    // URI переживает пересоздание Activity, пока открыта камера
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { currentOnPicked(it.toString()) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (uri != null) {
            if (success) currentOnPicked(uri) else currentOnCancelled(uri)
        }
    }

    fun launchCamera() {
        val uri = currentCreateUri()
        pendingCameraUri = uri
        cameraLauncher.launch(Uri.parse(uri))
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else currentOnDenied()
    }

    return remember(galleryLauncher, cameraLauncher, permissionLauncher) {
        PhotoPickerController(
            pickFromGallery = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            takePhoto = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }
}

/** Блок «Фото»: превью и кнопки «Камера» / «Галерея». */
@Composable
fun PhotoSection(
    photoPath: String?,
    isProcessing: Boolean,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.photo_title)
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                FoodImage(model = photoPath, size = 96.dp, shape = MaterialTheme.shapes.large)
                if (isProcessing) {
                    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                    }
                }
                if (photoPath != null && !isProcessing) {
                    FilledTonalIconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.Close, stringResource(R.string.photo_remove), Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = onTakePhoto, enabled = !isProcessing, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    Icon(Icons.Filled.PhotoCamera, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.photo_camera))
                }
                OutlinedButton(onClick = onPickGallery, enabled = !isProcessing, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.photo_gallery))
                }
            }
        }
    }
}
