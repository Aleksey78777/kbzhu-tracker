package ru.kbzhu.tracker.ui.foods

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kbzhu.tracker.domain.repository.PhotoStorage

/**
 * Следит за фото, загруженными на экране, но ещё не сохранёнными.
 * Если пользователь заменил фото или ушёл без сохранения — лишние файлы удаляются,
 * чтобы во внутреннем хранилище не копился мусор.
 */
class PendingPhotoTracker(private val storage: PhotoStorage) {

    private val pending = mutableSetOf<String>()

    fun track(path: String) {
        pending += path
    }

    /** Фото заменили/убрали до сохранения: если оно новое — удаляем файл сразу. */
    suspend fun discardIfPending(path: String?) {
        if (path != null && pending.remove(path)) storage.delete(path)
    }

    /** После успешного сохранения: оставляем [kept], остальные новые файлы удаляем. */
    suspend fun commit(kept: String?) {
        val toDelete = pending.filter { it != kept }
        pending.clear()
        withContext(NonCancellable) { toDelete.forEach { storage.delete(it) } }
    }

    /** Ушли без сохранения (вызывать из ViewModel.onCleared). */
    fun discardAll() {
        if (pending.isEmpty()) return
        val toDelete = pending.toList()
        pending.clear()
        // viewModelScope к этому моменту уже отменён — используем отдельный короткоживущий scope
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            toDelete.forEach { storage.delete(it) }
        }
    }
}
