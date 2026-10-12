package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.NewEvent
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    /** All events, newest first. */
    fun observeAll(orgId: String): Flow<List<Event>>
    fun observe(orgId: String, eventId: String): Flow<Event?>

    /** Creates the event, its members and increments organization.eventCount in one batch. Returns the id. */
    suspend fun create(orgId: String, event: NewEvent): String
    suspend fun updateDetails(orgId: String, eventId: String, details: EventDetails)
    suspend fun updateRoles(orgId: String, eventId: String, roles: List<EventRole>)
    suspend fun setStatus(orgId: String, eventId: String, status: EventStatus)

    /** Event members sorted by name. */
    fun observeMembers(orgId: String, eventId: String): Flow<List<EventMember>>

    /** Adds or re-activates members. Existing totals are kept. */
    suspend fun addMembers(orgId: String, eventId: String, members: List<EventMemberAssignment>)
    suspend fun setMemberRole(orgId: String, eventId: String, memberId: String, roleId: String?)
    suspend fun removeMember(orgId: String, eventId: String, memberId: String)
}
