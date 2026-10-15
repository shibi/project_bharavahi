package com.bharavahi.core.domain.usecase.schedule

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.DueSummary
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.model.ScheduleDetails
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.bharavahi.core.domain.repository.SheetSyncScheduler
import com.bharavahi.core.domain.rules.DueGenerator
import com.bharavahi.core.domain.rules.DueStatusRules
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.validation.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Upper bound on dues so a typo can't create thousands of rows. */
const val MAX_DUES = 120

class ObserveSchedulesUseCase @Inject constructor(
    private val org: OrgContext,
    private val schedules: ScheduleRepository,
) {
    operator fun invoke(eventId: String): Flow<List<Schedule>> =
        flow { emitAll(schedules.observeAll(org.orgId(), eventId)) }
}

class ObserveScheduleUseCase @Inject constructor(
    private val org: OrgContext,
    private val schedules: ScheduleRepository,
) {
    operator fun invoke(eventId: String, scheduleId: String): Flow<Schedule?> =
        flow { emitAll(schedules.observe(org.orgId(), eventId, scheduleId)) }
}

/** ORG-15: a schedule with paid / partly paid / pending counts for each due. Null if the schedule is gone. */
class ObserveScheduleSummaryUseCase @Inject constructor(
    private val org: OrgContext,
    private val schedules: ScheduleRepository,
    private val events: EventRepository,
) {
    operator fun invoke(eventId: String, scheduleId: String): Flow<Pair<Schedule, List<DueSummary>>?> = flow {
        val orgId = org.orgId()
        emitAll(
            combine(schedules.observe(orgId, eventId, scheduleId), events.observeMembers(orgId, eventId)) { s, m ->
                s?.let { it to DueStatusRules.summarize(it, m) }
            },
        )
    }
}

/** ORG-14: shows the dues a schedule would get, before saving. */
class PreviewDuesUseCase @Inject constructor() {
    operator fun invoke(details: ScheduleDetails): Result<List<Due>> =
        try {
            Result.success(build(id = "", details).dues)
        } catch (e: DomainException) {
            Result.failure(e)
        }
}

/** ORG-14: validates, generates the dues once (BR-1 to BR-3) and saves. Returns the schedule id. */
class CreateScheduleUseCase @Inject constructor(
    private val org: OrgContext,
    private val schedules: ScheduleRepository,
    private val sheets: SheetSyncScheduler,
) {
    suspend operator fun invoke(eventId: String, details: ScheduleDetails): Result<String> = domainResult {
        val orgId = org.orgId()
        schedules.create(orgId, eventId, build(id = "", details)).also { sheets.enqueue(orgId, eventId) }
    }
}

/** ORG-16: editing is refused once the schedule has a payment. */
class UpdateScheduleUseCase @Inject constructor(
    private val org: OrgContext,
    private val schedules: ScheduleRepository,
    private val sheets: SheetSyncScheduler,
) {
    suspend operator fun invoke(eventId: String, scheduleId: String, details: ScheduleDetails): Result<Unit> =
        domainResult {
            val orgId = org.orgId()
            requireUnlocked(schedules, orgId, eventId, scheduleId)
            schedules.update(orgId, eventId, build(scheduleId, details))
            sheets.enqueue(orgId, eventId)
        }
}

/** Only an unlocked schedule (no payments yet) can be deleted. */
class DeleteScheduleUseCase @Inject constructor(
    private val org: OrgContext,
    private val schedules: ScheduleRepository,
    private val sheets: SheetSyncScheduler,
) {
    suspend operator fun invoke(eventId: String, scheduleId: String): Result<Unit> = domainResult {
        val orgId = org.orgId()
        requireUnlocked(schedules, orgId, eventId, scheduleId)
        schedules.delete(orgId, eventId, scheduleId)
        sheets.enqueue(orgId, eventId)
    }
}

private suspend fun requireUnlocked(repo: ScheduleRepository, orgId: String, eventId: String, scheduleId: String) {
    val current = repo.observe(orgId, eventId, scheduleId).first() ?: throw DomainException.NotFound()
    if (current.locked) throw DomainException.ScheduleLocked()
}

private fun build(id: String, details: ScheduleDetails): Schedule {
    validate(details)
    val start = details.startDate!!
    val end = details.endDate!!
    return Schedule(
        id = id,
        name = details.name.trim(),
        description = details.description.trim(),
        amountPerMember = details.amountPerMember,
        numberOfDues = details.numberOfDues,
        startDate = start,
        endDate = end,
        dues = DueGenerator.generate(details.amountPerMember, details.numberOfDues, start, end),
        locked = false,
    )
}

internal fun validate(details: ScheduleDetails) {
    val errors = buildMap<Field, ValidationError> {
        Validators.required(details.name)?.let { put(Field.NAME, it) }
        if (details.startDate == null) put(Field.START_DATE, ValidationError.REQUIRED)
        if (details.endDate == null) put(Field.END_DATE, ValidationError.REQUIRED)
        if (details.startDate != null && details.endDate != null && details.endDate.isBefore(details.startDate)) {
            put(Field.END_DATE, ValidationError.END_BEFORE_START)
        }
        if (details.numberOfDues !in 1..MAX_DUES) put(Field.NUMBER_OF_DUES, ValidationError.INVALID_COUNT)
        when {
            details.amountPerMember <= 0 -> put(Field.AMOUNT, ValidationError.INVALID_AMOUNT)
            details.numberOfDues in 1..MAX_DUES &&
                details.amountPerMember / details.numberOfDues < DueGenerator.PAISE_PER_RUPEE ->
                put(Field.AMOUNT, ValidationError.AMOUNT_TOO_SMALL)
        }
    }
    if (errors.isNotEmpty()) throw DomainException.Validation(errors)
}
