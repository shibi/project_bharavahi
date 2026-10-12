package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class FirestoreScheduleRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : ScheduleRepository {

    private fun schedules(orgId: String, eventId: String) =
        db.collection(FirestorePaths.ORGANIZATIONS).document(orgId)
            .collection(FirestorePaths.EVENTS).document(eventId)
            .collection(FirestorePaths.SCHEDULES)

    override fun observeAll(orgId: String, eventId: String): Flow<List<Schedule>> =
        schedules(orgId, eventId).snapshots()
            .map { snapshot -> snapshot.documents.map { it.toSchedule() }.sortedWith(compareBy({ it.startDate }, { it.name })) }
            .catch { throw it.toDomainException() }

    override fun observe(orgId: String, eventId: String, scheduleId: String): Flow<Schedule?> =
        schedules(orgId, eventId).document(scheduleId).snapshots()
            .map { doc -> if (doc.exists()) doc.toSchedule() else null }
            .catch { throw it.toDomainException() }

    override suspend fun create(orgId: String, eventId: String, schedule: Schedule): String = firebaseCall {
        val ref = schedules(orgId, eventId).document()
        ref.set(schedule.copy(locked = false).toMap() + (FirestorePaths.Schedule.CREATED_AT to FieldValue.serverTimestamp())).awaitWrite()
        ref.id
    }

    override suspend fun update(orgId: String, eventId: String, schedule: Schedule) = firebaseCall {
        schedules(orgId, eventId).document(schedule.id).update(schedule.toMap()).awaitWrite()
        Unit
    }

    override suspend fun delete(orgId: String, eventId: String, scheduleId: String) = firebaseCall {
        schedules(orgId, eventId).document(scheduleId).delete().awaitWrite()
        Unit
    }
}
