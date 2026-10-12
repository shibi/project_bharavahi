package com.bharavahi.core.ui.components

import androidx.annotation.StringRes
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.ui.R

@StringRes
fun Throwable.messageRes(): Int = when (this) {
    is DomainException.InvalidCredentials -> R.string.core_error_invalid_credentials
    is DomainException.NotAuthorized -> R.string.core_error_not_authorized
    is DomainException.EmailAlreadyInUse -> R.string.core_error_email_in_use
    is DomainException.NotFound -> R.string.core_error_not_found
    is DomainException.ConfirmationMismatch -> R.string.core_error_confirmation_mismatch
    is DomainException.Network -> R.string.core_error_network
    is DomainException.Validation -> R.string.core_error_validation
    else -> R.string.core_error_unknown
}

@StringRes
fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.REQUIRED -> R.string.core_validation_required
    ValidationError.INVALID_EMAIL -> R.string.core_validation_invalid_email
    ValidationError.INVALID_PHONE -> R.string.core_validation_invalid_phone
    ValidationError.PASSWORD_TOO_SHORT -> R.string.core_validation_password_short
}
