package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.NewEvent
import com.bharavahi.core.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreEventRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : EventRepository {

    private fun org(orgId: String) = db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
    private fun events(orgId: String) = org(orgId).collection(FirestorePaths.EVENTS)
    private fun eventMembers(orgId: String, eventId: String) =
        events(orgId).document(eventId).collection(FirestorePaths.EVENT_MEMBERS)

    override fun observeAll(orgId: String): Flow<List<Event>> =
        events(orgId).snapshots()
            // A just-created event has no server time yet; keep it at the top.
            .map { snapshot -> snapshot.documents.map { it.toEvent() }.sortedByDescending { it.createdAtMillis ?: Long.MAX_VALUE } }
            .catch { throw it.toDomainException() }

    override fun observe(orgId: String, eventId: String): Flow<Event?> =
        events(orgId).document(eventId).snapshots()
            .map { doc -> if (doc.exists()) doc.toEvent() else null }
            .catch { throw it.toDomainException() }

    override suspend fun create(orgId: String, event: NewEvent): String = firebaseCall {
        val actorUid = auth.currentUser?.uid ?: throw DomainException.NotAuthorized()
        val ref = events(orgId).document()
        val batch = db.batch()
        batch.set(
            ref,
            event.details.toMap() + mapOf(
                FirestorePaths.Event.STATUS to EventStatus.ACTIVE.toFirestore(),
                FirestorePaths.Event.ROLES to event.roles.map { it.toMap() },
                FirestorePaths.Event.TOTAL_COLLECTED to 0L,
                FirestorePaths.Event.TOTAL_EXPENSES to 0L,
                FirestorePaths.Event.CREATED_AT to FieldValue.serverTimestamp(),
                FirestorePaths.Event.CREATED_BY to actorUid,
            ),
        )
        batch.putMembers(orgId, ref.id, event.members)
        batch.update(org(orgId), FirestorePaths.Org.EVENT_COUNT, FieldValue.increment(1))
        batch.commit().awaitWrite()
        ref.id
    }

    override suspend fun updateDetails(orgId: String, eventId: String, details: EventDetails) = firebaseCall {
        events(orgId).document(eventId).update(details.toMap()).awaitWrite()
        Unit
    }

    override suspend fun updateRoles(orgId: String, eventId: String, roles: List<EventRole>) = firebaseCall {
        events(orgId).document(eventId).update(FirestorePaths.Event.ROLES, roles.map { it.toMap() }).awaitWrite()
        Unit
    }

    override suspend fun setStatus(orgId: String, eventId: String, status: EventStatus) = firebaseCall {
        events(orgId).document(eventId).update(FirestorePaths.Event.STATUS, status.toFirestore()).awaitWrite()
        Unit
    }

    override fun observeMembers(orgId: String, eventId: String): Flow<List<EventMember>> =
        eventMembers(orgId, eventId).snapshots()
            .map { snapshot -> snapshot.documents.map { it.toEventMember() }.sortedBy { it.name.lowercase() } }
            .catch { throw it.toDomainException() }

    override suspend fun addMembers(orgId: String, eventId: String, members: List<EventMemberAssignment>) = firebaseCall {
        db.batch().apply { putMembers(orgId, eventId, members) }.commit().awaitWrite()
        Unit
    }

    override suspend fun setMemberRole(orgId: String, eventId: String, memberId: String, roleId: String?) = firebaseCall {
        eventMembers(orgId, eventId).document(memberId).update(FirestorePaths.EventMember.ROLE_ID, roleId).awaitWrite()
        Unit
    }

    override suspend fun removeMember(orgId: String, eventId: String, memberId: String) = firebaseCall {
        eventMembers(orgId, eventId).document(memberId).delete().awaitWrite()
        Unit
    }

    /** Runs online only (it is called by the sheet sync job), so a transaction is fine here (NFR-4 is about offline edits). */
    override suspend fun claimSheetId(orgId: String, eventId: String, expected: String?, sheetId: String): String = firebaseCall {
        val ref = events(orgId).document(eventId)
        db.runTransaction { tx ->
            val current = tx.get(ref).getString(FirestorePaths.Event.SHEET_ID)
            if (current != expected && current != null) {
                current
            } else {
                tx.update(ref, FirestorePaths.Event.SHEET_ID, sheetId)
                sheetId
            }
        }.await()
    }

    override suspend fun setSheetSynced(orgId: String, eventId: String, hash: Int) = firebaseCall {
        events(orgId).document(eventId).update(
            mapOf(
                FirestorePaths.Event.SHEET_HASH to hash.toLong(),
                FirestorePaths.Event.SHEET_SYNCED_AT to FieldValue.serverTimestamp(),
            ),
        ).awaitWrite()
        Unit
    }

    /** Merge keeps totalPaid and paidByDue of a member who is added again; new members start without them (read as 0). */
    private fun WriteBatch.putMembers(orgId: String, eventId: String, members: List<EventMemberAssignment>) {
        members.forEach { m ->
            set(
                eventMembers(orgId, eventId).document(m.memberId),
                mapOf(
                    FirestorePaths.EventMember.NAME to m.name,
                    FirestorePaths.EventMember.ROLE_ID to m.roleId,
                    FirestorePaths.EventMember.ACTIVE to true,
                    FirestorePaths.EventMember.ADDED_AT to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
    }

    private fun EventDetails.toMap(): Map<String, Any?> = mapOf(
        FirestorePaths.Event.NAME to name,
        FirestorePaths.Event.DESCRIPTION to description,
        FirestorePaths.Event.START_DATE to startDate.toFirestore(),
        FirestorePaths.Event.END_DATE to endDate.toFirestore(),
        FirestorePaths.Event.OPENING_BALANCE to openingBalance,
    )
}
