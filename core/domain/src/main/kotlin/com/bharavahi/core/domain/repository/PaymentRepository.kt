package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.NewPayment
import com.bharavahi.core.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    /** Payments of an event, newest first. */
    fun observeAll(orgId: String, eventId: String): Flow<List<Payment>>

    /**
     * BR-7: one batched write (works offline) that creates the payment and increments
     * eventMember.totalPaid, eventMember.paidByDue.{scheduleId}_{dueIndex} and event.totalCollected,
     * and locks the schedule. Returns the payment id.
     */
    suspend fun record(orgId: String, eventId: String, payment: NewPayment, reversesPaymentId: String? = null): String

    suspend fun setEmailStatus(orgId: String, eventId: String, paymentId: String, status: EmailStatus)
}
