package com.bharavahi.org.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bharavahi.core.domain.repository.PaymentBackupScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** NFR-3: the email waits for a network connection and survives the app being closed. */
class WorkManagerPaymentBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : PaymentBackupScheduler {

    override fun enqueue(orgId: String, eventId: String, paymentId: String) {
        val request = OneTimeWorkRequestBuilder<PaymentBackupWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    PaymentBackupWorker.KEY_ORG_ID to orgId,
                    PaymentBackupWorker.KEY_EVENT_ID to eventId,
                    PaymentBackupWorker.KEY_PAYMENT_ID to paymentId,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("payment-backup-$paymentId", ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds abstract fun paymentBackupScheduler(impl: WorkManagerPaymentBackupScheduler): PaymentBackupScheduler
}
