package com.bharavahi.core.domain.usecase.event

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.validation.Validators
import java.util.UUID

internal fun MutableMap<Field, ValidationError>.validateEventDetails(details: EventDetails) {
    Validators.required(details.name)?.let { put(Field.NAME, it) }
    if (details.startDate == null) put(Field.START_DATE, ValidationError.REQUIRED)
    if (details.endDate == null) put(Field.END_DATE, ValidationError.REQUIRED)
    if (details.startDate != null && details.endDate != null && details.endDate.isBefore(details.startDate)) {
        put(Field.END_DATE, ValidationError.END_BEFORE_START)
    }
    if (details.openingBalance < 0) put(Field.OPENING_BALANCE, ValidationError.INVALID_AMOUNT)
}

/** Role names must be filled in and unique, ignoring case. */
internal fun MutableMap<Field, ValidationError>.validateRoles(roles: List<EventRole>) {
    val names = roles.map { it.name.trim().lowercase() }
    when {
        names.any { it.isEmpty() } -> put(Field.ROLE_NAME, ValidationError.REQUIRED)
        names.toSet().size != names.size -> put(Field.ROLE_NAME, ValidationError.DUPLICATE)
    }
}

internal inline fun validating(block: MutableMap<Field, ValidationError>.() -> Unit) {
    val errors = buildMap<Field, ValidationError>(block)
    if (errors.isNotEmpty()) throw DomainException.Validation(errors)
}

internal fun EventDetails.trimmed() = copy(name = name.trim(), description = description.trim())

internal fun List<EventRole>.trimmed() = map { it.copy(name = it.name.trim()) }

object EventRoles {
    /** A short random id for a new role; roles live inside the event document. */
    fun newRole(name: String): EventRole = EventRole(UUID.randomUUID().toString().take(8), name)
}
