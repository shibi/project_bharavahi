package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreOrganizationRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : OrganizationRepository {

    private val organizations get() = db.collection(FirestorePaths.ORGANIZATIONS)

    override fun observeAll(): Flow<List<Organization>> =
        organizations.snapshots()
            .map { snapshot ->
                snapshot.documents
                    .map { it.toOrganization() }
                    .filter { it.status != OrganizationStatus.DELETED }
                    .sortedBy { it.name.lowercase() }
            }
            .catch { throw it.toDomainException() }

    override fun observe(orgId: String): Flow<Organization?> =
        organizations.document(orgId).snapshots()
            .map { doc -> if (doc.exists()) doc.toOrganization() else null }
            .catch { throw it.toDomainException() }

    override suspend fun create(details: OrganizationDetails, firstAdmin: OrgAdmin): String = firebaseCall {
        val actorUid = auth.currentUser?.uid ?: throw DomainException.NotAuthorized()
        val orgRef = organizations.document()
        val now = FieldValue.serverTimestamp()

        db.batch().apply {
            set(
                orgRef,
                details.toMap() + mapOf(
                    FirestorePaths.Org.STATUS to OrganizationStatus.ACTIVE.toFirestore(),
                    FirestorePaths.Org.MEMBER_COUNT to 0L,
                    FirestorePaths.Org.EVENT_COUNT to 0L,
                    FirestorePaths.Org.CREATED_AT to now,
                ),
            )
            set(
                orgRef.collection(FirestorePaths.ADMINS).document(firstAdmin.uid),
                mapOf(
                    FirestorePaths.Admin.NAME to firstAdmin.name,
                    FirestorePaths.Admin.EMAIL to firstAdmin.email,
                    FirestorePaths.Admin.PHONE to firstAdmin.phone,
                    FirestorePaths.Admin.ACTIVE to true,
                    FirestorePaths.Admin.ADDED_AT to now,
                    FirestorePaths.Admin.ADDED_BY to actorUid,
                ),
            )
            set(
                db.collection(FirestorePaths.ADMIN_INDEX).document(firstAdmin.uid),
                mapOf(
                    FirestorePaths.Index.ORG_ID to orgRef.id,
                    FirestorePaths.Index.ACTIVE to true,
                ),
            )
        }.commit().await()
        orgRef.id
    }

    override suspend fun update(orgId: String, details: OrganizationDetails) = firebaseCall {
        organizations.document(orgId).update(details.toMap()).await()
        Unit
    }

    override suspend fun setStatus(orgId: String, status: OrganizationStatus) = firebaseCall {
        val orgRef = organizations.document(orgId)
        val batch = db.batch()
        batch.update(orgRef, FirestorePaths.Org.STATUS, status.toFirestore())
        if (status == OrganizationStatus.DELETED) {
            val admins = orgRef.collection(FirestorePaths.ADMINS).get().await()
            admins.documents.forEach { admin ->
                batch.update(admin.reference, FirestorePaths.Admin.ACTIVE, false)
                batch.update(
                    db.collection(FirestorePaths.ADMIN_INDEX).document(admin.id),
                    FirestorePaths.Index.ACTIVE,
                    false,
                )
            }
        }
        batch.commit().await()
        Unit
    }

    private fun OrganizationDetails.toMap(): Map<String, Any> = mapOf(
        FirestorePaths.Org.NAME to name,
        FirestorePaths.Org.TYPE to type,
        FirestorePaths.Org.ADDRESS to address,
        FirestorePaths.Org.CONTACT_PHONE to contactPhone,
    )
}
