package com.bharavahi.core.domain.usecase.event

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.NewEvent
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.usecase.domainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** ORG-10: events of the community, newest first. */
class ObserveEventsUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    operator fun invoke(): Flow<List<Event>> = flow { emitAll(events.observeAll(org.orgId())) }

    companion object {
        fun filter(events: List<Event>, status: EventStatus?): List<Event> =
            if (status == null) events else events.filter { it.status == status }
    }
}

class ObserveEventUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    operator fun invoke(eventId: String): Flow<Event?> = flow { emitAll(events.observe(org.orgId(), eventId)) }
}

/** ORG-11: details, opening balance, roles and members in one save. Returns the new event id. */
class CreateEventUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(event: NewEvent): Result<String> = domainResult {
        validating {
            validateEventDetails(event.details)
            validateRoles(event.roles)
        }
        val roleIds = event.roles.map { it.id }.toSet()
        val members = event.members
            .distinctBy { it.memberId }
            .map { if (it.roleId in roleIds) it else it.copy(roleId = null) }
        events.create(org.orgId(), NewEvent(event.details.trimmed(), event.roles.trimmed(), members))
    }
}

/** ORG-12: edit name, description, dates and opening balance. */
class UpdateEventDetailsUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(eventId: String, details: EventDetails): Result<Unit> = domainResult {
        validating { validateEventDetails(details) }
        events.updateDetails(org.orgId(), eventId, details.trimmed())
    }
}

/** ORG-12: create, rename or remove roles. Members holding a removed role are left without one. */
class UpdateEventRolesUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(eventId: String, roles: List<EventRole>): Result<Unit> = domainResult {
        validating { validateRoles(roles) }
        val orgId = org.orgId()
        events.updateRoles(orgId, eventId, roles.trimmed())
        val kept = roles.map { it.id }.toSet()
        events.observeMembers(orgId, eventId).first()
            .filter { it.roleId != null && it.roleId !in kept }
            .forEach { events.setMemberRole(orgId, eventId, it.memberId, null) }
    }
}

/** ORG-10: suspend, archive, close or reactivate. */
class SetEventStatusUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(eventId: String, status: EventStatus): Result<Unit> = domainResult {
        events.setStatus(org.orgId(), eventId, status)
    }
}

class ObserveEventMembersUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    operator fun invoke(eventId: String): Flow<List<EventMember>> =
        flow { emitAll(events.observeMembers(org.orgId(), eventId)) }
}

/** ORG-12 and BR-6: members added later still owe every due. */
class AddEventMembersUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(eventId: String, members: List<EventMemberAssignment>): Result<Unit> = domainResult {
        if (members.isNotEmpty()) events.addMembers(org.orgId(), eventId, members.distinctBy { it.memberId })
    }
}

class SetEventMemberRoleUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(eventId: String, memberId: String, roleId: String?): Result<Unit> = domainResult {
        events.setMemberRole(org.orgId(), eventId, memberId, roleId)
    }
}

/** Only members with nothing paid can leave an event, so payment history always has its member. */
class RemoveEventMemberUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
) {
    suspend operator fun invoke(eventId: String, memberId: String): Result<Unit> = domainResult {
        val orgId = org.orgId()
        val member = events.observeMembers(orgId, eventId).first().firstOrNull { it.memberId == memberId }
            ?: throw DomainException.NotFound()
        if (member.totalPaid != 0L || member.paidByDue.values.any { it != 0L }) throw DomainException.MemberHasPayments()
        events.removeMember(orgId, eventId, memberId)
    }
}
