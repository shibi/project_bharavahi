package com.bharavahi.org.sheets

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.usecase.sheets.SyncAllSheetsUseCase
import com.bharavahi.core.domain.usecase.sheets.SyncEventSheetUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

/** ORG-38: updates one event's Google Sheet, or every event's when no event is given. */
@HiltWorker
class SheetSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncEvent: SyncEventSheetUseCase,
    private val syncAll: SyncAllSheetsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
        val force = inputData.getBoolean(KEY_FORCE, false)
        return try {
            if (eventId == null) {
                syncAll(force)
            } else {
                syncEvent(inputData.getString(KEY_ORG_ID) ?: return Result.failure(), eventId, force)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainException.NotAuthorized) {
            // Signed out, or Google access removed (Profile now asks to reconnect). Retrying won't help.
            Result.failure()
        } catch (e: Exception) {
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val KEY_ORG_ID = "orgId"
        const val KEY_EVENT_ID = "eventId"
        const val KEY_FORCE = "force"
        const val MAX_ATTEMPTS = 5
    }
}
