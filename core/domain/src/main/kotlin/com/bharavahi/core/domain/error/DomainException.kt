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

    /** ORG-16: the schedule already has a payment, so its dues are fixed. */
    class ScheduleLocked : DomainException()

    /** A member with payments in an event can't be removed from it; history must stay intact. */
    class MemberHasPayments : DomainException()
    /** ORG-25: this payment was already reversed, or is itself a reversal. */
    class AlreadyReversed : DomainException()

    /** Mail settings are missing in the Super Admin app, so backup emails can't be sent. */
    class MailNotConfigured : DomainException()

    /** ORG-4: a community already has the maximum number of active admins. */
    class AdminLimitReached : DomainException()
    class Network(cause: Throwable? = null) : DomainException(cause = cause)
    class Unknown(cause: Throwable? = null) : DomainException(cause?.message, cause)
}

enum class Field {
    NAME, TYPE, ADDRESS, PHONE, EMAIL, PASSWORD, ADMIN_NAME, ADMIN_EMAIL, ADMIN_PHONE, SENDER_EMAIL, APP_PASSWORD,
    NICKNAME, DESCRIPTION, START_DATE, END_DATE, OPENING_BALANCE, AMOUNT, NUMBER_OF_DUES, ROLE_NAME,
    MEMBER, EVENT, SCHEDULE, DUE, TITLE, DATE, DATE_OF_BIRTH, JOB, HOUSE_NAME,
}

enum class ValidationError {
    REQUIRED, INVALID_EMAIL, INVALID_PHONE, PASSWORD_TOO_SHORT,
    INVALID_AMOUNT, INVALID_COUNT, END_BEFORE_START, DUPLICATE, AMOUNT_TOO_SMALL,
    AMOUNT_TOO_LARGE, DUE_ALREADY_PAID, FUTURE_DATE,
}
