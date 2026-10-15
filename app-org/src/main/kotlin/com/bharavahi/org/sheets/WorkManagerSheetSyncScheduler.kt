package com.bharavahi.org.sheets

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bharavahi.core.domain.repository.GoogleAccountAuth
import com.bharavahi.core.domain.repository.SheetSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ORG-38: sheet syncs wait for a network connection and survive the app being closed. Each event has one
 * pending sync; a newer request replaces it after a short delay, so a burst of payments is one rewrite.
 */
class WorkManagerSheetSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SheetSyncScheduler {

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun enqueue(orgId: String, eventId: String) = enqueueUnique(
        "sheet-sync-$eventId",
        workDataOf(SheetSyncWorker.KEY_ORG_ID to orgId, SheetSyncWorker.KEY_EVENT_ID to eventId),
        DEBOUNCE_SECONDS,
    )

    override fun enqueueAll() = enqueueUnique("sheet-sync-all", workDataOf(SheetSyncWorker.KEY_FORCE to true), 0)

    /** Hourly catch-up, mainly for payments co-admins record on their own phones. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SheetSyncWorker>(1, TimeUnit.HOURS).setConstraints(online).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("sheet-sync-periodic", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun enqueueUnique(name: String, input: Data, delaySeconds: Long) {
        val request = OneTimeWorkRequestBuilder<SheetSyncWorker>()
            .setConstraints(online)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val DEBOUNCE_SECONDS = 10L
        const val BACKOFF_SECONDS = 30L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SheetsModule {
    @Binds abstract fun sheetSyncScheduler(impl: WorkManagerSheetSyncScheduler): SheetSyncScheduler
    @Binds abstract fun googleAccountAuth(impl: GisGoogleAccountAuth): GoogleAccountAuth
}
