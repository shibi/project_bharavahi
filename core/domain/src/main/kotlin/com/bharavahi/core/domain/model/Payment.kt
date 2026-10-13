package com.bharavahi.core.domain.model

import java.time.LocalDate

/** Money received from a member towards one due. Immutable once saved, except [emailStatus] (BR-11). */
data class Payment(
    val id: String,
    val memberId: String,
    val scheduleId: String,
    val dueIndex: Int,
    /** Paise. Negative only on a reversing entry (ORG-25). */
    val amount: Long,
    val method: PaymentMethod,
    val note: String,
    val paidOn: LocalDate?,
    val recordedBy: String,
    val createdAtMillis: Long?,
    val reversesPaymentId: String?,
    val emailStatus: EmailStatus,
    /** Saved on this phone but not yet on the server (NFR-2). */
    val waitingToSync: Boolean,
) {
    val isReversal: Boolean get() = reversesPaymentId != null
}

enum class PaymentMethod { ONLINE, OFFLINE }

enum class EmailStatus { PENDING, SENT, FAILED }

/** Input for ORG-21. Amount is in paise. */
data class NewPayment(
    val memberId: String,
    val scheduleId: String,
    val dueIndex: Int,
    val amount: Long,
    val method: PaymentMethod,
    val note: String,
    val paidOn: LocalDate,
)
