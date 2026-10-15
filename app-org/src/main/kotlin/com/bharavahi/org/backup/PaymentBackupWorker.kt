package com.bharavahi.org.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bharavahi.core.domain.usecase.payment.SendPaymentBackupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/** ORG-28 and ORG-29: emails the payment backup. Retries with backoff; after the last attempt marks it failed. */
@HiltWorker
class PaymentBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendBackup: SendPaymentBackupUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val orgId = inputData.getString(KEY_ORG_ID) ?: return Result.failure()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val paymentId = inputData.getString(KEY_PAYMENT_ID) ?: return Result.failure()
        return try {
            sendBackup(orgId, eventId, paymentId, LocalDate.now(), LocalDateTime.now().format(RECORDED_AT))
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Backup email for payment $paymentId failed (attempt ${runAttemptCount + 1} of $MAX_ATTEMPTS)", e)
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                runCatching { sendBackup.markFailed(orgId, eventId, paymentId) }
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val KEY_ORG_ID = "orgId"
        const val KEY_EVENT_ID = "eventId"
        const val KEY_PAYMENT_ID = "paymentId"
        const val MAX_ATTEMPTS = 5
        private const val TAG = "PaymentBackup"
        private val RECORDED_AT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    }
}
