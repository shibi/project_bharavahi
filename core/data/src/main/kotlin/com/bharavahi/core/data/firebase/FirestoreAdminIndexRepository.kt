package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.AdminLink
import com.bharavahi.core.domain.repository.AdminIndexRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreAdminIndexRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : AdminIndexRepository {

    private fun doc(uid: String) = db.collection(FirestorePaths.ADMIN_INDEX).document(uid)

    override suspend fun find(uid: String): AdminLink? = firebaseCall { doc(uid).get().await().toAdminLink() }

    override fun observe(uid: String): Flow<AdminLink?> =
        doc(uid).snapshots()
            .map { it.toAdminLink() }
            .catch { throw it.toDomainException() }

    private fun DocumentSnapshot.toAdminLink(): AdminLink? =
        if (!exists()) {
            null
        } else {
            AdminLink(
                orgId = getString(FirestorePaths.Index.ORG_ID).orEmpty(),
                active = getBoolean(FirestorePaths.Index.ACTIVE) ?: false,
            )
        }
}
