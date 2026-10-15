package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.SheetsConnection
import com.bharavahi.core.domain.model.SheetsStatus
import com.bharavahi.core.domain.repository.SheetsConfigRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreSheetsConfigRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : SheetsConfigRepository {

    private fun doc(orgId: String) = db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
        .collection(FirestorePaths.CONFIG).document(FirestorePaths.SHEETS)

    override fun observe(orgId: String): Flow<SheetsConnection?> =
        doc(orgId).snapshots().map { it.toConnection() }.catch { throw it.toDomainException() }

    override suspend fun get(orgId: String): SheetsConnection? = firebaseCall { doc(orgId).get().await().toConnection() }

    override suspend fun save(orgId: String, connection: SheetsConnection) = firebaseCall {
        doc(orgId).set(
            mapOf(
                FirestorePaths.Sheets.GOOGLE_EMAIL to connection.googleEmail,
                FirestorePaths.Sheets.CONNECTED_BY to connection.connectedBy,
                FirestorePaths.Sheets.STATUS to connection.status.toFirestoreName(),
                FirestorePaths.Sheets.CONNECTED_AT to FieldValue.serverTimestamp(),
            ),
        ).awaitWrite()
    }

    override suspend fun setStatus(orgId: String, status: SheetsStatus) = firebaseCall {
        doc(orgId).update(FirestorePaths.Sheets.STATUS, status.toFirestoreName()).awaitWrite()
    }

    override suspend fun clear(orgId: String) = firebaseCall { doc(orgId).delete().awaitWrite() }

    private fun DocumentSnapshot.toConnection(): SheetsConnection? {
        val email = getString(FirestorePaths.Sheets.GOOGLE_EMAIL)
        if (!exists() || email.isNullOrBlank()) return null
        return SheetsConnection(
            googleEmail = email,
            connectedBy = getString(FirestorePaths.Sheets.CONNECTED_BY).orEmpty(),
            status = getString(FirestorePaths.Sheets.STATUS).toEnumOrNull<SheetsStatus>() ?: SheetsStatus.CONNECTED,
        )
    }
}
