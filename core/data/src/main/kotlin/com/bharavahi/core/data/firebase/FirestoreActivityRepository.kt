package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.ActivityLog
import com.bharavahi.core.domain.repository.ActivityRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreActivityRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : ActivityRepository {

    /** Needs the collection-group index in firestore.indexes.json. */
    override fun observeRecent(limit: Int): Flow<List<ActivityLog>> =
        db.collectionGroup(FirestorePaths.ACTIVITY)
            .orderBy(FirestorePaths.Activity.AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toActivityLog() } }
            .catch { throw it.toDomainException() }

    override fun observeForOrganization(orgId: String, limit: Int): Flow<List<ActivityLog>> =
        db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
            .collection(FirestorePaths.ACTIVITY)
            .orderBy(FirestorePaths.Activity.AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toActivityLog() } }
            .catch { throw it.toDomainException() }

    override suspend fun log(orgId: String, action: ActivityAction, actorUid: String, target: String) = firebaseCall {
        db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
            .collection(FirestorePaths.ACTIVITY)
            .add(
                mapOf(
                    FirestorePaths.Activity.ACTION to action.toFirestore(),
                    FirestorePaths.Activity.ACTOR_UID to actorUid,
                    FirestorePaths.Activity.TARGET to target,
                    FirestorePaths.Activity.AT to FieldValue.serverTimestamp(),
                ),
            ).await()
        Unit
    }
}
