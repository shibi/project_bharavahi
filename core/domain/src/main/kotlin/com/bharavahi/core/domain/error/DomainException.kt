package com.bharavahi.core.domain.error

/** Errors the UI knows how to explain. Data-layer failures are mapped onto these. */
sealed class DomainException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause) {

    class Validation(val errors: Map<Field, ValidationError>) : DomainException("Invalid input")
    class InvalidCredentials : DomainException()
    class NotAuthorized : DomainException()
    class EmailAlreadyInUse : DomainException()
    class NotFound : DomainException()
    class ConfirmationMismatch : DomainException()
    class Network(cause: Throwable? = null) : DomainException(cause = cause)
    class Unknown(cause: Throwable? = null) : DomainException(cause?.message, cause)
}

enum class Field { NAME, TYPE, ADDRESS, PHONE, EMAIL, PASSWORD, ADMIN_NAME, ADMIN_EMAIL, ADMIN_PHONE, SENDER_EMAIL, APP_PASSWORD }

enum class ValidationError { REQUIRED, INVALID_EMAIL, INVALID_PHONE, PASSWORD_TOO_SHORT }
