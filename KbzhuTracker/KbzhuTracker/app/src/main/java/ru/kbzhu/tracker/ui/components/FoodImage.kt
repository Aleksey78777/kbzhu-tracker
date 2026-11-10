package ru.kbzhu.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import java.io.File

/**
 * Картинка продукта/записи. [model] — локальный путь к файлу или URL из Open Food Facts.
 * Без картинки показываем нейтральную иконку.
 */
@Composable
fun FoodImage(
    model: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = MaterialTheme.shapes.small
) {
    val data: Any? = when {
        model.isNullOrBlank() -> null
        model.startsWith("http") -> model
        else -> File(model)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (data == null) {
            PlaceholderIcon(size)
        } else {
            SubcomposeAsyncImage(
                model = data,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { PlaceholderIcon(size) },
                error = { PlaceholderIcon(size) }
            )
        }
    }
}

@Composable
private fun PlaceholderIcon(size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(size * 0.45f),
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}
