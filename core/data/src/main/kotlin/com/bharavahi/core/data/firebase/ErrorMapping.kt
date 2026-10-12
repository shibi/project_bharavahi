package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.coroutines.cancellation.CancellationException

/** Translates Firebase exceptions into errors the domain and UI understand. */
internal fun Throwable.toDomainException(): DomainException = when (this) {
    is DomainException -> this
    is FirebaseAuthWeakPasswordException ->
        DomainException.Validation(mapOf(Field.PASSWORD to ValidationError.PASSWORD_TOO_SHORT))
    is FirebaseAuthUserCollisionException -> DomainException.EmailAlreadyInUse()
    is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
        DomainException.InvalidCredentials()
    is FirebaseNetworkException -> DomainException.Network(this)
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> DomainException.NotAuthorized()
        FirebaseFirestoreException.Code.NOT_FOUND -> DomainException.NotFound()
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DomainException.Network(this)
        else -> DomainException.Unknown(this)
    }
    else -> DomainException.Unknown(this)
}

/** Runs a Firebase call and rethrows failures as [DomainException]. */
internal suspend inline fun <T> firebaseCall(crossinline block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toDomainException()
    }
