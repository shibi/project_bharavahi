package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.validation.Validators

internal fun MutableMap<Field, ValidationError>.validateDetails(details: OrganizationDetails) {
    Validators.required(details.name)?.let { put(Field.NAME, it) }
    Validators.required(details.type)?.let { put(Field.TYPE, it) }
    Validators.optionalPhone(details.contactPhone)?.let { put(Field.PHONE, it) }
}

internal fun MutableMap<Field, ValidationError>.validateAdmin(admin: NewAdmin) {
    Validators.required(admin.name)?.let { put(Field.ADMIN_NAME, it) }
    Validators.email(admin.email)?.let { put(Field.ADMIN_EMAIL, it) }
    Validators.optionalPhone(admin.phone)?.let { put(Field.ADMIN_PHONE, it) }
    Validators.password(admin.temporaryPassword)?.let { put(Field.PASSWORD, it) }
}

internal fun OrganizationDetails.trimmed() = OrganizationDetails(
    name = name.trim(),
    type = type.trim(),
    address = address.trim(),
    contactPhone = contactPhone.trim(),
)
