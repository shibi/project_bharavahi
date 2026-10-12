package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.repository.OrgAdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreOrgAdminRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : OrgAdminRepository {

    override fun observeAdmins(orgId: String): Flow<List<OrgAdmin>> =
        db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
            .collection(FirestorePaths.ADMINS)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toOrgAdmin(orgId) }.sortedBy { it.name.lowercase() } }
            .catch { throw it.toDomainException() }

    /** Updates the admin record and the login index together, so the rules see the change at once. */
    override suspend fun setActive(orgId: String, uid: String, active: Boolean) = firebaseCall {
        db.batch().apply {
            update(
                db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
                    .collection(FirestorePaths.ADMINS).document(uid),
                FirestorePaths.Admin.ACTIVE,
                active,
            )
            update(db.collection(FirestorePaths.ADMIN_INDEX).document(uid), FirestorePaths.Index.ACTIVE, active)
        }.commit().await()
        Unit
    }
}
