package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberDetails
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.repository.MemberRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreMemberRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : MemberRepository {

    private fun org(orgId: String) = db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
    private fun members(orgId: String) = org(orgId).collection(FirestorePaths.MEMBERS)

    override fun observeAll(orgId: String): Flow<List<Member>> =
        members(orgId).snapshots()
            .map { snapshot -> snapshot.documents.map { it.toMember() }.sortedBy { it.name.lowercase() } }
            .catch { throw it.toDomainException() }

    override fun observe(orgId: String, memberId: String): Flow<Member?> =
        members(orgId).document(memberId).snapshots()
            .map { doc -> if (doc.exists()) doc.toMember() else null }
            .catch { throw it.toDomainException() }

    override suspend fun add(orgId: String, details: MemberDetails): String = firebaseCall {
        val ref = members(orgId).document()
        db.batch().apply {
            set(
                ref,
                details.toMap() + mapOf(
                    FirestorePaths.Member.STATUS to MemberStatus.ACTIVE.toFirestore(),
                    FirestorePaths.Member.JOINED_AT to FieldValue.serverTimestamp(),
                ),
            )
            update(org(orgId), FirestorePaths.Org.MEMBER_COUNT, FieldValue.increment(1))
        }.commit().awaitWrite()
        ref.id
    }

    override suspend fun update(orgId: String, memberId: String, details: MemberDetails) = firebaseCall {
        val batch = db.batch()
        batch.update(members(orgId).document(memberId), details.toMap(forUpdate = true))
        forEachEventMembership(orgId, memberId) { batch.update(it, FirestorePaths.EventMember.NAME, details.name) }
        batch.commit().awaitWrite()
        Unit
    }

    override suspend fun setStatus(orgId: String, memberId: String, status: MemberStatus) = firebaseCall {
        val batch = db.batch()
        batch.update(members(orgId).document(memberId), FirestorePaths.Member.STATUS, status.toFirestore())
        forEachEventMembership(orgId, memberId) {
            batch.update(it, FirestorePaths.EventMember.ACTIVE, status == MemberStatus.ACTIVE)
        }
        batch.commit().awaitWrite()
        Unit
    }

    /**
     * Calls [action] with every eventMembers doc of this member. Events the device can't read right now
     * (offline and not cached) are skipped, so the member change itself still goes through.
     */
    private suspend fun forEachEventMembership(orgId: String, memberId: String, action: (DocumentReference) -> Unit) {
        val events = runCatching { org(orgId).collection(FirestorePaths.EVENTS).get().await() }.getOrNull() ?: return
        events.documents.forEach { event ->
            val ref = event.reference.collection(FirestorePaths.EVENT_MEMBERS).document(memberId)
            val exists = runCatching { ref.get().await().exists() }.getOrDefault(false)
            if (exists) action(ref)
        }
    }

    /**
     * Empty optional extras are left out on add. On update ([forUpdate]) they are deleted instead,
     * so clearing a field in the form removes it from the document.
     */
    private fun MemberDetails.toMap(forUpdate: Boolean = false): Map<String, Any> {
        val extrasMap = mapOf(
            FirestorePaths.Member.DATE_OF_BIRTH to extras.dateOfBirth.toFirestore(),
            FirestorePaths.Member.GENDER to extras.gender?.toFirestoreName(),
            FirestorePaths.Member.BLOOD_GROUP to extras.bloodGroup?.toFirestoreName(),
            FirestorePaths.Member.JOB to extras.job.ifEmpty { null },
            FirestorePaths.Member.HOUSE_NAME to extras.houseName.ifEmpty { null },
            FirestorePaths.Member.SHIRT_SIZE to extras.shirtSize?.toFirestoreName(),
        )
        val extrasFields: Map<String, Any> = if (forUpdate) {
            extrasMap.mapValues { (_, value) -> value ?: FieldValue.delete() }
        } else {
            extrasMap.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        }
        return mapOf(
            FirestorePaths.Member.NAME to name,
            FirestorePaths.Member.NICKNAME to nickname,
            FirestorePaths.Member.PHONE to phone,
            FirestorePaths.Member.EMAIL to email,
        ) + extrasFields
    }
}
