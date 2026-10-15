package com.bharavahi.core.domain.fake

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.AdminLink
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.NewEvent
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.NewPayment
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.repository.PaymentBackupScheduler
import com.bharavahi.core.domain.repository.PaymentRepository
import com.bharavahi.core.domain.repository.AdminIndexRepository
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.bharavahi.core.domain.repository.SheetSyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeOrgContext(private val id: String = "org1") : OrgContext {
    override suspend fun orgId() = id
}

class FakeAdminIndexRepository(links: Map<String, AdminLink> = emptyMap()) : AdminIndexRepository {
    val links = MutableStateFlow(links)
    override suspend fun find(uid: String): AdminLink? = links.value[uid]
    override fun observe(uid: String): Flow<AdminLink?> = links.map { it[uid] }
}

class FakeScheduleRepository : ScheduleRepository {
    val schedules = MutableStateFlow<Map<String, Schedule>>(emptyMap())
    var deleted = mutableListOf<String>()

    override fun observeAll(orgId: String, eventId: String) = schedules.map { it.values.toList() }
    override fun observe(orgId: String, eventId: String, scheduleId: String) = schedules.map { it[scheduleId] }
    override suspend fun create(orgId: String, eventId: String, schedule: Schedule): String {
        val id = "s${schedules.value.size + 1}"
        schedules.value = schedules.value + (id to schedule.copy(id = id))
        return id
    }
    override suspend fun update(orgId: String, eventId: String, schedule: Schedule) {
        schedules.value = schedules.value + (schedule.id to schedule)
    }
    override suspend fun delete(orgId: String, eventId: String, scheduleId: String) {
        deleted += scheduleId
        schedules.value = schedules.value - scheduleId
    }
}

class FakeEventRepository : EventRepository {
    val events = MutableStateFlow<Map<String, Event>>(emptyMap())
    val members = MutableStateFlow<Map<String, EventMember>>(emptyMap())
    var created: NewEvent? = null

    override fun observeAll(orgId: String) = events.map { it.values.toList() }
    override fun observe(orgId: String, eventId: String) = events.map { it[eventId] }
    override suspend fun create(orgId: String, event: NewEvent): String {
        created = event
        return "e1"
    }
    override suspend fun updateDetails(orgId: String, eventId: String, details: EventDetails) = Unit
    override suspend fun updateRoles(orgId: String, eventId: String, roles: List<EventRole>) = Unit
    override suspend fun setStatus(orgId: String, eventId: String, status: EventStatus) = Unit
    override fun observeMembers(orgId: String, eventId: String) = members.map { it.values.toList() }
    override suspend fun addMembers(orgId: String, eventId: String, members: List<EventMemberAssignment>) {
        members.forEach { this.members.value += it.memberId to eventMember(it.memberId, roleId = it.roleId) }
    }
    override suspend fun setMemberRole(orgId: String, eventId: String, memberId: String, roleId: String?) {
        val m = members.value[memberId] ?: throw DomainException.NotFound()
        members.value += memberId to m.copy(roleId = roleId)
    }
    override suspend fun removeMember(orgId: String, eventId: String, memberId: String) {
        members.value -= memberId
    }
    override suspend fun claimSheetId(orgId: String, eventId: String, expected: String?, sheetId: String): String {
        val event = events.value.getValue(eventId)
        if (event.sheetId != expected) return event.sheetId!!
        events.value += eventId to event.copy(sheetId = sheetId)
        return sheetId
    }
    override suspend fun setSheetSynced(orgId: String, eventId: String, hash: Int) {
        events.value += eventId to events.value.getValue(eventId).copy(sheetHash = hash, sheetSyncedAtMillis = 1L)
    }
}

class FakeSheetSyncScheduler : SheetSyncScheduler {
    val queued = mutableListOf<String>()
    var all = 0
    override fun enqueue(orgId: String, eventId: String) { queued += eventId }
    override fun enqueueAll() { all++ }
}

fun eventMember(
    id: String,
    roleId: String? = null,
    active: Boolean = true,
    paidByDue: Map<String, Long> = emptyMap(),
) = EventMember(id, "Name $id", roleId, active, paidByDue.values.sum(), paidByDue)

class FakePaymentRepository : PaymentRepository {
    val payments = MutableStateFlow<List<Payment>>(emptyList())
    override fun observeAll(orgId: String, eventId: String) = payments.map { it.reversed() }
    override suspend fun record(orgId: String, eventId: String, payment: NewPayment, reversesPaymentId: String?): String {
        val id = "p${payments.value.size + 1}"
        payments.value += Payment(
            id, payment.memberId, payment.scheduleId, payment.dueIndex, payment.amount, payment.method, payment.note,
            payment.paidOn, "admin", null, reversesPaymentId, EmailStatus.PENDING, waitingToSync = true,
        )
        return id
    }
    override suspend fun setEmailStatus(orgId: String, eventId: String, paymentId: String, status: EmailStatus) {
        payments.value = payments.value.map { if (it.id == paymentId) it.copy(emailStatus = status) else it }
    }
}

class FakeBackupScheduler : PaymentBackupScheduler {
    val queued = mutableListOf<String>()
    override fun enqueue(orgId: String, eventId: String, paymentId: String) { queued += paymentId }
}
