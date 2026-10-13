package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.domain.model.NewExpense
import com.bharavahi.core.domain.repository.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class FirestoreExpenseRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ExpenseRepository {

    private fun event(orgId: String, eventId: String) =
        db.collection(FirestorePaths.ORGANIZATIONS).document(orgId).collection(FirestorePaths.EVENTS).document(eventId)

    override fun observeAll(orgId: String, eventId: String): Flow<List<Expense>> =
        event(orgId, eventId).collection(FirestorePaths.EXPENSES).snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.map { it.toExpense() }
                    .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAtMillis ?: Long.MAX_VALUE })
            }
            .catch { throw it.toDomainException() }

    override suspend fun add(orgId: String, eventId: String, expense: NewExpense): String = firebaseCall {
        val actorUid = auth.currentUser?.uid ?: throw DomainException.NotAuthorized()
        val eventRef = event(orgId, eventId)
        val ref = eventRef.collection(FirestorePaths.EXPENSES).document()
        db.batch().apply {
            set(
                ref,
                mapOf(
                    FirestorePaths.Expense.TITLE to expense.title,
                    FirestorePaths.Expense.AMOUNT to expense.amount,
                    FirestorePaths.Expense.DATE to expense.date.toFirestore(),
                    FirestorePaths.Expense.DESCRIPTION to expense.description,
                    FirestorePaths.Expense.PAID_BY_MEMBER_ID to expense.paidByMemberId,
                    FirestorePaths.Expense.RECORDED_BY to actorUid,
                    FirestorePaths.Expense.CREATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            update(eventRef, FirestorePaths.Event.TOTAL_EXPENSES, FieldValue.increment(expense.amount))
        }.commit().awaitWrite()
        ref.id
    }
}
