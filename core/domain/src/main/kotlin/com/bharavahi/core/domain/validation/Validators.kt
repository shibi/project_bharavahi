package com.bharavahi.core.domain.validation

import com.bharavahi.core.domain.error.ValidationError

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^\\+?[0-9 ]{7,15}$")
    const val MIN_PASSWORD_LENGTH = 8

    fun required(value: String): ValidationError? =
        if (value.isBlank()) ValidationError.REQUIRED else null

    fun email(value: String): ValidationError? = when {
        value.isBlank() -> ValidationError.REQUIRED
        !EMAIL.matches(value.trim()) -> ValidationError.INVALID_EMAIL
        else -> null
    }

    /** Optional phone: blank is allowed, anything else must look like a phone number. */
    fun optionalPhone(value: String): ValidationError? =
        if (value.isBlank() || PHONE.matches(value.trim())) null else ValidationError.INVALID_PHONE

    fun password(value: String): ValidationError? = when {
        value.isEmpty() -> ValidationError.REQUIRED
        value.length < MIN_PASSWORD_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        else -> null
    }
}
