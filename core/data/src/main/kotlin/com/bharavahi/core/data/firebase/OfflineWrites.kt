package com.bharavahi.core.data.firebase

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * NFR-1 and NFR-2: a Firestore write finishes only when the server confirms it, which never happens offline.
 * The local cache already shows the change, so wait briefly for an early failure (such as a refused
 * permission) and otherwise let the write sync in the background.
 */
internal suspend fun Task<Void>.awaitWrite(timeoutMillis: Long = WRITE_WAIT_MILLIS) {
    withTimeoutOrNull(timeoutMillis) { await() }
}

private const val WRITE_WAIT_MILLIS = 2_500L
