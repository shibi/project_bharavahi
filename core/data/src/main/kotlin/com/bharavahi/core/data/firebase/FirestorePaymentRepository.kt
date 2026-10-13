package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.NewPayment
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.repository.PaymentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class FirestorePaymentRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : PaymentRepository {

    private fun event(orgId: String, eventId: String) =
        db.collection(FirestorePaths.ORGANIZATIONS).document(orgId).collection(FirestorePaths.EVENTS).document(eventId)

    private fun payments(orgId: String, eventId: String) = event(orgId, eventId).collection(FirestorePaths.PAYMENTS)

    /** Includes metadata changes so a payment flips from "waiting to sync" to synced (NFR-2). */
    override fun observeAll(orgId: String, eventId: String): Flow<List<Payment>> =
        payments(orgId, eventId).snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.map { it.toPayment() }
                    // Unsynced payments have no server time yet; they are the newest.
                    .sortedByDescending { it.createdAtMillis ?: Long.MAX_VALUE }
            }
            .catch { throw it.toDomainException() }

    override suspend fun record(orgId: String, eventId: String, payment: NewPayment, reversesPaymentId: String?): String =
        firebaseCall {
            val actorUid = auth.currentUser?.uid ?: throw DomainException.NotAuthorized()
            val eventRef = event(orgId, eventId)
            val ref = payments(orgId, eventId).document()
            val memberRef = eventRef.collection(FirestorePaths.EVENT_MEMBERS).document(payment.memberId)
            val scheduleRef = eventRef.collection(FirestorePaths.SCHEDULES).document(payment.scheduleId)
            val dueField = "${FirestorePaths.EventMember.PAID_BY_DUE}.${Due.key(payment.scheduleId, payment.dueIndex)}"

            db.batch().apply {
                set(
                    ref,
                    mapOf(
                        FirestorePaths.Payment.MEMBER_ID to payment.memberId,
                        FirestorePaths.Payment.SCHEDULE_ID to payment.scheduleId,
                        FirestorePaths.Payment.DUE_INDEX to payment.dueIndex,
                        FirestorePaths.Payment.AMOUNT to payment.amount,
                        FirestorePaths.Payment.METHOD to payment.method.toFirestore(),
                        FirestorePaths.Payment.NOTE to payment.note,
                        FirestorePaths.Payment.PAID_ON to payment.paidOn.toString(),
                        FirestorePaths.Payment.RECORDED_BY to actorUid,
                        FirestorePaths.Payment.CREATED_AT to FieldValue.serverTimestamp(),
                        FirestorePaths.Payment.REVERSES_PAYMENT_ID to reversesPaymentId,
                        FirestorePaths.Payment.EMAIL_STATUS to EmailStatus.PENDING.toFirestore(),
                    ),
                )
                update(
                    memberRef,
                    mapOf(
                        FirestorePaths.EventMember.TOTAL_PAID to FieldValue.increment(payment.amount),
                        dueField to FieldValue.increment(payment.amount),
                    ),
                )
                update(eventRef, FirestorePaths.Event.TOTAL_COLLECTED, FieldValue.increment(payment.amount))
                update(scheduleRef, FirestorePaths.Schedule.LOCKED, true)
            }.commit().awaitWrite()
            ref.id
        }

    override suspend fun setEmailStatus(orgId: String, eventId: String, paymentId: String, status: EmailStatus) = firebaseCall {
        payments(orgId, eventId).document(paymentId)
            .update(FirestorePaths.Payment.EMAIL_STATUS, status.toFirestore())
            .awaitWrite()
    }
}
