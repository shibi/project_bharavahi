package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    /** Schedules of an event, by start date. */
    fun observeAll(orgId: String, eventId: String): Flow<List<Schedule>>
    fun observe(orgId: String, eventId: String, scheduleId: String): Flow<Schedule?>

    /** Saves a new schedule ([Schedule.id] is ignored). Returns the id. */
    suspend fun create(orgId: String, eventId: String, schedule: Schedule): String

    /** Replaces an unlocked schedule. */
    suspend fun update(orgId: String, eventId: String, schedule: Schedule)
    suspend fun delete(orgId: String, eventId: String, scheduleId: String)
}
